package cn.sonata.vpn.common.transport.udp;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * UDP 传输抽象接口
 * 职责：收发 UDP 数据包
 */
public interface UdpTransport extends AutoCloseable {

    /**
     * 绑定本地地址
     * @param localAddress 本地地址（null 表示自动分配）
     */
    void bind(InetSocketAddress localAddress) throws Exception;

    /**
     * 发送数据包
     * @param data 数据内容
     * @param target 目标地址
     * @return 实际发送的字节数
     */
    int send(ByteBuffer data, InetSocketAddress target) throws Exception;

    /**
     * 接收数据包（阻塞）
     * @param buffer 接收缓冲区
     * @return 发送方地址，null 表示超时或关闭
     */
    InetSocketAddress receive(ByteBuffer buffer) throws Exception;

    /**
     * 获取本地绑定地址
     */
    InetSocketAddress getLocalAddress();

    /**
     * 关闭传输
     */
    @Override
    void close() throws Exception;
}