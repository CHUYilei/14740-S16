package model;


import java.security.acl.Owner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import simulate.Tracker;

/**
 * Created by Xiaocheng Ou on 16/4/19.

a.k.a node in P2P. 
Each peer will hold some resources, 
and will request resources from other peers according to torrent file 
and tracker’s instructions.
 */
public class Peer implements Runnable{
	public int peer_id;
    private Location location;
    private Bandwidth bandwidth;
    private Map<Integer, List<Integer>> ownedChunk = new HashMap<Integer, List<Integer>>();
    private ArrayList<TargetFile> ownedfile = new ArrayList<TargetFile>();
    private Torrent needfile;
    private int storeCapacity;
    private float totalDownloadamount=0;
    private float totalUploadamount=0;
    private static Tracker tracker;
    private int downloadCount=0;
    private int uploadCoun=0;
    private Thread thread;
    
    public Peer(int peer_id, Location location, Bandwidth bandwidth, int storeCapacity){
    	//initialize the paremeter
    	this.peer_id = peer_id;
    	this.location = location;
    	this.bandwidth = bandwidth;
    	this.storeCapacity = storeCapacity;
    	thread = new Thread(this);
    }
    
    public Thread getThread() {
        return thread;
    }

    public void setThread(Thread thread) {
        this.thread = thread;
    }
    
    
    public Torrent getTorrent() {
		return needfile;
	}



	public void setTorrent(Torrent needfile) {
		this.needfile = needfile;
	}



	public Map<Integer, List<Integer>> getOwnedChunk() {
		return ownedChunk;
	}
	
	public void setOwnedChunk(Map<Integer, List<Integer>> ownedChunk) {
		this.ownedChunk = ownedChunk;
	}
	
	public ArrayList<TargetFile> getOwnedfile() {
		return ownedfile;
	}
	
	public void setOwnedfile(ArrayList<TargetFile> ownedfile) {
		this.ownedfile = ownedfile;
	}
	
	public Tracker getTracker() {
		return tracker;
	}
	
	public void setTracker(Tracker tracker) {
		Peer.tracker = tracker;
	}
	
	public int getPeer_id() {
		return peer_id;
	}

	public void setPeer_id(int peer_id) {
		this.peer_id = peer_id;
	}

	
	
	public void addFileToPeer(TargetFile file){
		ownedfile.add(file);
		int chunnumber = file.getChunkNum();
		List<Integer> chunfile = new ArrayList<Integer>();
		for(int i=0;i<chunnumber;i++){
			chunfile.add(i);
			totalDownloadamount+=file.getChunkList().get(i);
		}
		ownedChunk.put(file.getFile_id(), chunfile);
	   //System.out.println("peerid:"+peer_id+"owenedchunksize:"+ownedChunk.size());
		
	}
    
	
	
	
    

