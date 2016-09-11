package services;

import datatypes.Datagram;
import datatypes.Segment;

import java.io.IOException;
import java.util.Timer;

/**
 * Author:
 * xou
 * ychu1
 *
 * March 17
 * @reference: https://github.com/venkatesh5789/TTP/blob/master/src/services/TTPConnEndPoint.java
 * @reference: https://github.com/wentianqi7/ReliableTransportOverUDP
 * Client sender
 * would handle seq# when create a new segment
 */

/**
 * Client-sender part
 */
public class ClientSender {
    private DatagramService datagramService;

    /* addr/port */
    private String srcIP;
    private String dstIP;
    private short srcPort;
    private short dstPort;

    /* registered receiver */
    private ClientReceiverRunnable client_receiver;

    /* timer and timeout value */
    private Timer timer;
    private int timeout;

    private static final int CHUNKSIZE = 500;

    /**
     * Constructor
     */
    public ClientSender(String srcIP, String dstIP, short srcPort, short dstPort, int timeout, DatagramService datagramService){
        this.srcIP = srcIP;
        this.dstIP = dstIP;
        this.srcPort = srcPort;
        this.dstPort = dstPort;

        this.datagramService = datagramService;

        this.timer = new Timer();
        this.timeout = timeout;
    }

    /**
     * (1) wrap data into segment (2) update client-receiver seq
     */
    public Segment createNewSegment(byte[] originData, int flag, int ack, int seq){
        byte[] data = Util.preprocessData(originData);
        Segment segment = new Segment(data,seq,ack,flag);
        // update client-receiver's seq#
        client_receiver.update_client_seq_num(segment);

        return segment;
    }

    /**
     * Use datagramService to send the encapsulated segment
     * combined encapsulate and this one
     *
     * @param segment
     */
    public void sendOutSegment(Segment segment){
        /* encapsulate */
        short checksum = Util.checksum(segment.getData());
        short size = (short)Util.getSegmentSize(segment);
        Datagram datagram = new Datagram(this.srcIP,this.dstIP,this.srcPort,this.dstPort,size,checksum,segment);

        try {
            datagramService.sendDatagram(datagram);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setClientReceiver(ClientReceiverRunnable rcv){
        this.client_receiver = rcv;
    }
}
