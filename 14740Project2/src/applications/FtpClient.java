/*
 * FTP client
 *
 * @Author:
 * xou
 * ychu1
 */

package applications;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import services.DatagramService;
import services.TTPService;
import services.Util;

/**
 * FTP client, providing port and file name
 * file will be stored in client folder
 */
public class FtpClient {
	private static String clientFolderPath = System.getProperty("user.dir")+"/client_folder";

	private static DatagramService ds;
	private static TTPService ts;
	public static final int buflen = 1024*200;
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException {
		if(args.length != 5) {
			System.out.println("Usage: java -cp . applications.FtpClient <client port> <server port> <filename> <window size> <timeout>\n");
			System.exit(-1);
		}

		File clientDir = new File(clientFolderPath);
		if(!clientDir.exists()){
			clientDir.mkdir();
		}

		short clientPort = Short.parseShort(args[0]);
		short serverPort = Short.parseShort(args[1]);
		String fileName = args[2];
		int windows_size = Integer.parseInt(args[3]);
		int timeout = Integer.parseInt(args[4]);

//		short clientPort = Short.parseShort("20000");
//		short serverPort = Short.parseShort("10000");
////		String fileName = "jsontree1.txt";
//		String fileName = "Project2-Handout.pdf";
//		int windows_size = Integer.parseInt("10000");
//		int timeout = Integer.parseInt("1000");

		System.out.println("Starting client ...");
		
		ds = new DatagramService(clientPort, 10);
		ts = new TTPService(ds);
		
		ts.client_open_connection(timeout,windows_size, clientPort, serverPort);
		System.out.println("Client Connection established.\n");
		
		/* send filename to the server*/
		ts.client_fetch_file(fileName);
		System.out.println("Client has sent request with filename "+fileName);

		// start to receive data
		ts.client_start_receive_file();
		System.out.println("Transfer Finished");
		
        //md5
		byte[] md5_info = ts.getClientReceiver().MD5_Resp_Value;
		byte[] md5_body = new byte[md5_info.length];
		System.arraycopy(md5_info, 0, md5_body, 0, md5_info.length);

	    // receive data and verification
	    File reseivedFile = ts.client_receive_file();
	    System.out.println("file length: "+reseivedFile.length());
	    
	    byte[] bytefile = new byte[(int)reseivedFile.length()];
	    InputStream fileInputStream = null;
	    
	    try {
	    	fileInputStream = new FileInputStream(reseivedFile);
			
	    	File file =new File(clientFolderPath+"/"+fileName);

			if(file.exists()){
				file.delete();
			}

			FileOutputStream FileOutputStream = new FileOutputStream(file,true);
			BufferedOutputStream BufferedOutputStream = new BufferedOutputStream(FileOutputStream);
			
			boolean first = true;
			while ( fileInputStream.read(bytefile) != -1) {
				if (first) {
					byte[] md5_compute = Util.getMd5(bytefile);
					
					if (Arrays.equals(md5_body, md5_compute)) {
						// computed and reference md5 are identical
						System.out.println("MD5 Verified");
						// store the data in local file
					} else {
						System.out.println("Error: MD5 not match");
						System.exit(-1);
					}
					first = false;
				}
				
				BufferedOutputStream.write(bytefile);
	        } 
			BufferedOutputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	    /*close connection*/
	    ts.client_close_connection();
	    System.out.println("Client Connection closed.");
		System.out.println("Client Exit Without Error.");
		System.exit(0);
	    
	}
}
