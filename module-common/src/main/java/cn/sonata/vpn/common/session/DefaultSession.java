package cn.sonata.vpn.common.session;

import cn.sonata.vpn.common.packet.*;
import cn.sonata.vpn.common.protocol.*;
import cn.sonata.vpn.common.transport.TransportException;
import cn.sonata.vpn.common.transport.tcp.*;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


public class DefaultSession implements Session {

    private final TcpConnection connection;
    private final ProtocolFSM fsm;
    private final SessionListener listener;

    private SessionState state = SessionState.INIT;

    // Ensure at most one in-flight receive to avoid piling up blocking reads.
    private final AtomicBoolean receiving = new AtomicBoolean(false);

    // Debug flag: enable with JVM arg -Dsonata.debug.session=true
    private static final boolean DEBUG = Boolean.getBoolean("sonata.debug.session");

    private static void dbg(String msg) {
        if (DEBUG) {
            System.out.println("[session] " + msg);
        }
    }

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
        state = SessionState.RUNNING;   //sessionState

        dbg("start(): state=" + state + ", fsm=" + fsm.getState() + ", local=" + connection.getLocalAddress() + ", remote=" + connection.getRemoteAddress());

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
        state = SessionState.RUNNING;   //sessionState.Running 不发包
        dbg("startPassive(): state=" + state + ", fsm=" + fsm.getState() + ", local=" + connection.getLocalAddress() + ", remote=" + connection.getRemoteAddress());
    }

    /**
     *向应用层传输数据，调度fsm, 控制io
     */
    @Override
    public void onReadable() {
        if (state != SessionState.RUNNING)
            return;

        // 如果已有一次异步 read 在进行中，则本次调度直接跳过。
        // 降低竞态和任务堆积
        // driveOnce() 可能非常频繁调用；JdkTcpConnection.receiveAsync() 内部是阻塞 read，堆积任务会放大竞态。
        if (!receiving.compareAndSet(false, true)) {
            return;
        }

        ByteBuffer buffer = ByteBuffer.allocate(4096);

        dbg("onReadable(): schedule receiveAsync, fsm=" + fsm.getState() + ", thread=" + Thread.currentThread().getName());

        try{
            connection.receiveAsync(buffer).thenAccept(n -> {
                try {
                    dbg("receiveAsync completed: n=" + n + ", thread=" + Thread.currentThread().getName() + ", fsm=" + fsm.getState());

                    if(n == null || n < 0)
                    {
                        dbg("receiveAsync: remote closed -> close()");
                        close();
                        return;
                    }

                    buffer.flip();
                    var packets = PacketCodec.decode(buffer);
                    dbg("decode: packets=" + (packets == null ? "null" : packets.size()));

                    for (Packet packet : packets)
                    {
                        dbg("dispatch packet: " + packet);
                        ProtocolEffect effect = fsm.handlePacket(packet);
                        apply(effect);
                        if(state != SessionState.RUNNING)
                            return;

                        //向应用层导出数据
                        if (listener != null) {
                            listener.exposeReceived(List.of(packet));
                        }
                    }
                } finally {
                    receiving.set(false);
                }
            });



        }catch (TransportException e)
        {
            receiving.set(false);
            dbg("onReadable(): TransportException -> close(): " + e.getMessage());
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

        dbg("apply(): action=" + effect.getAction() + ", outputs=" + (effect.getOutputs() == null ? 0 : effect.getOutputs().size()) + ", fsm=" + fsm.getState());

        try{
            //控制tcp连接
            switch (effect.getAction())
            {
                //TODO:可以加入default暴露问题语义，暂时不做处理
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
