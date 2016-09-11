package services;

import java.io.IOException;
import java.util.ArrayList;

import datatypes.Datagram;
import datatypes.Segment;

/**
 * @Author:
 * xou
 * ychu1
 * @reference: https://github.com/venkatesh5789/TTP/blob/master/src/services/TTPConnEndPoint.java
 * @reference: https://github.com/wentianqi7/ReliableTransportOverUDP
 * Client-receiver
 */
public class ClientReceiverRunnable implements Runnable{

	/** Previous Layer API and utility */
	private DatagramService Datagramservice;
	private Datagram datagram;
	
	/** TTP_Segment_Thread Utility */
	private ReceiverChecksum Rec_Thread;
	private ClientSender Sen_Thread;
	
	/** TTP_Segment_Client_Receiver_Thread connection management */
	private ArrayList<Segment> correct_order_segment;
	private ArrayList<byte[]> return_data = new ArrayList<byte[]>();
	private int Seq_Num = Util.CLIENT_INITIAL_SEQNUM;
	private int Ack_Num =0;
	private int options=0;
	private int pre_options=0;
	private int window_size =0;
	private static int MAX_WINDOWS_SIZE;

	public byte[] MD5_Resp_Value;
	public String tempPath;

	/**
	 * Constructor
     */
	public ClientReceiverRunnable(DatagramService datagramService, int size) {
		System.err.println("construct client receiver");
		correct_order_segment = new ArrayList<Segment>();
		Rec_Thread = new ReceiverChecksum();
		Datagramservice = datagramService;	
		MAX_WINDOWS_SIZE  = size;
	}
	
	/* reassemble the correct-order-file by reading from segment from correct_order_segment */
	int i=0;
	public ArrayList<byte[]> reassemble_file(){
		for (Segment segment : correct_order_segment) {
				System.out.println("in order buffer :"+i++);
				return_data.add(segment.getData());
		}
		return return_data;
		
	}
	
	int j=-1;
	int k=0;
	int sum =0;

	/* Client receiving handler */
	@Override
	public void run() {
		while(true){
			j++;
			try {
				datagram = Datagramservice.receiveDatagram();
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}
			/* check received datagram checksum*/
			try {
				if(!Rec_Thread.checksum(datagram)) {
					System.out.println("check sum errror");
					continue;
				}
			} catch (IOException e) {
				System.out.println("check sum exception");
				e.printStackTrace();
			}
			System.out.println("check sum correct");
			/* check md5*/
			Segment segment = (Segment)datagram.getData();

			
			/* If checksum is correct */
			int flag = segment.getFlag();
			if(flag == Util.MD5){
				this.MD5_Resp_Value = segment.getData();
				options = Util.MD5;
				Ack_Num = segment.getSequenceNum() + Util.getSegmentSize(segment);
				
				/*create TTP-segment of reply*/
				Segment newseg2 = Sen_Thread.createNewSegment(null,Util.ACK,Ack_Num,Seq_Num);
				Sen_Thread.sendOutSegment(newseg2);
			}else if(flag == Util.SYN_ACK){
				options = Util.SYN_ACK;
						
				/* Sequence number and acknowledge number management */
				Ack_Num = segment.getSequenceNum() + Util.getSegmentSize(segment);
						
				/*create TTP-segment of reply*/
				Segment newseg = Sen_Thread.createNewSegment(null,Util.SYN_ACK,Ack_Num,Seq_Num);
				Sen_Thread.sendOutSegment(newseg);
				
			
			}else if(flag == Util.FILE_TRANSFER){
				/** Connection status management */
				System.out.println("entering file transfer");
				options = Util.FILE_TRANSFER;
				
				if (pre_options == Util.RETRANSFER) {
					Ack_Num += Util.getSegmentSize(segment);
					pre_options = 0; //?
				}
				
				if (segment.getSequenceNum() != Ack_Num) {
					System.out.println("The number :"+j +"datagram receive  "+" [error] + ack number :"+ Ack_Num  );
					System.out.println("The number :"+j +"datagram receive  "+" [error] + seg number :"+ segment.getSequenceNum() );
					/*create TTP-segment of reply*/
					Segment newseg2 = Sen_Thread.createNewSegment(null,Util.ACK,Ack_Num,Seq_Num);
					Sen_Thread.sendOutSegment(newseg2);
					
				}
				
				/** In-order sequence packet */
				if (segment.getSequenceNum() == Ack_Num) {
					
					System.out.println("The number :"+j +"datagram receive  "+" [good] + ack number :"+ Ack_Num );
					sum += segment.getData().length;
					Ack_Num = segment.getSequenceNum() + Util.getSegmentSize(segment);
					correct_order_segment.add(segment);
					window_size ++;
					
					/*create TTP-segment of reply*/
					Segment newseg2 = Sen_Thread.createNewSegment(null,Util.ACK,Ack_Num,Seq_Num);
					Sen_Thread.sendOutSegment(newseg2);
					
					//window size 有没有用？
					if (window_size == MAX_WINDOWS_SIZE) {
						window_size = 0;
						
						/*create TTP-segment of reply*/
						Segment newseg22 =  Sen_Thread.createNewSegment(null,Util.ACK,Ack_Num,Seq_Num);
						Sen_Thread.sendOutSegment(newseg22);
						
					}
				}
				
			}else if(flag == Util.FILE_EOF){
			
				options = Util.FILE_EOF;
				/*create TTP-segment of reply*/
				Ack_Num = segment.getSequenceNum() + Util.getSegmentSize(segment);
				Segment newseg22 =  Sen_Thread.createNewSegment(null,Util.FIN,Ack_Num,Seq_Num);
				Sen_Thread.sendOutSegment(newseg22);
				
			
			}else if(flag == Util.RETRANSFER){

				Ack_Num = segment.getSequenceNum();
				pre_options = Util.RETRANSFER;
						
			}else if(flag == Util.FIN_ACK){
				/** Connection status management */
				System.out.println("enter finack");
				options = Util.CLOSE;
				
			}else if(flag == Util.CLOSE){
				options = Util.CLOSE;
			}else{
				options = Util.CLOSE;
			}
			
			if (options == Util.SYN_ACK ||options== Util.CLOSE) {
				break;
			}
			
			
		}
		System.out.println("sum data:"+ sum );
		System.out.println("correct order seg:"+correct_order_segment.size());
	}
	/** GETTER and SETTER Functions */
	public int get_sequence_number() {
		return this.Seq_Num;
	}
	
	
	public int get_ack_number() {
		return Ack_Num;
	}


	public void setClientSender(ClientSender client_sender) {
		Sen_Thread = client_sender;
	}
	
	public void update_client_seq_num(Segment segment){
		
	   this.Seq_Num += Util.getSegmentSize(segment);
	}

}
