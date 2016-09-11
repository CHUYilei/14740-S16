package applications;

import datatypes.Datagram;
import services.TTPService;

import java.io.*;

/**
 * @Author:
 * Xiaocheng OU
 * Yilei CHU
 *
 * Ftp request handler for server
 */
public class ServerExecutor implements Runnable {
    private TTPService ttpService;
    private Datagram synDatagram;
    private short serverPort, clientPort;
    private int maxServerWindowSize;
    private int timeout;

    /**
     * Constructor
     */
    public ServerExecutor(TTPService ttpService, Datagram firstDatagram, short serverPort, int maxServerWindowSize, int timeout) {
        this.ttpService = ttpService;
        this.synDatagram = firstDatagram;
        this.serverPort = serverPort;
        this.maxServerWindowSize = maxServerWindowSize;
        this.timeout = timeout;
    }

    /**
     * handle one file request by sending out the file
     */
    @Override
    public void run() {
        this.clientPort = synDatagram.getSrcport();
        System.out.println("Handle client at port "+clientPort);

        ttpService.server_start_listen_connection(this.timeout,this.maxServerWindowSize,this.clientPort,this.serverPort, synDatagram);
        String fileName = new String((byte[])ttpService.getServerReceiver().data);
        System.out.println("Server serves file "+fileName);

        // read file and send content
        // md5 is included in serverSender's send whole file
        RandomAccessFile f = null;
        try {
            f = new RandomAccessFile(fileName, "r");
            byte[] content = new byte[(int)f.length()];
            f.read(content);
            System.out.println("total length would be "+f.length());
            
            ttpService.server_send_whole_file(content);
            System.out.println("Server sends out file, length "+content.length);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Thread.currentThread().interrupt();
        return;
    }
}
