package cn.sonata.vpn.common.transport.tcp;


import cn.sonata.vpn.common.transport.TransportException;

import java.net.SocketAddress;

public interface TcpServer extends AutoCloseable{

    /**
     * 完成Server的地址绑定
     * @param endpoint 待绑定IP
     * @throws TransportException 自定义异常
     */
    void bind(SocketAddress endpoint) throws TransportException;

    /**
     * 建立链接
     * @return  返回建立成功的链接
     * @throws TransportException   自定义异常
     */
    TcpConnection accept() throws TransportException;


}
