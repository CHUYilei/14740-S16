package simulate;

import model.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class does the main simulation job
 * including initialization
 *
 * @Author:
 * Xiaocheng OU
 * Yilei CHU
 */
public class Simulation {
    /* constants */
	private static int MAX_CHUNKSIZE = 4; // 1K
    private static int MAX_FILESIZE = 1024 * 100; //10G
    private static int MIN_FILESIZE = 1024 * 1; //1K
    private static int MAX_STORAGECAPACITY = 40 * 10; //1T
    private static int MIN_STORAGECAPACITY = 0; //20G
    private static int MIN_BANDWIDTH = 1024*200; //200K
    private static int MAX_BANDWIDTH = 1024*1024*100; //10M

    private static int PEER_NUM = 3;               // default peer number
    private static int FILE_NUM = 3;

    /* maps storing objects */
    private static Map<Integer, Peer> peerMap; //<peer id,peer>
    private static Map<Integer, TargetFile> fileMap; // <file id,file>

    /**
     * Initialize all peers, files and maps
     */
    private void init(){
        // initialize all maps
        peerMap = new HashMap<Integer, Peer>();
        fileMap = new HashMap<Integer, TargetFile>();

        // <file id, <chunk id, list of peers owning this chunk>>
        ConcurrentHashMap<Integer, Map<Integer, List<Peer>>> trackMap = new ConcurrentHashMap<Integer, Map<Integer, List<Peer>>>();

        Random rand = new Random();

        //initialize peers
        for (int i = 0; i < PEER_NUM; i++) {
            int peerID = i;
            Location loc = generateLocation();
            Bandwidth bw = generateBandwidth();
            int capacity = rand.nextInt((MAX_STORAGECAPACITY - MIN_STORAGECAPACITY) + 1) + MIN_STORAGECAPACITY;

            Peer peer = new Peer(peerID,loc,bw,capacity);
            peerMap.put(peerID,peer);
           System.out.println("[peer] ID:"+i+",loc:"+loc.toString()+"bandwidth:"+bw.toString()+",capacity:"+capacity);
        }

        System.err.println("[Main] "+PEER_NUM+" peers are initialized");

        //initialize files and allocate each to several peers, and initialize tracker
        for (int i = 0; i < FILE_NUM; i++) {
            int filesize = rand.nextInt((MAX_FILESIZE - MIN_FILESIZE ) + 1) + MIN_FILESIZE;
            int fileID = i;
            TargetFile file = new TargetFile(fileID,10*4,MAX_CHUNKSIZE);
            fileMap.put(fileID,file);

            // select peers and allocate, store info in tracker
            List<Peer> peerList = selectPeers();

            for(Peer peer:peerList){
                peer.addFileToPeer(file);
            }

            Map<Integer,List<Peer>> chunkMap = new HashMap<Integer,List<Peer>>();
            for (int j = 0; j < file.getChunkNum(); j++) {
            	List<Peer> peerList2 = new ArrayList<>();
            	for(Peer p:peerList) {
            		peerList2.add(p);
            	}
                chunkMap.put(j,peerList2);
            }
            trackMap.put(fileID,chunkMap);
        }
        
//        /*===================== for debug ======================*/
//        System.err.println("Trackmap is: ");
//        for (Map.Entry<Integer, Map<Integer, List<Peer>>> trackentry : trackMap.entrySet())
//        {
//            System.err.println("fileID:"+trackentry.getKey() + ":" );
//            Map<Integer, List<Peer>> map = trackentry.getValue();
//
//            for(Map.Entry<Integer, List<Peer>> entry:map.entrySet()){
//                System.err.print("    chunkID:"+entry.getKey());
//                List<Peer> list = entry.getValue();
//                System.err.print(" peers:");
//                for(Peer p:list){
//                    System.err.print(p.getPeer_id()+", ");
//                }
//                System.err.println("");
//            }
//        }
//        /*===================== for debug ======================*/
        
        
        // initialize torrent files, one per peer
        for (int i = 0; i < PEER_NUM; i++) {
            generateTorrentForPeer(peerMap.get(i));
        }
       

        // initialize tracker
        Tracker tracker = new Tracker(trackMap);

        // allocate tracker for each peer
        for (int i = 0; i < PEER_NUM; i++) {
            Peer peer = peerMap.get(i);
            peer.setTracker(tracker);
        }
    }

    // start the simulation
    private void start(){
        for (int i = 0; i < PEER_NUM; i++) {
            Peer peer = peerMap.get(i);
            peer.getThread().start();
        }

        for (int i = 0; i < PEER_NUM; i++) {
            Peer peer = peerMap.get(i);
            try {
				peer.getThread().join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }

        System.err.println("Simulation has been finished!");

    }

    /* helper functions for initialization */
    private Location generateLocation(){
        double longitude = Math.random() * 180;
        double latitude = Math.random() * 180;

        return new Location(longitude,latitude);
    }

    private Bandwidth generateBandwidth(){
        Random rand = new Random();
        double uploadBw = rand.nextInt((MAX_BANDWIDTH - MIN_BANDWIDTH) + 1) + MIN_BANDWIDTH;
        double downloadBw = rand.nextInt((MAX_BANDWIDTH - MIN_BANDWIDTH) + 1) + MIN_BANDWIDTH;;

        return new Bandwidth(uploadBw,downloadBw);
    }

    private List<Peer> selectPeers(){
        Random rand = new Random();
        int selectedCnt = Math.min(rand.nextInt(PEER_NUM)+1,PEER_NUM);

        Set<Integer> peerIDSet = new HashSet<Integer>();
        for (int i = 0; i < selectedCnt; i++) {
            int id = rand.nextInt(PEER_NUM);
            while(peerIDSet.contains(id)){
                id = rand.nextInt(PEER_NUM);
            }
            peerIDSet.add(id);
        }

        List<Peer> peerList = new ArrayList<Peer>();
        for(int id:peerIDSet){
            peerList.add(peerMap.get(id));
        }

        return peerList;
    }

    private List<TargetFile> selectFiles(){
        Random rand = new Random();
        int selectedCnt = Math.min(rand.nextInt(FILE_NUM)+1,FILE_NUM/2);

        Set<Integer> fileIDSet = new HashSet<Integer>();
        for (int i = 0; i < selectedCnt; i++) {
            int id = rand.nextInt(FILE_NUM);
            while(fileIDSet.contains(id)){
                id = rand.nextInt(FILE_NUM);
            }
            fileIDSet.add(id);
        }

        List<TargetFile> fileList = new ArrayList<TargetFile>();
        for(int id:fileIDSet){
            fileList.add(fileMap.get(id));
        }

        return fileList;
    }

    private void generateTorrentForPeer(Peer peer){
        List<TargetFile> fileList = selectFiles();
        Torrent torrent = new Torrent(fileList);
        peer.setTorrent(torrent);
    }

    public static void main(String[] args) {
        Simulation simulation = new Simulation();
        simulation.init();
        simulation.start();
    }
    

}
