package model;

import java.util.ArrayList;
import java.util.List;

/**
the real file that peers need to download and upload, 
containing meta data like size, 
which is useful for chunking(fragmentation).
 *
 * @Author:
 * Xiaocheng OU
 * Yilei CHU
 */
public class TargetFile {
    private int file_id;
    private List<Integer> chunkList; //list of chunksize, chunk_id starts from 0
    private int fileSize;
    private int chunkNum;

    public TargetFile(int fileID, int totalSize,int maxChunkSize){
        this.file_id = fileID;
        this.fileSize = totalSize;

        // fragmentation
        this.chunkNum = totalSize/maxChunkSize;

        this.chunkList = new ArrayList<Integer>();
        for (int i = 0; i < chunkNum; i++) {
            this.chunkList.add(maxChunkSize);
        }

        int diff = totalSize - chunkNum * maxChunkSize;
        if(diff > 0){
            ++this.chunkNum; //division has remainder
            this.chunkList.add(diff);
        }
    }

    public int getFile_id() {
        return file_id;
    }

    public List<Integer> getChunkList() {
        return chunkList;
    }

    public int getChunkNum() {
        return chunkNum;
    }

    public int getFileSize() {
        return fileSize;
    }
}
