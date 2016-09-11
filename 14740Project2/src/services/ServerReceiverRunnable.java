package services;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

import datatypes.Datagram;
import datatypes.Segment;

/**
 * @Author
 * xou
 * ychu1
 * @reference: https://github.com/venkatesh5789/TTP/blob/master/src/services/TTPConnEndPoint.java
 * @reference: https://github.com/wentianqi7/ReliableTransportOverUDP
 * Server's receiver side
 */
public class ServerReceiverRunnable implements Runnable {

	/** Previous Layer API and utility */
	private DatagramService Datagramservice;
	private Datagram datagram;
	private boolean istrans =true;
	
	/** TTP API and utility to upper layer */
	public Object data;
	
	/** TTP_Segment_Server_Receiver_Thread instance */
	private ReceiverChecksum Rec_Thread;	
	private ServerSender Send_Thread;	
	
	/** TTP Segment variable: only used in server sender */
	private int Seq_Num=Util.SERVER_INITIAL_SEQNUM;
	private int Ack_Num;
	
	/** TTP Connection Management */	
	private int options;

	/** TTP Resource */
	public Semaphore semaphore= new Semaphore(0);
	public LinkedList<Datagram> datagramlist = new LinkedList<Datagram>();;
	
	
	/** Constructor */
	public ServerReceiverRunnable(DatagramService datagram_service, Datagram datagram) {
		Rec_Thread = new ReceiverChecksum();	
		this.Datagramservice = datagram_service;
		this.datagram = datagram;

	}
	
	/** go_back_N_send 
	 * This function is to sliding the sender's windows 
	 * */
	private void go_back_N_send(Segment segment) {
		Send_Thread.deleteSentAckedSegments(segment.getAckNum());
		/** Automatically Sliding the windows and send out the segment */
		Send_Thread.slideWindow();
	}

	/**
	 * Handler by flag in segment
	 */
	@Override
	public void run() {
		while (true) {
			if (istrans) {
				istrans = false;
			} else {
				/** Receiving datagrams and unwrapped it */
				try {
					semaphore.acquire(1);
					datagram = datagramlist.remove(0);
					semaphore.release(0);
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			/* check received datagram checksum*/
			try {
				if(Rec_Thread.checksum(datagram) == false)
					continue;
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			/* If checksum is correct */
			Segment segment = (Segment)datagram.getData();
			int flag = segment.getFlag();
			if(flag == Util.SYN){
				System.out.println("server receive syn");
				/** Connection Management */
				options = Util.SYN_ACK;
				/** Sequence number, acknowledge number management */
				Ack_Num = segment.getSequenceNum() + 1;
				
				/*create TTP-segment of reply*/
				Segment newseg = Send_Thread.createNewSegment(null,Util.SYN_ACK,Ack_Num,Seq_Num);
				Send_Thread.sendOutSegment(newseg);
				System.out.println("send out the syn_ack");
			
			}else if(flag == Util.SYN_ACK){
				/** Connection Management */
				options = Util.ACK;		
				
				/** Sequence number, acknowledge number management */
				Ack_Num++;
				
			}else if(flag == Util.ACK){
				/** Connection Management */
				System.out.println("enter ACK ");
				options = Util.ACK;
				
				/** Sequence number, acknowledge number management */
				Ack_Num++;
				
				go_back_N_send(segment);
				
			}else if(flag == Util.FILE_FETCH){
				/** Connection Management */
				options = Util.FILE_FETCH;
				
				/** Sequence number, acknowledge number management */
				Ack_Num++;
				
				data = segment.getData();
				
			}else if(flag == Util.FIN){
                System.out.println("server receive fin");
				/** Connection Management */
				options = Util.CLOSE;
				
				/** Sequence number, acknowledge number management */
				Ack_Num++;
				
				segment = Send_Thread.createNewSegment(segment.getData(), Util.FIN_ACK,Ack_Num,Seq_Num);
				Send_Thread.sendOutSegment(segment);
				Send_Thread.stopTimer();
										
			}else if(flag == Util.CLOSE){
				options = Util.CLOSE;
			}else{
				options = Util.CLOSE;
			}
			//server should run forever serving multiple clients ??
			if (options == Util.CLOSE || options == Util.FILE_FETCH) {
				break;
			}
			
		}
	}
	
	/** GETTER and SETTER Functions */
	public int get_sequence_number() {
		return this.Seq_Num;
	}
	
	public void set_sequence_number(int sequence_number) {
		this.Seq_Num = sequence_number;
	}	
	
	public int get_ack_number() {
		return Ack_Num;
	}

	public void set_ack_number(int ack_number) {
		this.Ack_Num = ack_number;
	}
	public void setServerSender(ServerSender sender) {
		Send_Thread = sender;
	}
	
	public void update_server_seq_num(Segment segment){
		
		this.Seq_Num += Util.getSegmentSize(segment);
	}
	

}
