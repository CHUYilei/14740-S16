package services;

import java.io.IOException;

import datatypes.Datagram;
import datatypes.Segment;

/**
 * 
 * @author Xiaocheng OU
 * @author Yilei CHU
 * @reference: https://github.com/venkatesh5789/TTP/blob/master/src/services/TTPConnEndPoint.java
 * @reference: https://github.com/wentianqi7/ReliableTransportOverUDP
 *
 * Calculate checksum from the whole datagram header
 */
public class ReceiverChecksum {
	public boolean checksum(Datagram datagram) throws IOException {
		int sum =0;
		int highbyte;
		int lowbyte;
				
		Segment segment = (Segment)datagram.getData();
		byte[] data = segment.getData();
		
		short checksum = Util.checksum(data);
		if (checksum == datagram.getChecksum()) {
			return true;
		}
		
		return false;
	}
}