package services;

import datatypes.Segment;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Author:
 * xou
 * ychu1
 * @reference: https://github.com/venkatesh5789/TTP/blob/master/src/services/TTPConnEndPoint.java
 * @reference: https://github.com/wentianqi7/ReliableTransportOverUDP
 * March 16
 *
 * Util class for storing constants like flags and util functions
 */
public abstract class Util {
    /* flags in payload */
    public static final int SYN = 1;
    public static final int SYN_ACK = 2;
    public static final int ACK = 3;
    public static final int FIN = 4;
    public static final int FIN_ACK = 5;
    public static final int FILE_FETCH = 6;     // contain file name
    public static final int FILE_TRANSFER = 7;  // file content
    public static final int FILE_EOF = 8;       // end of file, no file content, show transfer finish
    public static final int MD5 = 9;  //not eof, only fragment of a file

    /* connection status */
    public static final int RETRANSFER = 11;     // this segment is a retransfer
    public static final int OPEN = 12;
    public static final int CLOSE = 13;
    public static final int FIN_WAIT = 14;

    /* initial sequence number */
    public static final int CLIENT_INITIAL_SEQNUM = 14740;
    public static final int SERVER_INITIAL_SEQNUM = 15650;

    /* Util functions */

    /**
     * Get size of segment (for setting seq# and ack#) as number of bytes
     * @param segment
     * @return
     */
    public static int getSegmentSize(Segment segment){
        ByteArrayOutputStream byteOutputStream = null;
        ObjectOutputStream objectOutputStream = null;
        try {
            byteOutputStream = new ByteArrayOutputStream();
            objectOutputStream = new ObjectOutputStream(byteOutputStream);
            objectOutputStream.writeObject(segment);
            objectOutputStream.flush();
            objectOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return byteOutputStream.toByteArray().length;
    }

    /**
     * Checksum on content
     * @param content payload.getContent()
     * @return
     * @throws IOException
     */
    public static short checksum(byte[] content){
        int sum =0;
        int highbyte;
        int lowbyte;
        if(content.length%2==0){
            for(int i=0;i<content.length;i=i+2){
                highbyte = (content[i] << 8) & 0xff00;
                lowbyte = (content[i + 1]) & 0xff;
                sum = sum + highbyte + lowbyte;
                if((sum & 0xf0000)!=0){
                    sum = sum & 0xffff;
                    sum = sum+1;
                }
            }
        }else{
            highbyte = (content[content.length-1]<<8) & 0xff00;
            sum =sum + highbyte;
            if((sum & 0xf0000)!=0){
                sum = sum & 0xffff;
                sum = sum+1;
            }
        }
        return (short)(~sum & 0xffff);
    }

    /**
     * Preprocess data, create a byte for empty data
     */
    public static byte[] preprocessData(byte[] data){
        if(data == null || data.length == 0){
            return new byte[1];
        } else {
            return data;
        }
    }

    /**
     * MD5 for whole file
     */
    public static byte[] getMd5(byte[] content){
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
            md.update(content);
            byte[] digest = md.digest();
            StringBuffer sb = new StringBuffer();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return String.valueOf(sb).getBytes();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

}
