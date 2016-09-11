package applications;

import datatypes.Datagram;
import datatypes.Segment;
import services.DatagramService;
import services.TTPService;
import services.Util;

import java.io.IOException;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

/**
 * FTP server
 * handle client's file request
 *
 * @Author:
 * Xiaocheng OU
 * Yilei CHU
 */
public class FtpServer {
    private short serverPort;
    private int serverWindowSize;
    private int timeout;
    private DatagramService datagramService;
    Map<Short, TTPService> clientMap;

    public FtpServer(short serverPort,int serverWindowSize,int timeout){
        this.serverPort = serverPort;
        this.serverWindowSize = serverWindowSize;
        try {
            this.datagramService = new DatagramService(serverPort,1);
        } catch (SocketException e) {
            e.printStackTrace();
            System.exit(1);
        }
        this.clientMap = new HashMap<Short, TTPService>();
        this.timeout = timeout;
    }

    /**
     * Start handling any client by using a new thread
     */
    public void start(){
        System.out.println("Server starting...");
        while(true){
            try {
                Datagram datagram  = this.datagramService.receiveDatagram();
                Segment seg = (Segment)datagram.getData();
                int flag = seg.getFlag();

                if(flag == Util.SYN){
                    TTPService ttpService = new TTPService(this.datagramService);
                    ttpService.set_server_status(Util.CLOSE);
                    this.clientMap.put(datagram.getSrcport(),ttpService);
                    ServerExecutor executor = new ServerExecutor(ttpService,datagram,this.serverPort,this.serverWindowSize,this.timeout);
                    System.out.println("[FTPServer] start server executor");
                    Thread serverThread = new Thread(executor);
                    serverThread.start();
                }else{
                    TTPService ttpService = clientMap.get(datagram.getSrcport());
                    ttpService.getServerReceiver().datagramlist.add(datagram);
                    ttpService.getServerReceiver().semaphore.release(1);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

    }

    public static void main(String[] args) {
        if(args.length != 3) {
            System.out.println("Usage: java -cp . applications.FtpServer <server port> <window size> <timeout>\n");
            System.exit(-1);
        }

        short serverPort = Short.parseShort(args[0]);
        int windowSize = Integer.parseInt(args[1]);
        int timeout = Integer.parseInt(args[2]);

        FtpServer server = new FtpServer(serverPort,windowSize,timeout);
        server.start();
    }
}
