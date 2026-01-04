package cn.sonata.vpn.common.session;

import cn.sonata.vpn.common.packet.Packet;

import java.util.List;

public interface SessionListener {
    /**
     * 推荐用Executor实现
     * 内部消化packets
     */
    public void exposeReceived(List<Packet> packets);

    void onSessionClosed(SessionCloseReason reason);

}
