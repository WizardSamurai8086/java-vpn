package cn.sonata.vpn.common.transport.tcp;

import cn.sonata.vpn.common.transport.TransportException;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

public class JdkTcpServer implements TcpServer {

    private ServerSocket server;


    @Override
    public void bind(SocketAddress address) throws TransportException {

        try {

            this.server = new ServerSocket();
            server.bind(address);

        } catch ( IOException e) {
            throw new TransportException("Tcp server bind exception" + address, e);
        }
    }

    /**
     *     accept() 返回的是一个已经建立的连接抽象，
     *     Socket 是 JDK 的实现细节，被封装在 TcpConnection 内部，
     *     这样可以避免抽象泄漏，并保证传输语义的一致性
     * @return  TcpConnection
     * @throws TransportException   自定义TransportException
     */
    @Override
    public TcpConnection accept() throws TransportException {

        try {


            if (server == null) {
                throw new TransportException("server not bound exception");
            }

            Socket socket =  server.accept();
            return new JdkTcpConnection(socket);
        } catch (IOException e) {
            throw new TransportException("Tcp server accept exception " , e);
        }
    }

    /**
     * 关闭ServerSocket,不再接受链接
     * @throws TransportException   自定义异常
     */
    @Override
    public void close() throws TransportException {

    try {

        if (server == null) {
            throw new TransportException("server not bound exception");
        }

        server.close();
    }
    catch (IOException e) {
        throw new TransportException("Tcp server close exception", e);
    }

    }
}
