package cn.sonata.vpn.common.transport.tcp;

import cn.sonata.vpn.common.transport.TransportException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public interface TcpConnection {


    /**
     * 获取本地绑定地址
     * @return 本地地址，未绑定则返回 null
     */
    SocketAddress getLocalAddress();

    /**
     * 获取外部地址
     * @return  外部地址， 未绑定则返回null
     */
    SocketAddress getRemoteAddress();

    /**
     * 判断链接是否建立
     */
    boolean isConnected();

    /**
     * 判断链接是否关闭
     */
    boolean isClosed();

    /**
     * 发送数据
     *  TCP 是字节流，此方法会尽力发送所有数据
     * @param data  待传数据
     * @return  CompletableFuture<Integer> 实际发送大小
     * @throws TransportException   传输层失败了
     */
    CompletableFuture<Integer> sendAsync(ByteBuffer data) throws TransportException;

    /**
     * 异步接收数据
     * 读取最多 buffer.remaining() 字节
     * @param buffer 缓冲区
     * @return CompletableFuture<Integer> 实际接收大小，-1 流结束
     * @throws TransportException   传输层失败了
     */
    CompletableFuture<Integer> receiveAsync(ByteBuffer buffer) throws TransportException;


    /**
     * 优雅关闭信道
     * 等未发送数据发送完成
     * @throws TransportException   传输层失败了
     */
    void shutdownAsync() throws TransportException;

    /**
     * 立刻关闭信道
     * 丢弃未传输数据
     * @throws TransportException   传输层失败了
     */
    void closeAsync() throws TransportException;



}