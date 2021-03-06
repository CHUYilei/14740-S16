#!/usr/bin/python

from mininet.topo import Topo
from mininet.node import CPULimitedHost
from mininet.link import TCLink
from mininet.net import Mininet
from mininet.util import dumpNodeConnections
from mininet.log import setLogLevel
from mininet.cli import CLI
from mininet.node import OVSController


class UnbalancedTreeTopo(Topo):
    "Customized unbalanced tree topology class in hw2 manual"    
    def __init__(self):
        # Initialize topology
        Topo.__init__( self )
        
        " create all 6 switches "
        s1 = self.addSwitch('s1')
        s2 = self.addSwitch('s2')
        s3 = self.addSwitch('s3')
        s4 = self.addSwitch('s4')
        s5 = self.addSwitch('s5')
        s6 = self.addSwitch('s6')
        
        " add links between switches "
        self.addLink(s1,s2, bw=10, delay='1ms', loss=3)
        self.addLink(s1,s3, bw=15, delay='2ms', loss=2)
        self.addLink(s2,s4, bw=20, delay='4ms', loss=1)
        self.addLink(s3,s5, bw=20, delay='4ms', loss=1)
        self.addLink(s5,s6, bw=40, delay='10ms', loss=2)
                
        " create all 8 hosts "
        h1=self.addHost('h1')
        h2=self.addHost('h2')
        h3=self.addHost('h3')
        h4=self.addHost('h4')
        h5=self.addHost('h5')
        h6=self.addHost('h6')
        h7=self.addHost('h7')
        h8=self.addHost('h8')
        
        " add links between hosts and switches (default link value) "
        self.addLink(h1,s1)
        self.addLink(h2,s2)
        self.addLink(h3,s3)
        self.addLink(h4,s4)
        self.addLink(h5,s4)
        self.addLink(h6,s5)
        self.addLink(h7,s6)
        self.addLink(h8,s6)
        
        print 'finish building'

def perfTest():
    "Create network and run simple performance tests"
    topo=UnbalancedTreeTopo()
    net=Mininet(topo=topo,host=CPULimitedHost, link=TCLink,controller = OVSController)
    net.start()
    print "Dumping host connections"
    dumpNodeConnections(net.hosts)
    
    hostNum = 8
    
    # store all hosts in a list
    hostList = []
    for i in range(1,hostNum+1):
        hostList.append(net.get('h'+str(i)))
         
    print "Task1: connectivity test by 10 ping msg between each host pair"
    for i in range(hostNum):
        for j in range(hostNum):
            if i!=j:
                print 'Result of host '\
                    + hostList[i].name+' ('+ hostList[i].IP() +')'\
                    +' ping host '\
                    + hostList[j].name+' (ip: '+ hostList[j].IP() +' )'
                print hostList[i].cmd('ping -c10 %s' % hostList[j].IP())
                print
                   
    print "Task2: measure TCP bandwidth between each pair of hosts"
    for i in range(hostNum):
        for j in range(hostNum):
            if i!=j:
                result = net.iperf([hostList[j],hostList[i]])
                print 'TCP bandwidth from server '+ hostList[i].name +' to client '+ hostList[j].name+' '+result[0]
                print 'from client '+ hostList[j].name +' to server '+ hostList[i].name+' '+ result[1]
                print

    print "Task3: measure UDP packet loss between each pair of hosts at bandwidth 15Mb/s"
    for i in range(hostNum):
        for j in range(hostNum):
            if i!=j:
                server = hostList[j]
                client = hostList[i]
                print 'server: '+server.name+' client: '+client.name
                result = server.cmd('iperf -u -s &')
                pid = server.cmd('echo $!')
            
                print "Done running iperf on server, starting client now"
                result1=client.cmd('iperf -c %s -u -b 15m' % server.IP())
                print result1
            
                print "Shutting down the iperf server"
                server.cmd('kill -9 $pid')
                print
    
    net.stop()

if __name__ == '__main__':
    setLogLevel('info')
    perfTest()
