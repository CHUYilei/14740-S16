package simulate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import model.Peer;

/**
the tracker server of P2P, 
contains the latest chunk allocation info.
 */
public class Tracker {
    private ConcurrentHashMap<Integer, Map<Integer, List<Peer>>> trackMap;

    public Tracker(ConcurrentHashMap<Integer, Map<Integer, List<Peer>>> trackMap){
        this.trackMap = trackMap;
    }

    public Map<Integer, List<Peer>> getFilePeers(int file_id){
        return trackMap.get(file_id);
    }

    public void addChunkForPeer(int file_id, int chunk_id, Peer peer){
    	Map<Integer, List<Peer>> idmap = trackMap.get(file_id);
    	List<Peer> chunklist = idmap.get(chunk_id);
    	chunklist.add(peer);     
    }

    public void removeChunkForPeer(int file_id, int chunk_id, Peer peer){
    	Map<Integer, List<Peer>> idmap = trackMap.get(file_id);
    	List<Peer> chunklist = idmap.get(chunk_id);
    	chunklist.remove(peer);
    }
    
    public void removePeer(Peer peer){
    	
    }

    public Map<Integer, Map<Integer, List<Peer>>> gettrackermap(){
        return trackMap;
    }

    public void setTrackmap(ConcurrentHashMap<Integer, Map<Integer, List<Peer>>> trackMap){
        this.trackMap = trackMap;
    }


}


    
