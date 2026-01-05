package cn.sonata.vpn.server.app;

import cn.sonata.vpn.common.packet.Packet;
import cn.sonata.vpn.common.protocol.ProtocolFSM;
import cn.sonata.vpn.common.session.DefaultSession;
import cn.sonata.vpn.common.session.SessionCloseReason;
import cn.sonata.vpn.common.session.SessionListener;
import cn.sonata.vpn.common.transport.TransportException;
import cn.sonata.vpn.common.transport.tcp.JdkTcpServer;
import cn.sonata.vpn.common.transport.tcp.TcpConnection;
import cn.sonata.vpn.common.transport.tcp.TcpServer;
import cn.sonata.vpn.server.core.ServerSession;
import cn.sonata.vpn.server.io.ServerSessionListenerImpl;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;



public class Main {

    public static void main(String[] args)
            throws InterruptedException, TimeoutException {

        ServerSession serverSession = ServerSession.create(createSession());
        if (serverSession == null) return;

        serverSession.start();

        ServerSession.StepResult step = ServerSession.StepResult.NOOP;

        while (true) {
            step = serverSession.driveOnce();
            switch (step) {
                case NOOP:
                    serverSession.awaitReady(Duration.ofNanos(100));
                    break;
                case PROGRESSED:
                    //TODO:未来可以细化握手
                    serverSession.markReady();
                    break;
                case CLOSED:
                    System.out.println("ServerSession has been closed");
                    return;
                case ERROR:
                    System.out.println("ServerSession error");
                    return;
                default:
                    throw new IllegalStateException("Unexpected value: " + step);

            }

        }
    }

    private static DefaultSession createSession() {
        try {
            InetSocketAddress address = new InetSocketAddress("127.0.0.1", 9000);
            TcpServer tcpServer = new JdkTcpServer();
            tcpServer.bind(address);
            System.out.println("[server] listening on " + address + ", waiting for client...");

            TcpConnection conn = tcpServer.accept();
            System.out.println("[server] client connected: " + conn);

            return DefaultSession.create(
                    conn,
                    ProtocolFSM.create(),
                    ServerSessionListenerImpl.getInstance()     //Listener实现回调机制
            );
        } catch (TransportException e) {
            e.printStackTrace();
            return null;
        }
    }

}
