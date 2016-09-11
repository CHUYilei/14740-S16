package datatypes;

import java.io.Serializable;

/**
 * Author:
 * xou
 * ychu1
 *
 * The real payload wrapped inside a datagram
 */
public class Segment implements Serializable {
    /* header inside payload */
    private int flag;       //such as ACK
    private int sequenceNum;
    private int ackNum;

    private byte[] data;    //the real data

    /**
     * constructor
     */
    public Segment(byte[] data, int sequenceNum, int ackNum, int flag){
        this.sequenceNum = sequenceNum;
        this.ackNum = ackNum;
        this.flag = flag;

        setData(data);
    }

    /**
     * constructor
     */
    public Segment(int sequenceNum, int ackNum, int flag, byte[] data){
        this.sequenceNum = sequenceNum;
        this.ackNum = ackNum;
        this.flag = flag;
        setData(data);
    }


    /**
     * Automatically handle empty data
     * @param data
     */
    public void setData(byte[] data){
        if(data == null || data.length == 0){   //empty data (such as merely SYN)
            this.data = new byte[1];
        }else{
            this.data = data;
        }
    }

    /* getter and setter */

    public int getFlag() {
        return flag;
    }

    public int getSequenceNum() {
        return sequenceNum;
    }

    public void setSequenceNum(int sequenceNum) {
        this.sequenceNum = sequenceNum;
    }

    public int getAckNum() {
        return ackNum;
    }

    public void setAckNum(int ackNum) {
        this.ackNum = ackNum;
    }

    public byte[] getData() {
        return data;
    }
}
