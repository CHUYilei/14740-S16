/**
 * 14740 Project 1
 *
 * @Author: Yilei CHU
 * @Andrew ID: ychu1
 *
 * @Date: 2016 Feb 18
 */


import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Server thread class
 * Read request from client socket and process
 */
public class ServerExecutor implements Runnable{

    private Socket clientSock;  // client socket that accepted by server
    private String wwwDir;      // absolute path to www directory

    public ServerExecutor(Socket clientSock,String wwwDir){
        this.clientSock = clientSock;
        this.wwwDir = wwwDir;
    }

    @Override
    public void run() {
        BufferedReader inStream = null;
        DataOutputStream outStream = null;
        String request = null; // the request read from client socket
        String response = null; // the response to send to client

        try {
            inStream = new BufferedReader(new InputStreamReader(
                    clientSock.getInputStream()));
            outStream = new DataOutputStream(clientSock.getOutputStream());

            /* Read the request from the client */
            request = inStream.readLine();

            if(request == null || (request.trim()).isEmpty()){
                System.err.println("Empty request has been received. Thread exists.");
                return;
            }

            System.err.println("Read request from client "
                    + clientSock.getInetAddress() + ":"
                    + clientSock.getPort() + " " + request);

            /* Process the request and prepare response */
            ParseRequest parser = new ParseRequest(request,wwwDir);
            response = parser.respond();
            outStream.writeBytes(response);

            // for debugging
            //outStream.writeBytes(request);

            outStream.flush();

        } catch (IOException e) {
            System.err.println("I/O exception in server executor");
        } finally {
            /* close the client's socket */
            if(this.clientSock != null){
                try {
                    clientSock.close();
                } catch (IOException e) {
                    System.err.println("I/O exception in closing client socket");
                }
            }
        }
    }
}
