package services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Random;

import datatypes.Datagram;

/**
 * Manipulated datagram service:
 * error cases included
 * @reference: https://github.com/venkatesh5789/TTP/blob/master/src/services/TTPConnEndPoint.java
 * @reference: https://github.com/wentianqi7/ReliableTransportOverUDP
 */
public class DatagramService {

	private int port;
	private int verbose;
	private DatagramSocket socket;
	private int totalcase = 1000;
	private int random_num;
	

	public DatagramService(int port, int verbose) throws SocketException {
		super();
		this.port = port;
		this.verbose = verbose;

		socket = new DatagramSocket(port);
	}

	/**
	 * Sender with error cases include
	 * @param datagram
	 * @throws IOException
     */
	public void sendDatagram(Datagram datagram) throws IOException{

		ByteArrayOutputStream bStream = new ByteArrayOutputStream(1500);
		ObjectOutputStream oStream = new ObjectOutputStream(bStream);
		oStream.writeObject(datagram);
		oStream.flush();

		// Create Datagram Packet
		byte[] data = bStream.toByteArray();
		InetAddress IPAddress = InetAddress.getByName(datagram.getDstaddr());
		DatagramPacket packet = new DatagramPacket(data, data.length,
				IPAddress, datagram.getDstport());

		//test case:
		// Random rand = new Random();
		// random_num=rand.nextInt(totalcase);
		
		// /*test cases with error cases */
		// if(random_num%200 == 0) {
		// 	System.out.println("Testing Delayed Packets(out-of-order Packets)");
		// 	Random random1 = new Random();
		// 	int delay = random1.nextInt(300) + 200;
		// 	try{
		// 		Thread.sleep(delay);
		// 	}catch (InterruptedException e) {
		// 		e.printStackTrace();
		// 	}
		// 	socket.send(packet);
		// 	System.out.println("Packet sent with delay:" + delay);
		// } else if(random_num%200 == 1) {
		// 	System.out.println("Testing Duplicate Packets");
		// 	Random random2 = new Random();
		// 	int duplicatenum = random2.nextInt(5)+2;
		// 	for(int i = 0; i< duplicatenum; i++)
		// 		socket.send(packet);
		// 	System.out.println("Packet sent " + duplicatenum + " times");
		// } else if(random_num%200 == 3) {
		// 	System.out.println("Testing Dropped Packets");
		// } else if(random_num%200 == 4) {
		// 	System.out.println("Testing Checksum Error");
		// 	datagram.setChecksum((short) 100);
		// } else {
		// 	socket.send(packet);
		// } 
		
		// Send packet
		socket.send(packet);
		System.out.println("datagramservice send out");
	}

	public Datagram receiveDatagram() throws IOException,
			ClassNotFoundException {

		byte[] buf = new byte[1500];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);

		socket.receive(packet);
		ByteArrayInputStream bStream = new ByteArrayInputStream(
				packet.getData());
		ObjectInputStream oStream = new ObjectInputStream(bStream);
		Datagram datagram = (Datagram) oStream.readObject();

		return datagram;
	}
}
