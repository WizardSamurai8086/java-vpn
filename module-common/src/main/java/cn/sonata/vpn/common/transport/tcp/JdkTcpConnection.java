package cn.sonata.vpn.common.transport.tcp;

import cn.sonata.vpn.common.transport.TransportException;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;




public class JdkTcpConnection implements TcpConnection {

    private static final ExecutorService IO_EXECUTOR = Executors.newCachedThreadPool();
    //创建本地socket
    private final Object receiveLock = new Object();    //加锁
    private final Object sendLock = new Object();       //类字段
    private final Socket socket;
    public JdkTcpConnection(Socket socket) {
        this.socket = socket;
    }


    @Override
    public SocketAddress getLocalAddress() {
        return socket.getLocalSocketAddress();
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return  socket.getRemoteSocketAddress();
    }

    @Override
    public boolean isConnected() {
        return socket.isConnected() && !socket.isClosed();
    }

    @Override
    public boolean isClosed() {
        return socket.isClosed();
    }

    @Override
    public CompletableFuture<Integer> sendAsync(ByteBuffer data) throws TransportException {

        return CompletableFuture.supplyAsync(() -> {
            try {

                OutputStream out = socket.getOutputStream();    //获取socket输出流
                int len = data.remaining();                     //获取待发送数据长度
                byte[] tmp = new byte[len];                     //创建暂用字节数组

                data.get(tmp, 0, len);

                synchronized (sendLock) {       //避免写入顺序被破坏（interleaving）
                    out.write(tmp);
                    out.flush();
                }
                return len;


            } catch (IOException e) {

                throw new CompletionException(
                        new TransportException("tcp send exception", e)
                );

        }

    }, IO_EXECUTOR);
    }

    @Override
    public CompletableFuture<Integer> receiveAsync(ByteBuffer buffer) throws TransportException {

        return CompletableFuture.supplyAsync(() -> {
            try {
                InputStream in = socket.getInputStream();
                int max = buffer.remaining();
                byte[] tmp = new byte[max];

                synchronized (receiveLock) {        //防止字节所有权被破坏（stealing）
                    int  len = in.read(tmp);
                    if (len == -1) {
                        return -1; //对端关闭
                    }

                    buffer.put(tmp, 0, len);
                    return len;
                }



            } catch (IOException e) {

                throw new CompletionException(
                        new TransportException("tcp receive exception", e)
                );

            }


        }, IO_EXECUTOR);
    }

    @Override
    public void shutdownAsync() throws TransportException {
        try {
            socket.shutdownOutput();
        } catch (IOException e) {
            throw new TransportException("tcp shutdown exception",e);
        }
    }

    @Override
    public void closeAsync() throws TransportException {
        try {
            socket.close();
        }catch (IOException e) {
            throw new TransportException("tcp close exception",e);
        }
    }
}
