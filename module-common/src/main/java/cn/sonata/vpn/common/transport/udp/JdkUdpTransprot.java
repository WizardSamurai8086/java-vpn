package cn.sonata.vpn.common.transport.udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class JdkUdpTransprot implements UdpTransport {

    DatagramSocket socket;



    @Override
    public void bind(InetSocketAddress localAddress) throws Exception {
        if (socket != null) {
            throw new Exception("bind 已存在");
        }
        if (localAddress == null) {
            socket = new DatagramSocket();
        }
        else {
            socket = new DatagramSocket(localAddress);
        }
    }

    @Override
    public int send(ByteBuffer data, InetSocketAddress target) throws Exception {
        ensureOpen();

        int len = data.remaining();
        byte[] bytes = new byte[len];
        data.get(bytes);
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, target);
        socket.send(packet);
        return len;
    }

    @Override
    public InetSocketAddress receive(ByteBuffer buffer) throws Exception {
        ensureOpen();

        byte[] array = buffer.array();
        DatagramPacket packet = new DatagramPacket(array, array.length);

        socket.receive(packet);

        buffer.clear();

        buffer.put(packet.getData(), 0, packet.getLength());
        buffer.flip();


        return (InetSocketAddress) packet.getSocketAddress();
    }

    @Override
    public InetSocketAddress getLocalAddress() {

        if (socket == null) {
            return null;
        }
        return (InetSocketAddress) socket.getLocalSocketAddress();
    }

    @Override
    public void close() throws Exception {
        if (socket != null) {
            socket.close();
        }
    }

    void ensureOpen()  {
        if(socket == null || socket.isClosed() ) {
            throw new IllegalStateException("udp 链接失败");
        }

    }
}
