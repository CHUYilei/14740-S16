package model;

/**
 * @Author:
 * Xiaocheng OU
 * Yilei CHU

 simulate the bandwidth condition in each node, 
 used for selecting faster and stronger nodes 
 in peer selection
 */
public class Bandwidth {
    private double uploadBandwidth;
    private double downloadBandwidth;

    public Bandwidth(double uploadBandwidth, double downloadBandwidth) {
        this.uploadBandwidth = uploadBandwidth;
        this.downloadBandwidth = downloadBandwidth;
    }

    public double getUploadTime(double size){
        return size/uploadBandwidth;
    }

    public double getDownloadBandwidth(double size){
        return size/downloadBandwidth;
    }
    
    public String toString(){
        return "upload - "+uploadBandwidth+",download - "+downloadBandwidth;
    }
}
