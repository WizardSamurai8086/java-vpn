package cn.sonata.vpn.sandbox.tcp;

import cn.sonata.vpn.common.transport.tcp.JdkTcpConnection;
import cn.sonata.vpn.common.transport.tcp.TcpConnection;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * TCP Client Demo - 连接服务器并发送字符串
 */
public class ClientDemo {

    public static void main(String[] args) {
        try {
            // 1. 创建 Socket 并连接到服务器
            Socket socket = new Socket();
            InetSocketAddress serverAddress = new InetSocketAddress("localhost", 8080);
            System.out.println("[Client] Connecting to " + serverAddress + "...");
            socket.connect(serverAddress);
            System.out.println("[Client] Connected to server");

            // 2. 创建 TcpConnection
            TcpConnection conn = new JdkTcpConnection(socket);

            // 3. 发送字符串
            String message = "Hello from TCP Client!";
            ByteBuffer buffer = ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8));

            conn.sendAsync(buffer)
                    .thenAccept(len -> {
                        System.out.println("[Client] Sent " + len + " bytes: " + message);
                    })
                    .exceptionally(e -> {
                        System.err.println("[Client] Send failed: " + e.getMessage());
                        return null;
                    })
                    .join(); // 等待异步操作完成

            // 4. 等待一下确保数据发送完毕，然后关闭
            Thread.sleep(500);
            conn.closeAsync();
            System.out.println("[Client] Connection closed");

        } catch (Exception e) {
            System.err.println("[Client] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}