	@Override
	public void run() {
		// TODO Auto-generated method stub
		//go to tracker to find which peer to get the chunk thread
		new Thread(new requestRunnable()).start();
		
	}
	
	
	public void request(){
		//go to tracker to find which peer to get the chunk
		List<TargetFile> needfilelist = needfile.getFileList();
		
		for(int i =0;i<needfilelist.size();i++){
			TargetFile targetFile = needfilelist.get(i);
			int file_id = targetFile.getFile_id();
			if(!ownedChunk.containsKey(file_id)) {
				Map<Integer, List<Peer>> allfilechunk = tracker.getFilePeers(file_id);
				
				List<Integer> fileichunklist = new ArrayList<Integer>();
				for(int j=0;j<  allfilechunk.size();j++){
					List<Peer> agreepeer = new ArrayList<Peer>();
					//file i chunk j peerlist
					List<Peer> peerlist = allfilechunk.get(j);
					//handshake
					
					//System.out.println("peerid:"+peer_id+"request file from peer:"+peerlist.get(0).peer_id+"for fileid:"+file_id+"and chunkid:"+j);
					Message handshake= new Message(Message.HANDSHAKE);
						for(int k=0;k<peerlist.size();k++){
							Peer peerdsc =  peerlist.get(k);
							//System.out.println("peerid:"+peer_id+"request file from peer:"+peerdsc.peer_id);
							Message handshakereply = util.CommunicationUtil.sendMessage(this, peerdsc, handshake);
							//get the response opcode from peer
							if(handshakereply.getOpcode()== Message.AGREE){
								agreepeer.add(peerdsc);
							}
						}
						//choose which peer to request this chunk according to the distance and bandwidth
						Double score=Double.MAX_VALUE;
						Peer minpeer =null;
						for(int m =0; m<agreepeer.size();m++){
							double distance = location.getDistance(location, agreepeer.get(m).location);
							List<Integer> chunkListt = targetFile.getChunkList();
							double uploadspeedforpeer = agreepeer.get(m).bandwidth.getUploadTime(chunkListt.get(j));
							double newscore = distance+1/uploadspeedforpeer*100;
							if(newscore<score){
								score = newscore;
								minpeer = agreepeer.get(m);
							}
				
						}
						System.err.println("Peer_id:"+" ["+peer_id+"] "+" request file from peer:"+" ["+minpeer.peer_id+"] "+" for file_id: "+file_id+" and chunkid: "+j);
						// get the chunk from the min peer
						Message requestchunk = new Message(Message.GET_CHUNKS);
						requestchunk.setFileID(file_id);
						requestchunk.setChunkID(j);
						List<Integer> chunkList = targetFile.getChunkList();
						requestchunk.setChunkSize(chunkList.get(j));
						Message getchunk = util.CommunicationUtil.sendMessage(this,minpeer, requestchunk);
  
						if(storeCapacity>totalDownloadamount+getchunk.getChunkSize()){
							if(getchunk.getOpcode()== Message.GIVE_CHUNKS){
								int newgetfileid= getchunk.getFileID();
								int newgetchunkid = getchunk.getChunkID();
								//update amount
								downloadCount++;
								totalDownloadamount+= getchunk.getChunkSize();
						
								//update ownedchunk
								fileichunklist.add(j);
								ownedChunk.put(file_id, fileichunklist);
						
								//update the tracker
								tracker.addChunkForPeer(newgetfileid, newgetchunkid, this);
								System.err.println("Peer_id:"+" ["+peer_id+"] "+" receive file from peer:"+" ["+minpeer.peer_id+"] "+" for file_id: "+file_id+" and chunkid: "+j);
							}
						}else {
							System.out.println("Storge full for peer id:"+peer_id);
						}
				
				
				}
				//update ownedcompletefile
				if(ownedChunk.size() == allfilechunk.size()){
					ownedfile.add(targetFile);
				}
		     }
			
		}
		
		
	}
	
	public  class requestRunnable implements Runnable {

	        public void run() {
	            request();
	        }
	}
		
	
	
	//receive thread
	public Message receiveMsg(Peer peerSrc, Message msg){
		int opcode = msg.getOpcode();
		switch(opcode){
			case 1:
				
				int peerdownloadCount = peerSrc.downloadCount;
				
				int peeruploadCount = peerSrc.uploadCoun;
				if(peerdownloadCount !=0){
					//System.out.println("ratio:"+peeruploadCount/ peerdownloadCount);
				}
				
				
				Message ret;
				if(peerdownloadCount>100 && peeruploadCount/peerdownloadCount<1){
					System.out.println("Peer:"+peerSrc.getPeer_id()+"'s request is refused ");					ret = new Message(Message.REFUSE);
				}else{
					 ret = new Message(Message.AGREE);
				}
				
				
				return ret;
				
				
			case 2:
				Message ret1 = new Message(Message.GIVE_CHUNKS);
				int requirefile = msg.getFileID();
				
				int requirechunk = msg.getChunkID();
			    
				
				if(!ownedChunk.get(requirefile).isEmpty()){
					List<Integer> chunklistowened = ownedChunk.get(requirefile);
					//System.err.println("in peerid:"+peer_id+"peerid:"+peerSrc.peer_id+"requirefileid:"+requirefile+"requirechunk:"+requirechunk+"and my chunksize is:"+chunklistowened.size());
				
					
					if(chunklistowened.contains(requirechunk)){
						ret1.setChunkID(requirechunk);
						
						ret1.setChunkSize(msg.getChunkSize());
						ret1.setFileID(requirefile);
						totalUploadamount+= ret1.getChunkSize();
						uploadCoun++;
						return ret1;
					}

				}
				
		}
		return null;
		
	}
		
	
}
