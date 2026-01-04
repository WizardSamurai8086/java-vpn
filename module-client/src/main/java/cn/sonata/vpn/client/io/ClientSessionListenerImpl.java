package cn.sonata.vpn.client.io;

import cn.sonata.vpn.common.packet.Packet;
import cn.sonata.vpn.common.session.SessionCloseReason;
import cn.sonata.vpn.common.session.SessionListener;

import java.util.List;

public class ClientSessionListenerImpl implements SessionListener {
    @Override
    public void exposeReceived(List<Packet> packets) {

    }

    @Override
    public void onSessionClosed(SessionCloseReason reason) {

    }
}
