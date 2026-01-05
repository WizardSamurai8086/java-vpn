package cn.sonata.vpn.server.io;

import cn.sonata.vpn.common.packet.Packet;
import cn.sonata.vpn.common.session.SessionCloseReason;
import cn.sonata.vpn.common.session.SessionListener;
import cn.sonata.vpn.common.transport.TransportException;
import cn.sonata.vpn.server.proxy.ProxyService;

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
public class ServerSessionListenerImpl implements SessionListener {

    private final AppIO appIO = new StringAppIO();
    private final ProxyService proxyService;

    private ServerSessionListenerImpl(ProxyService proxyService) {
        this.proxyService = proxyService;
    }

    public static ServerSessionListenerImpl create(ProxyService proxyService) {
        return new ServerSessionListenerImpl(proxyService);
    }

    @Override
    public void exposeReceived(List<Packet> packets) {
        if (packets == null || packets.isEmpty()) return;

        for (Packet packet : packets) {
            System.out.println("[server][listener] received packet: " + packet);
            appIO.onPacket(packet);

            // 最小可跑：收到就转发到 upstream
            try {
                proxyService.send(appIO);
            } catch (TransportException e) {
                System.out.println("[server][listener] proxy send failed: " + e.getMessage());
            }
        }
    }

    @Override
    public void onSessionClosed(SessionCloseReason reason) {
        System.out.println("[server][listener] session closed: " + reason);
    }
}
