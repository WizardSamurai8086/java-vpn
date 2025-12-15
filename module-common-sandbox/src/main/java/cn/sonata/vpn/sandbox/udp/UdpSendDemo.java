package cn.sonata.vpn.sandbox.udp;
import cn.sonata.vpn.common.transport.udp.*;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class UdpSendDemo {
    public static void main(String[] args) throws Exception {
        //创建udp发送端和发送数据
        String str = "hello, udp";
        ByteBuffer data = ByteBuffer.wrap(str.getBytes());
        JdkUdpTransprot transprot = new JdkUdpTransprot();
        InetSocketAddress localAddress = new InetSocketAddress("127.0.0.1", 10086);
        InetSocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 30011);
        try {
            transprot.bind(localAddress);
        }catch (Exception e){
            e.printStackTrace();
        }
        try {
            transprot.send(data, remoteAddress);
        }catch (Exception e){
            e.printStackTrace();
        }


        transprot.close();




    }
}
