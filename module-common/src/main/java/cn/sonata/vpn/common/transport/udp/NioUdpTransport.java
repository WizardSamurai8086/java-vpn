package cn.sonata.vpn.common.transport.udp;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class NioUdpTransport implements UdpTransport{

    private DatagramChannel channel;
    private volatile boolean closed = false;    //udp connection's states


    public NioUdpTransport() throws Exception  {
            this.channel = DatagramChannel.open();
            // 阻塞模式
            this.channel.configureBlocking(true);


    }

    @Override
    public void bind(InetSocketAddress localAddress) throws Exception {
        if(localAddress == null) {

            localAddress = new InetSocketAddress(0);
        }

        channel.bind(localAddress);


    }

    @Override
    public int send(ByteBuffer data, InetSocketAddress target) throws Exception {
        checkClosed();

        return channel.send(data, target);
    }

    @Override
    public InetSocketAddress receive(ByteBuffer buffer) throws Exception {
        checkClosed();
        buffer.clear();
        return (InetSocketAddress) channel.receive(buffer);

    }

    @Override
    public InetSocketAddress getLocalAddress() {
        try{
            return  (InetSocketAddress) channel.getLocalAddress();
        } catch(Exception e){
            return null;
        }
    }

    @Override
    public void close() throws Exception {
        if(!closed) {
            closed = true;
            channel.close();
        }
    }

    private void checkClosed() throws Exception {
        if(closed) {
            throw new Exception("The transport 已关闭");
        }
    }
}
