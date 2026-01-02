package cn.sonata.vpn.common.session;

import cn.sonata.vpn.common.packet.*;
import cn.sonata.vpn.common.protocol.*;
import cn.sonata.vpn.common.transport.TransportException;
import cn.sonata.vpn.common.transport.tcp.*;
import java.nio.ByteBuffer;


public class DefaultSession implements Session {

    private final TcpConnection connection;
    private final ProtocolFSM fsm;
    private final SessionListener listener;

    private SessionState state = SessionState.INIT;

    /**
     * 公有静态工厂方法
     * 避免暴露内部接口
     */
    public static DefaultSession create(TcpConnection connection, ProtocolFSM fsm, SessionListener listener) {
        return new DefaultSession(connection, fsm, listener);
    }

    /**
     * 绑定对应Connection和protocol
     * private method
     * @param connection 绑定TcpConnection
     * @param fsm 绑定协议状态机
     */
    private DefaultSession(TcpConnection connection, ProtocolFSM fsm, SessionListener listener) {
        this.connection = connection;
        this.fsm = fsm;
        this.listener = listener;
    }


    /***getter***/
    public TcpConnection getConnection() {
        return connection;
    }

    public ProtocolFSM getFsm() {
        return fsm;
    }


    public SessionState getState() {
        return state;
    }
    /*===getter===*/

    /**
     * state指的是session
     * 启动session
     * 启动fsm
     */
    @Override
    public void start() {

        if (state != SessionState.INIT)
            return;
        state = SessionState.RUNNING;

        ProtocolEffect effect = fsm.onSessionStart();
        apply(effect);
    }

    /**
     * 用于监听端的start方法
     * Start session execution loop without triggering ProtocolFSM.onSessionStart().
     * Intended for server/passive side demos where the first packet comes from remote.
     */
    public void startPassive() {
        if (state != SessionState.INIT) {
            return;
        }
        state = SessionState.RUNNING;
    }

    /**
     *
     */
    @Override
    public void onReadable() {
        if (state != SessionState.RUNNING)
            return;

        ByteBuffer buffer = ByteBuffer.allocate(4096);

        try{
            connection.receiveAsync(buffer).thenAccept(n -> {
                if(n == null || n < 0)
                {
                    close();
                    return;
                }

                buffer.flip();
                var packets = PacketCodec.decode(buffer);

                for (Packet packet : packets)
                {
                    ProtocolEffect effect = fsm.handlePacket(packet);
                    apply(effect);
                    if(state != SessionState.RUNNING)
                        return;

                }
            });



        }catch (TransportException e)
        {
            close();
        }


    }

    @Override
    public void close() {
        if(state == SessionState.CLOSED)
            return;

        state = SessionState.CLOSED;
        try{
            connection.closeAsync();

        }catch (TransportException Ignore)
        {

        }

        if (listener != null) {
            listener.onSessionClosed(SessionCloseReason.LOCAL_CLOSE);
        }

    }

    /**
     * 将fsm决策转化成IO操作
     * @param effect 接收从fsm产生的effect
     */
    private void apply(ProtocolEffect effect) {
        //卫语句确保session状态运行
        if (effect == null || state != SessionState.RUNNING)
        {
            return;
        }

        try{
            //控制tcp连接
            switch (effect.getAction())
            {
                case NONE -> {}
                case SEND -> {
                    for(Packet packet : effect.getOutputs())
                    {
                        ByteBuffer buffer = PacketCodec.encode(packet);
                        connection.sendAsync(buffer);
                    }
                }
                case CLOSE_GRACEFUL -> {
                    connection.shutdownAsync();
                    state = SessionState.CLOSED;
                    if (listener != null) {
                        listener.onSessionClosed(SessionCloseReason.NORMAL);
                    }
                }
                case CLOSE_ERROR -> {
                    connection.closeAsync();
                    state = SessionState.CLOSED;
                    if (listener != null) {
                        listener.onSessionClosed(SessionCloseReason.PROTOCOL_ERROR);
                    }
                }
            }
        } catch (TransportException e)
        {
            state = SessionState.CLOSED;
            try{
                connection.closeAsync();

            }catch (TransportException Ignore)
            {
            }
            if (listener != null) {
                listener.onSessionClosed(SessionCloseReason.IO_ERROR);
            }
        }
    }




}
