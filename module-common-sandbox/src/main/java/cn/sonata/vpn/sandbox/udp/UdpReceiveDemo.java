package cn.sonata.vpn.sandbox.udp;

import cn.sonata.vpn.common.transport.udp.JdkUdpTransprot;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class UdpReceiveDemo {
    public static void main(String[] args) throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        JdkUdpTransprot transprot = new JdkUdpTransprot();
        InetSocketAddress receivePort = new InetSocketAddress("127.0.0.1", 30011);
        transprot.bind(receivePort);
        transprot.receive(buffer);


        // 2. 创建一个字节数组，大小为缓冲区中剩余可读的数据量
        byte[] receivedBytes = new byte[buffer.remaining()];

        // 3. 将缓冲区中的数据读取到字节数组中
        buffer.get(receivedBytes);

        String receivedString = new String(receivedBytes, "UTF-8");

        System.out.println(receivedString);

    }
}
