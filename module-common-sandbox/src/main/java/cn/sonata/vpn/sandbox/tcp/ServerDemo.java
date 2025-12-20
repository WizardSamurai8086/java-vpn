package cn.sonata.vpn.sandbox.tcp;

import cn.sonata.vpn.common.transport.TransportException;
import cn.sonata.vpn.common.transport.tcp.JdkTcpServer;
import cn.sonata.vpn.common.transport.tcp.TcpConnection;
import cn.sonata.vpn.common.transport.tcp.TcpServer;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * TCP Server Demo - 接收客户端发送的字符串并打印
 */
public class ServerDemo {

    public static void main(String[] args) {
        TcpServer server = new JdkTcpServer();

        try {
            // 1. 绑定到本地 8080 端口
            InetSocketAddress address = new InetSocketAddress("localhost", 8080);
            server.bind(address);
            System.out.println("[Server] Listening on " + address);

            // 2. 阻塞等待客户端连接
            System.out.println("[Server] Waiting for client connection...");
            TcpConnection conn = server.accept();
            System.out.println("[Server] Client connected: " + conn.getRemoteAddress());

            // 3. 接收数据
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            conn.receiveAsync(buffer)
                    .thenAccept(len -> {
                        if (len > 0) {
                            buffer.flip();
                            byte[] data = new byte[buffer.remaining()];
                            buffer.get(data);
                            String message = new String(data, StandardCharsets.UTF_8);
                            System.out.println("[Server] Received: " + message);
                        } else {
                            System.out.println("[Server] Connection closed by client");
                        }
                    })
                    .exceptionally(e -> {
                        System.err.println("[Server] Receive failed: " + e.getMessage());
                        return null;
                    })
                    .join(); // 等待异步操作完成

            // 4. 关闭连接和服务器
            Thread.sleep(1000); // 等待一下，确保客户端收到数据
            conn.closeAsync();
            server.close();
            System.out.println("[Server] Server closed");

        } catch (TransportException e) {
            System.err.println("[Server] Transport error: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}