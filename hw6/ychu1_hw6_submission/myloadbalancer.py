"""
Author: Yilei CHU (ychu1)
Date: 2016 April 7

14740 homework 6
Module for performing load balancing between hosts and servers

Assumption: the input ip for servers and load balancer are valid

Basis: per flow
Implementation: flow_mem records client <-> server flows
Two keys:
client -> server src/dst ip/port => entry
server -> client src/dst ip/port => entry

server_map stores available servers' info
new entry is added into server_map when load balancer receives arp response

Reference: pox/ext/ip_loadbalancer.py

===================================================================================
Attention: when grading, please don't modify this class
IPs for servers and load balancer are passed in through command line
===================================================================================

"""
from pox.core import log, core
from pox.lib.addresses import IPAddr, EthAddr
from pox.lib.packet import arp, ethernet, ETHER_BROADCAST
from pox.lib.revent import EventMixin
import pox.openflow.libopenflow_01 as of
from pox.lib.util import str_to_bool, dpid_to_str
import time
from proto.arp_responder import launch as arp_launch
import logging

# global variable
FLOW_IDLE_TIMEOUT = 60  # in seconds
FLOW_HARD_TIMEOUT = 0  # infinity
log = core.getLogger("loadbalancer")
pid = None

# load balancer class using round-robin
class loadbalancer(EventMixin):

    """ ============================ helper classes ============================ """
    class myflow(object):   # used as key in flow_mem
        def __init__(self, srcip, srcport, dstip,dstport):
            self.srcip = srcip
            self.srcport = srcport
            self.dstip = dstip
            self.dstport = dstport

        def __str__(self):
            return str(self.__dict__)

        def __eq__(self, other):
            return self.__dict__ == other.__dict__

        def __hash__(self):
            return hash((self.srcip,self.srcport,self.dstip,self.dstport))

    class mem_entry(object): # flow memory entry
        def __init__(self, server_ip, client_port):
            self.server = server_ip
            self.client_port = client_port

        def __str__(self):
            return str(self.__dict__)

        def __eq__(self, other):
            return self.__dict__ == other.__dict__

    """ ============================ end of helper classes , start of lb class ============================ """

    def __init__(self, connection, lb_ip, server_ips=[]):
        self.lb_ip = IPAddr(lb_ip)
        self.lb_mac = connection.eth_addr
        self.server_ips = [IPAddr(i) for i in server_ips]
        self.connection = connection
        self.last_server_id = 0

        try:
            self.log = log.getChild(dpid_to_str(self.connection.dpid))
        except:
            self.log = log

        self.flow_mem = {}      # key: myflow value:mem_entry (for recording flows for msg between same pair of server and client)
        self.server_map = {}    # key: ip value: mac,port (initially mac & port of servers not known, need arp)
        
        self.probe_interval = 0.3 # interval between each probe
        self.probe_cnt = 0
        self.probe_servers()    #start probing

    # send arp requests to known servers
    def probe_servers(self):
        ''' do probe, use ip to get each server's mac and port '''
        if self.probe_cnt >= len(self.server_ips):
            self.log.debug("[arp] finish all arps ")
            return

        server_ip = self.server_ips.pop(0) #start probing from start
        self.server_ips.append(server_ip)
        self.log.debug("[arp] will ARP %s", server_ip)

        # prepare arp packet
        arp_pkt = arp()
        arp_pkt.prototype = arp.PROTO_TYPE_IP
        arp_pkt.opcode = arp.REQUEST
        arp_pkt.hwtype = arp.HW_TYPE_ETHERNET
        arp_pkt.hwsrc = self.lb_mac
        arp_pkt.hwdst = ETHER_BROADCAST
        arp_pkt.protosrc = self.lb_ip
        arp_pkt.protodst = server_ip

        # wrap into ethernet packet
        ether_pkt = ethernet(type=ethernet.ARP_TYPE, src=self.lb_mac, dst=ETHER_BROADCAST)
        ether_pkt.set_payload(arp_pkt)

        # wrap into msg and send
        msg = of.ofp_packet_out()
        msg.data = ether_pkt.pack()
        msg.actions.append(of.ofp_action_output(port=of.OFPP_FLOOD))
        msg.in_port = of.OFPP_NONE
        self.connection.send(msg)
        self.log.debug("[arp] finish ARPing for %s", server_ip)

        # increase probe cnt, prepare for next probe
        self.probe_cnt += 1
        time.sleep(self.probe_interval)
        self.probe_servers()

    # drop packet
    def drop(self,event):
        if event.ofp.buffer_id is not None:
            msg = of.ofp_packet_out(data=event.ofp)
            self.connection.send(msg)
        return None

    # round-robin choose next server, return server ip, only pick from server_map
    def choose_server(self):
        keys = self.server_map.keys()
        self.last_server_id = (self.last_server_id+1)%len(keys)
        return keys[self.last_server_id]

    # override
    def _handle_PacketIn(self, event):
        inport = event.port
        packet = event.parsed

        # ------------------------- arp from probe ---------------------------
        is_tcp = packet.find('tcp')
        if not is_tcp:
            is_arp = packet.find('arp')
            if is_arp:
                if is_arp.opcode == arp.REPLY:
                    # add server into server_map
                    self.server_map[is_arp.protosrc] = is_arp.hwsrc, inport
                    self.log.debug("[INFO] server %s up", is_arp.protosrc)
                return
            return self.drop(event) # not tcp not arp

        is_ipv4 = packet.find('ipv4')
        if is_ipv4 is None:
            # from other like ipv6, not handle
            self.log.warn("[INFO] TCP packet not using ipv4")
            self.drop(event)

        # ------------------------- forward packet from client to server ---------------------------
        if is_ipv4.dstip == self.lb_ip:
            # check whether recorded in flow_mem
            key = self.myflow(is_ipv4.srcip,is_tcp.srcport,is_ipv4.dstip,is_tcp.dstport)
            entry = self.flow_mem.get(key)

            if entry is None or entry.server not in self.server_map:
                # choose a new server responsible for this flow
                server_ip = self.choose_server()
                if server_ip is None:
                    self.log.debug("[INFO] lb cannot find a server to handle")
                    return self.drop(event)

                # insert entry into flow memory
                entry = self.mem_entry(server_ip,inport)
                self.flow_mem[key] = entry  # forward key: client -> server
                reverse_key = self.myflow(server_ip,is_tcp.dstport,is_ipv4.srcip,is_tcp.srcport)
                self.flow_mem[reverse_key] = entry  # reverse key: server -> client
                self.log.debug("[INFO] key1: %s, key2: %s",str(key),str(reverse_key))
                self.log.debug("[INFO] flow_mem insert new entry: %s",str(entry))

            server_ip = entry.server
            self.log.debug("[INFO] lb directs packet to %s",server_ip)
            server_mac, server_port = self.server_map[server_ip]

            actions = []
            actions.append(of.ofp_action_dl_addr.set_dst(server_mac))
            actions.append(of.ofp_action_nw_addr.set_dst(server_ip))
            actions.append(of.ofp_action_output(port=server_port))
            match = of.ofp_match.from_packet(packet, inport)
            msg = of.ofp_flow_mod(command=of.OFPFC_ADD,
                                  idle_timeout=FLOW_IDLE_TIMEOUT,
                                  hard_timeout=of.OFP_FLOW_PERMANENT,
                                  data=event.ofp,
                                  actions=actions,
                                  match=match)
            self.connection.send(msg)
            return

        # ------------------------- reply packet from server to client ---------------------------
        else:
            if is_ipv4.srcip not in self.server_ips:
                self.log.debug("[INFO] lb receive pkt from unknown server %s",is_ipv4.srcip)
                return self.drop(event)

            key = self.myflow(is_ipv4.srcip,is_tcp.srcport,is_ipv4.dstip,is_tcp.dstport)
            entry = self.flow_mem.get(key)
            self.log.debug("[reply] lb try to find key %s in mem",str(key))

            # reverse table entry
            mac, port = self.server_map[entry.server]

            actions = []
            actions.append(of.ofp_action_dl_addr.set_src(self.lb_mac))
            actions.append(of.ofp_action_nw_addr.set_src(self.lb_ip))
            actions.append(of.ofp_action_output(port=entry.client_port))
            match = of.ofp_match.from_packet(packet, inport)
            msg = of.ofp_flow_mod(command=of.OFPFC_ADD,
                                  idle_timeout=FLOW_IDLE_TIMEOUT,
                                  hard_timeout=of.OFP_FLOW_PERMANENT,
                                  data=event.ofp,
                                  actions=actions,
                                  match=match)
            self.connection.send(msg)
            self.log.debug("[INFO] lb directs reply from server %s to client port %s",is_ipv4.srcip,entry.client_port)
            return

# lauching, references pox/ext/ip_loadbalancer.py
def launch(lb_ip, server_ips):
    server_ips = server_ips.replace(",", " ").split()
    server_ips = [IPAddr(i) for i in server_ips]
    lb_ip = IPAddr(lb_ip)

    arp_launch(eat_packets=False, **{str(lb_ip): True})    # ARP Responder
    logging.getLogger("proto.arp_responder").setLevel(logging.WARN)

    def _handle_ConnectionUp(event):
        global pid
        if pid is None:
            log.info("load balancer starts.")
            core.registerNew(loadbalancer, event.connection, IPAddr(lb_ip), server_ips)
            pid = event.dpid
        if pid != event.dpid:
            log.warn("Ignoring switch %s", event.connection)
        else:
            log.info("Load balancing on %s", event.connection)
            core.loadbalancer.connection = event.connection
            event.connection.addListeners(core.loadbalancer)
    core.openflow.addListenerByName("ConnectionUp", _handle_ConnectionUp)