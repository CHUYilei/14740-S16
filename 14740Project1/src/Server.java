/**
 * 14740 Project 1
 *
 * @Author: Yilei CHU
 * @Andrew ID: ychu1
 *
 * @Date: 2016 Feb 18
 */

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Server class (with main method)
 */
public class Server {
	private static ServerSocket srvSock;

	public static void main(String args[]) {
		String buffer = null;
		int port = 8080;	// default listening port
		String wwwDir = "";
		BufferedReader inStream = null;
		DataOutputStream outStream = null;

		/* Parse parameter and do args checking */
		if (args.length != 2) {
			System.err.println("Usage: java Server <port_number> <Absolute path to www directory>");
			System.exit(1);
		}

		/* Get server's port */
		try {
			port = Integer.parseInt(args[0]);
		} catch (Exception e) {
			System.err.println("Usage: java Server <port_number>");
			System.exit(1);
		}

		if (port > 65535 || port < 1024) {
			System.err.println("Port number must be in between 1024 and 65535");
			System.exit(1);
		}

		/* Get absolute path to www directory */
		wwwDir = args[1];

		// check whether www dir path valid
		File f = new File(wwwDir);
		if(!f.exists() || !f.isDirectory()){
			System.err.println(wwwDir+" is not a valid directory");
			System.exit(1);
		}

		/*
		 * Create a socket to accept() client connections. This combines
		 * socket(), bind() and listen() into one call. Any connection
		 * attempts before this are terminated with RST.
		 */
		try {
			srvSock = new ServerSocket(port);
		} catch (IOException e) {
			System.err.println("Unable to listen on port " + port);
			System.exit(1);
		}

		/* start accepting client socket */
		while (true) {
			/*
			 * Get a sock for further communication with the client.
			 * This socket is sure for this client.
			 * Further connections are still accepted on srvSock.
			 */
			try {
				Socket clientSock = srvSock.accept();
				System.err.println("Accpeted new connection from "
						+ clientSock.getInetAddress() + ":"
						+ clientSock.getPort());

				// create new executor thread to handle request (socket close inside thread)
				ServerExecutor executor = new ServerExecutor(clientSock,wwwDir);
				Thread handler = new Thread(executor);
				handler.start();

			} catch (IOException e) {
				continue;
			}


		}
	}
}
