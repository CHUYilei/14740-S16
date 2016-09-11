package model;

/**
 * This class simulates the message between peers
 * the communication is initiated in CommunicationUtils
 *
 * @Author:
 * Xiaocheng OU
 * Yilei CHU
 */
public class Message {
    /* request opcode */
    public static int HANDSHAKE = 1; //peer A wants to establiosh connection with B
    public static int GET_CHUNKS = 2; //peer A wants to get chunks from B

    /* response opcode */
    public static int REFUSE = 3; //peer B refuse A's connection establishment request
    public static int AGREE = 4; //peer B agrees A's connection establishment request
    public static int OFFLINE = 5; //peer B is currently offline (TODO may not be used)
    public static int GIVE_CHUNKS = 6; //peer B responds to A's GET_CHUNKS request with chunk content

    private int opcode;
    private int chunkSize; //chunk size
    private int fileID,chunkID;

    public int getFileID() {
        return fileID;
    }

    public void setFileID(int fileID) {
        this.fileID = fileID;
    }

    public int getChunkID() {
        return chunkID;
    }

    public void setChunkID(int chunkID) {
        this.chunkID = chunkID;
    }

    public Message(int opcode) {
        this.opcode = opcode;
    }

    public int getOpcode() {
        return opcode;
    }

	public int getChunkSize() {
		return chunkSize;
	}

	public void setChunkSize(int chunkSize) {
		this.chunkSize = chunkSize;
	}
    
    
}
