package cn.sonata.vpn.client.app;

import cn.sonata.vpn.client.core.ClientSession;
import cn.sonata.vpn.client.io.ClientSessionListenerImpl;
import cn.sonata.vpn.common.packet.Packet;
import cn.sonata.vpn.common.packet.PacketCodec;
import cn.sonata.vpn.common.packet.PacketHeader;
import cn.sonata.vpn.common.packet.PacketType;
import cn.sonata.vpn.common.protocol.ProtocolFSM;
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

        while (true) {
            ClientSession.StepResult step = clientSession.driveOnce();
            switch (step) {
                case NOOP:

                    clientSession.awaitReady(Duration.ofNanos(100));
                    break;
                case PROGRESSED:
                    // 注意：这里未必真的 ready，只是推进了一步。
                    // 先保留你的逻辑，但至少保证发包长度正确、异常可见。
                    packetSendSimulator(clientSession.getSession().getConnection());
                    clientSession.markReady();
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
            // 不要吞掉并返回 null，否则后面很难定位
            throw new RuntimeException("[Client] Failed to create session", e);
        }
    }

    private static void packetSendSimulator(TcpConnection connection) {
        try {
            for (int i = 1; i <= 5; i++) {
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
            // 打印堆栈，便于看到真正的报错点
            e.printStackTrace();
        }
    }
}
