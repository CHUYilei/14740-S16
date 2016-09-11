package services;

import datatypes.Datagram;
import datatypes.Segment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Anthor:
 * xou
 * ychu1
 * March 16
 * @reference: https://github.com/venkatesh5789/TTP/blob/master/src/services/TTPConnEndPoint.java
 * @reference: https://github.com/wentianqi7/ReliableTransportOverUDP
 */
public class TTPService {

    private DatagramService datagramService;    //each sender/receiver thread would have its datagram service

    /* window */
    private int server_window_size;
    private int client_window_size;

    /* connection status */
    private int client_status;
    private int server_status;

    /* client/server sender/receiver */
    public ServerSender server_sender;
    private ClientSender client_sender;
    private ClientReceiverRunnable client_receiver;
    private ServerReceiverRunnable server_receiver;

    /* addr/port */
    private String clientIP = "127.0.0.1";
    private String serverIP = "127.0.0.1";

    /* constructor */
    public TTPService(DatagramService datagramService){
        this.datagramService = datagramService;
    }

    /* client methods */
    /**
     * client wants to establish connection with server
     * by sending SYN and client's initial sequence number
     */
    public void client_open_connection(int client_timeout,int client_window_size,short client_port,short server_port){
        init_client(client_timeout,client_window_size,client_port,server_port);

        System.out.println("Client has been initialized");

        // send SYN
        Segment synSegment = client_sender.createNewSegment(null,Util.SYN,0,Util.CLIENT_INITIAL_SEQNUM);
        client_sender.sendOutSegment(synSegment);
        System.out.println("Client sends out SYN");
        client_receiver.run();

        client_status = Util.OPEN;
    }

    /**
     * client wrap file name as data in request and
     * send to server with flag FILE_FETCH
     */
    public void client_fetch_file(String filename){
        client_status = Util.FILE_FETCH;

        byte[] data = filename.getBytes(Charset.forName("UTF-8"));
        Segment fetchFileSegment = client_sender.createNewSegment(
                data,Util.FILE_FETCH,client_receiver.get_ack_number(),client_receiver.get_sequence_number());
        client_sender.sendOutSegment(fetchFileSegment);
        System.out.println("Client send fetch request for "+filename);
    }

    /**
     * can combine with retrieve file
     */
    public void client_start_receive_file(){
        this.client_receiver.run();
    }

    /**
     * initialize server and run server receiver
     */
    public void server_start_listen_connection(int server_timeout, int server_window_size,
                                               short client_port, short server_port, Datagram fileFetchDatagram){

        init_server(server_timeout,server_window_size,client_port,server_port,fileFetchDatagram);
        this.server_receiver.run();
        System.out.println("server start listing");
        server_status = Util.OPEN;
    }

    /**
     * server "send a file" command from upper layer
     */
    public void server_send_whole_file(byte[] fileContent){
        server_sender.sendWholeFileContent(fileContent);
        this.server_receiver.run();
    }

    /**
     * write received content into a temporary file
     * @return
     */
    public File client_receive_file(){
        ArrayList<byte[]> contentList = client_receiver.reassemble_file();
        System.out.println("client received content size: "+contentList.size());
        //create a temporary file using current time and start write
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmssSS").format(new Date());
        File tempFile = new File(timestamp);
        FileOutputStream outStream = null;

        try {
            outStream = new FileOutputStream(tempFile, true);
            for(byte[] cur: contentList){
                outStream.write(cur);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(outStream != null){
                try {
                    outStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println("[Client] receive file: "+tempFile.length()+" bytes");
        return tempFile;
    }

    /**
     * client close connection
     */
    public void client_close_connection(){
        Segment finSegment = client_sender.createNewSegment(
                null,Util.FIN,client_receiver.get_ack_number(),client_receiver.get_sequence_number());
        client_sender.sendOutSegment(finSegment);

        client_status = Util.FIN_WAIT;
    }

    /**
     * Establish client sender/receiver
     * sender needs addr/port & timeout value, receiver no need
     */
    private void init_client(int client_timeout,int client_window_size,short client_port,short server_port){
        this.client_sender = new ClientSender(this.clientIP,this.serverIP,
                client_port,server_port,client_timeout,this.datagramService);
        this.client_receiver = new ClientReceiverRunnable(this.datagramService,client_window_size);

        // register each other
        this.client_sender.setClientReceiver(client_receiver);
        this.client_receiver.setClientSender(client_sender);
    }

    /**
     * Establish server sender/receiver
     */
    private void init_server(int server_timeout, int server_window_size, short client_port, short server_port, Datagram fileFetchDatagram){
        this.server_sender = new ServerSender(this.serverIP,this.clientIP,
                server_port,client_port,server_timeout,this.datagramService,server_window_size);
        this.server_receiver = new ServerReceiverRunnable(this.datagramService,fileFetchDatagram);

        // register each other
        this.server_sender.setServerReceiver(server_receiver);
        this.server_receiver.setServerSender(server_sender);
    }

    /* setter and getter */
    public int get_client_status() {
        return client_status;
    }
    public void set_client_status(int client_status) {
        this.client_status = client_status;
    }
    public int get_server_status() {
        return server_status;
    }
    public void set_server_status(int server_status) {
        this.server_status = server_status;
    }

    public void shutDownServerTimer(){
        this.server_sender.stopTimer();
    }

    public ClientReceiverRunnable getClientReceiver(){
        return this.client_receiver;
    }
    public ServerReceiverRunnable getServerReceiver(){
        return this.server_receiver;
    }
}
