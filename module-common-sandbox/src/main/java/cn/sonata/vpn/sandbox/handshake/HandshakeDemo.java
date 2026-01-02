package cn.sonata.vpn.sandbox.handshake;

import cn.sonata.vpn.common.protocol.ProtocolFSM;
import cn.sonata.vpn.common.session.DefaultSession;
import cn.sonata.vpn.common.session.SessionState;
import cn.sonata.vpn.common.transport.TransportException;
import cn.sonata.vpn.common.transport.tcp.JdkTcpConnection;
import cn.sonata.vpn.common.transport.tcp.JdkTcpServer;
import cn.sonata.vpn.common.transport.tcp.TcpConnection;
import cn.sonata.vpn.common.transport.tcp.TcpServer;

import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Minimal handshake demo.
 *
 * Usage:
 *   args[0] = "server" or "client"
 *   args[1] = host (optional, default localhost)
 *   args[2] = port (optional, default 8081)
 */
public class HandshakeDemo {

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0].trim().toLowerCase() : "server";
        String host = args.length > 1 ? args[1].trim() : "localhost";
        int port = args.length > 2 ? Integer.parseInt(args[2].trim()) : 8081;

        if ("server".equals(mode)) {
            runServer(host, port);
            return;
        }
        if ("client".equals(mode)) {
            runClient(host, port);
            return;
        }

        System.err.println("Unknown mode: " + mode + ". Use server|client");
    }

    private static void runServer(String host, int port) throws TransportException {
        InetSocketAddress address = new InetSocketAddress(host, port);      //绑定serverIp
        TcpServer server = new JdkTcpServer();                              //创建tcpServer
        server.bind(address);                                               //绑定IP
        System.out.println("[server] listening on " + address);

        TcpConnection conn = server.accept();
        System.out.println("[server] accepted: remote=" + conn.getRemoteAddress());

        DefaultSession session = DefaultSession.create(conn, ProtocolFSM.create(), reason ->
                System.out.println("[server] session closed: " + reason));

        // server side: passive start; it waits for HELLO from client
        session.startPassive();
        driveSession(session, "server");

        try {
            server.close();
        } catch (Exception e) {
            throw new TransportException("server close failed", e);
        }
    }

    private static void runClient(String host, int port) throws Exception {
        InetSocketAddress address = new InetSocketAddress(host, port);  // 绑定remoteIp
        Socket socket = new Socket();                                   // 创建套接字
        socket.connect(address, 3000);                           // 套接字绑定连接

        TcpConnection conn = new JdkTcpConnection(socket);              //  绑定Connection
        System.out.println("[client] connected: remote=" + conn.getRemoteAddress());    //绑定clientIp

        DefaultSession session = DefaultSession.create(conn, ProtocolFSM.create(), reason ->
                System.out.println("[client] session closed: " + reason));  //连接，fsm, impl方法

        // client side: start handshake by sending HELLO
        session.start();        //发一个hello包进行握手

        driveSession(session, "client");
    }

    /**
     * Simplest possible driver: repeatedly call onReadable() and sleep a bit.
     *
     * This keeps the demo small and avoids introducing a Reactor/Selector model.
     */
    private static void driveSession(DefaultSession session, String role) {
        int idleRounds = 0;     //fsm阻塞轮数

        var lastFsmState = session.getFsm().getState();     //读取fsm状态

        while (session.getState() == SessionState.RUNNING   //确保session正常
                && session.getConnection().isConnected()
                && !session.getConnection().isClosed()) {
            session.onReadable();

            // If FSM progressed, consider this as activity and reset idle counter.
            var nowFsmState = session.getFsm().getState();
            if (nowFsmState != lastFsmState) {
                idleRounds = 0;
                lastFsmState = nowFsmState;
            }

            // Small sleep to avoid busy-spin. In a real model you'd block on read readiness.
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            // Guard for the demo: don't hang forever.
            // If nothing happens for a while, close.
            if (++idleRounds > 400) { // ~20s
                System.out.println("[" + role + "] idle timeout, closing");
                session.close();
                break;
            }

            if (session.getFsm().isClosed()) {
                System.out.println("[" + role + "] fsm closed, closing");
                session.close();
                break;
            }
        }

        System.out.println("[" + role + "] driver finished: sessionState=" + session.getState() + ", fsmState=" + session.getFsm().getState());
    }
}
