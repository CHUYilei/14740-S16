package util;

import model.Message;
import model.Peer;

import java.util.HashMap;
import java.util.Map;

/**
 * This class simulates the communication between hosts
 * we do not use socket here, rather we let this class control the communication
 *
 * @Author:
 * Xiaocheng OU
 * Yilei CHU
 */
public class CommunicationUtil {

    public static Message sendMessage(Peer src, Peer dst, Message msg){
        return dst.receiveMsg(src, msg);
    }

}
