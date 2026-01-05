package cn.sonata.vpn.client.app;

import cn.sonata.vpn.client.core.ClientSession;
import cn.sonata.vpn.client.io.ClientSessionListenerImpl;
import cn.sonata.vpn.common.packet.Packet;
import cn.sonata.vpn.common.packet.PacketCodec;
import cn.sonata.vpn.common.packet.PacketHeader;
import cn.sonata.vpn.common.packet.PacketType;
import cn.sonata.vpn.common.protocol.ProtocolFSM;
import cn.sonata.vpn.common.protocol.ProtocolState;
import cn.sonata.vpn.common.session.DefaultSession;
import cn.sonata.vpn.common.transport.tcp.JdkTcpConnection;
import cn.sonata.vpn.common.transport.tcp.TcpConnection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

public class Main {
    public static void main(String[] args) throws InterruptedException, TimeoutException {
        ClientSession clientSession = ClientSession.create(createSession());
        clientSession.start();

        boolean dataSent = false;

        while (true) {
            ClientSession.StepResult step = clientSession.driveOnce();

            // 只有在握手完成（FSM 进入 READY）后才允许发送 DATA，否则 server 端会在 INIT/NEGOTIATING 收到 DATA
            // 并按当前 FSM 规则直接 closeError。
            if (!dataSent && clientSession.getSession().getFsm().getState() == ProtocolState.READY) {
                packetSendSimulator(clientSession.getSession().getConnection());    //模拟发包
                dataSent = true;
                clientSession.markReady();
            }

            switch (step) {
                case NOOP:
                    clientSession.awaitReady(Duration.ofNanos(100));
                    break;
                case PROGRESSED:
                    // PROGRESSED 仅表示触发了一次 onReadable() 调度，不代表握手已完成
                    break;
                case CLOSED:
                    System.out.println("ClientSession has been closed");
                    return;
                case ERROR:
                    System.out.println("ClientSession error");
                    return;
                default:
                    throw new IllegalStateException("Unexpected value: " + step);
            }
        }
    }

    private static DefaultSession createSession() {
        try {
            InetSocketAddress serverAddress = new InetSocketAddress("127.0.0.1", 9000);

            Socket socket = new Socket();
            socket.connect(serverAddress);

            TcpConnection conn = new JdkTcpConnection(socket);
            return DefaultSession.create(conn, ProtocolFSM.create(), ClientSessionListenerImpl.getInstance());
        } catch (IOException e) {
            throw new RuntimeException("[Client] Failed to create session", e);
        }
    }

    private static void packetSendSimulator(TcpConnection connection) {
        try {
            for (int i = 1; i <= 6; i++) {
                if (i == 6)
                {
                    connection.sendAsync(PacketCodec.encode(Packet.close()));
                    return;
                }
                byte[] bodyBytes = ("Hello-" + i).getBytes(StandardCharsets.UTF_8);
                ByteBuffer body = ByteBuffer.wrap(bodyBytes);

                PacketHeader dataHeader = new PacketHeader(
                        0x56504E44,
                        (short) 0x00,
                        PacketType.DATA,
                        body.remaining()
                );

                Packet packet = new Packet(dataHeader, body);

                connection.sendAsync(PacketCodec.encode(packet));
                Thread.sleep(2000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
