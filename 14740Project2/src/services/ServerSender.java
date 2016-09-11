package services;

import datatypes.Datagram;
import datatypes.Segment;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Author:
 * xou
 * ychu1
 * @reference: https://github.com/venkatesh5789/TTP/blob/master/src/services/TTPConnEndPoint.java
 * @reference: https://github.com/wentianqi7/ReliableTransportOverUDP
 * March 17
 *
 * Server sender
 * would handle seq# when create a new segment
 */

public class ServerSender {
    private DatagramService datagramService;

    /* addr/port */
    private String srcIP;
    private String dstIP;
    private short srcPort;
    private short dstPort;

    /* registered receiver */
    private ServerReceiverRunnable server_receiver;

    /* queue for segments */
    private Queue<Segment> sendBuffer;     /* segment queue for sending out */
    private Queue<Segment> fragmentQueue; /* segment list after fragmentstion */

    /* timer and timeout value */
    private Timer timer;
    private int timeout;
    private ActionListener timerListener;

    /* window size */
    private static int maxWindowSize = -1;

    private static final int CHUNKSIZE = 500;

    /**
     * Constructor
     */
    public ServerSender(String srcIP, String dstIP, short srcPort, short dstPort, int timeout, DatagramService datagramService,int maxWindowSize){
        this.srcIP = srcIP;
        this.dstIP = dstIP;
        this.srcPort = srcPort;
        this.dstPort = dstPort;

        this.sendBuffer = new LinkedList<Segment>();
        this.fragmentQueue = new LinkedList<Segment>();
        this.datagramService = datagramService;

        this.maxWindowSize = maxWindowSize;

        this.timer = null;
        this.timeout = timeout;
        this.timerListener = new TimerListener(this);

    }

    /**
     * (1) wrap data into segment (2) update server-receiver seq
     */
    public Segment createNewSegment(byte[] originData, int flag, int ack, int seq){
        byte[] data = Util.preprocessData(originData);
        Segment segment = new Segment(data,seq,ack,flag);
        // update server-receiver's seq#
        server_receiver.update_server_seq_num(segment);

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

    /**
     * fragment the whole data into pieces,
     * put each piece into Fragment queue
     */
    private void fragmentation(byte[] originData){
        // chunk the data (null data will be handled by segment constructor)
        byte[] data = Util.preprocessData(originData);
        int totalLen = data.length;
        int remainLen = totalLen;

        while (remainLen > 0){
            int currentChunkSize = Math.min(remainLen,CHUNKSIZE);
            byte[] buffer = new byte[currentChunkSize];
            System.arraycopy(originData, totalLen-remainLen,buffer, 0, currentChunkSize);

            Segment segment = createNewSegment(buffer,Util.FILE_TRANSFER,this.server_receiver.get_ack_number(),this.server_receiver.get_sequence_number());
            this.fragmentQueue.offer(segment);

            remainLen -= currentChunkSize;
        }

        // add a segment to indicate end of file
        Segment indicatorSegment = createNewSegment(null,Util.FILE_EOF,this.server_receiver.get_ack_number(),this.server_receiver.get_sequence_number());
        this.fragmentQueue.offer(indicatorSegment);
    }

    /**
     * TTP layer version of "send a whole file"
     * will handle fragmentation and send out
     *
     * Note: content may still be part of file
     */
    public void sendWholeFileContent(byte[] content){
        // do MD5 and send the MD5 in a separate segment
        byte[] md5Value = Util.getMd5(content);
        Segment md5Segment = createNewSegment(md5Value,Util.MD5,this.server_receiver.get_ack_number(),this.server_receiver.get_sequence_number());
        this.fragmentQueue.offer(md5Segment);

        // fragment and add into fragment list
        fragmentation(content);

        // start timer (timer serves for a whole file, TODO after finish, timer should be set to null)
        if(this.timer == null){
            this.timer = new Timer(timeout*60, this.timerListener);
            timer.start();
        } else{
            System.out.println("[Error] Timer is not null at beginning of file");
            timer.restart();
        }

        // send out according to window size
       // slideWindow();
        new Thread(new SenderRunnable()).start();
    }

    public void setServerWindowSize(int N){
        maxWindowSize = N;
    }

    public void setServerReceiver(ServerReceiverRunnable rcv){
        this.server_receiver = rcv;
    }

    /**
     * Advance the window
     * only send those new segemnts newly fetched from fragment list
     * after sending out, put these fragments into senderBuffer
     * sendBuffer only store those sentNotAcked
     */
    public void slideWindow(){
        boolean isFirstSegment = true;

        // put segments to send into sender buffer, waiting for send out
        while(sendBuffer.size() < this.maxWindowSize && !fragmentQueue.isEmpty()){
            Segment segment = fragmentQueue.poll();
            System.out.println("server send segment :"+segment.getFlag());
            sendOutSegment(segment);
            sendBuffer.offer(segment);

            if(isFirstSegment){
                restartTimer();
                isFirstSegment =  false;
            }
        }
    }

    /**
     * retransmit everything inside senderBuffer
     */
    public void retransmitAllSentNotAcked(){
        for(Segment segment: sendBuffer){
            sendOutSegment(segment);
        }
    }

    /**
     * Delete all segments from sender buffer which has been acked
     */
    public void deleteSentAckedSegments(int ack){
        while(!sendBuffer.isEmpty()){
            if(sendBuffer.peek().getSequenceNum() < ack){   // not <=, ack is expected, not received
                sendBuffer.poll();
            }else{
                return;
            }
        }
    }

    /**
     * getter
     */
    public boolean isFragmentQueueEmpty(){
        return fragmentQueue.isEmpty();
    }

    /* timer handling */
    public void restartTimer(){
        if(this.timer != null)
		this.timer.restart();
    }

    public void stopTimer(){
    	System.err.println("Server sender timer is shutdown!");
        if(this.timer != null){
		this.timer.stop();
        	this.timer = null;
	}
    }

    /**
     * new thread merely for sending
     */
    private class SenderRunnable implements Runnable {

        public void run() {
            slideWindow();
        }
    }

}
