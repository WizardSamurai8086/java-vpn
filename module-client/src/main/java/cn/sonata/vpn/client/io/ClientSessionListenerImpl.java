package cn.sonata.vpn.client.io;

import cn.sonata.vpn.common.packet.Packet;
import cn.sonata.vpn.common.session.SessionCloseReason;
import cn.sonata.vpn.common.session.SessionListener;

import java.util.List;


/**
 * SessionListenerImpl
 * <p>
 * 教学 / 演示用的 SessionListener 实现。
 * <p>
 * 设计说明：
 * - 该类作为 Session -> 应用层 的出口
 * - 当前实现为同步回调，仅用于演示数据流
 * <p>
 * 注意：
 * - 严格模型中，listener 仅用于 signal / wakeup
 * - Packet 的消费与协议推进应由 ServerSession.driveOnce 控制
 * - 此处为阶段性实现（DDL trade-off）
 */
public class ClientSessionListenerImpl implements SessionListener {


    private final AppIO appIO = new StringAppIO();


    private ClientSessionListenerImpl() {

    }

    /**
     * 伪单例，实际上是工厂方法 DDL trade-off
     * @return
     */
    public static ClientSessionListenerImpl getInstance() {
        return new ClientSessionListenerImpl();
    }


    @Override
    public void exposeReceived(List<Packet> packets) {

        if (packets == null || packets.isEmpty()) {
            return;
        }


        for (Packet packet : packets) {
            System.out.println("[server][listener] received packet: " + packet);
            appIO.onPacket(packet);
        }
    }

    @Override
    public void onSessionClosed(SessionCloseReason reason) {
        System.out.println("[client][listener] session closed: " + reason);
    }
}
