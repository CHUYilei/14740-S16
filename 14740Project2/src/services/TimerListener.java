package services;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @Author:
 * Xiaocheng OU
 * Yilei CHU
 * @reference: https://github.com/venkatesh5789/TTP/blob/master/src/services/TTPConnEndPoint.java
 * @reference: https://github.com/wentianqi7/ReliableTransportOverUDP
 * This action listener would wait for time out and retransmit all
 */
public class TimerListener implements ActionListener{
    private ServerSender serverSender;

    public TimerListener(ServerSender serverSender){
        this.serverSender = serverSender;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("Time out! Start retransmission");
        this.serverSender.retransmitAllSentNotAcked();
        this.serverSender.restartTimer();
    }
}
