package cn.sonata.vpn.server.app;

import cn.sonata.vpn.common.protocol.ProtocolFSM;
import cn.sonata.vpn.common.session.DefaultSession;
import cn.sonata.vpn.common.transport.TransportException;
import cn.sonata.vpn.common.transport.tcp.JdkTcpConnection;
import cn.sonata.vpn.common.transport.tcp.JdkTcpServer;
import cn.sonata.vpn.common.transport.tcp.TcpConnection;
import cn.sonata.vpn.common.transport.tcp.TcpServer;
import cn.sonata.vpn.server.core.ServerSession;
import cn.sonata.vpn.server.io.ServerSessionListenerImpl;
import cn.sonata.vpn.server.proxy.ProxyService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
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

            /**
             * 绑定client
             */
            InetSocketAddress address = new InetSocketAddress("127.0.0.1", 9000);
            TcpServer tcpServer = new JdkTcpServer();
            tcpServer.bind(address);
            System.out.println("[server] listening on " + address + ", waiting for client...");

            TcpConnection conn = tcpServer.accept();
            System.out.println("[server] client connected: " + conn);

            /**
             * 绑定upstream
             */
            InetSocketAddress upstreamAddress = new InetSocketAddress("127.0.0.1", 9001);
            Socket upstreamSocket = new Socket();
            upstreamSocket.connect(upstreamAddress);
            TcpConnection upstreamConn = new JdkTcpConnection(upstreamSocket);
            return DefaultSession.create(
                    conn,
                    ProtocolFSM.create(),
                    ServerSessionListenerImpl.create(new ProxyService(upstreamConn, conn))     //Listener实现回调机制
            );
        } catch (TransportException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
