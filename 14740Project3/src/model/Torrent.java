package model;

import java.util.List;

/**
 * This class simulates the .torrent file
 * it contains file list to be downloaded
 * the real peers of each chunk of each file needs to be checked from tracker
 *
 * @Author:
 * Xiaocheng OU
 * Yilei CHU
 */
public class Torrent {
    private List<TargetFile> fileList;

    public Torrent(List<TargetFile> fileList) {
        this.fileList = fileList;
    }

    public List<TargetFile> getFileList() {
        return fileList;
    }
}