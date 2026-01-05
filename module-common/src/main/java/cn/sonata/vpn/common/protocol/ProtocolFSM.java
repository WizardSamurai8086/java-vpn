package cn.sonata.vpn.common.protocol;

import cn.sonata.vpn.common.packet.Packet;
import cn.sonata.vpn.common.packet.PacketType;

import java.util.List;

/**
 * this ProtocolFSM is used to ctrl the system scheduling of protocol.
 *
 */
public class ProtocolFSM {

    private ProtocolState state;

    // Debug flag: enable with JVM arg -Dsonata.debug.fsm=true
    private static final boolean DEBUG = Boolean.getBoolean("sonata.debug.fsm");

    private static void dbg(String msg) {
        if (DEBUG) {
            System.out.println("[fsm] " + msg);
        }
    }

    /**
     * 私有构造函数，工厂方法
     */
    private ProtocolFSM() {

        this.state = ProtocolState.INIT;

    }

    /**
     *静态工厂方法构造FSM
     * @return protocolFSM
     */
    public static ProtocolFSM create() {
        return new ProtocolFSM();
    }

    /*===getter===*/
    public ProtocolState getState() {
        return state;
    }

    public boolean isClosed() {
        return this.state == ProtocolState.CLOSE;
    }

    /**
     * 主动发送请求
     * @return  ProtocolEffect
     */
    public ProtocolEffect onSessionStart() {
        if (state != ProtocolState.INIT) {
            dbg("onSessionStart in non-INIT state=" + state + " -> CLOSE_ERROR");
            return ProtocolEffect.closeError();
        }
        // 客户端侧协议：主动发 HELLO
        // IMPORTANT: after sending HELLO, we must expect HELLO_ACK next.
        // So we advance to NEGOTIATING here.
        state = ProtocolState.NEGOTIATING;  //激活本地fsm
        dbg("onSessionStart: send HELLO, next state=" + state);
        return ProtocolEffect.send(List.of(Packet.hello()));
    }

    /**
     *
     * 协议状态机
     * 实现响应式输出
     * FSM 产出 ProtocolEffect，由 Session/Transport 层执行发送/关闭等副作用（side effects）
     * @param packet 待处理包
     * @return  ProtocolEffect to Session
     *
     */
    public ProtocolEffect handlePacket(Packet packet) {

        if (packet == null) {
            dbg("handlePacket: null packet in state=" + state + " -> CLOSE_ERROR");
            return ProtocolEffect.closeError();
        }
        PacketType type = packet.getHeader().getType();
        dbg("handlePacket: state=" + state + ", type=" + type);

        switch (this.state) {
            case INIT:
                if(packet.getHeader().getType() == PacketType.HELLO)
                {
                    state = ProtocolState.NEGOTIATING;
                    Packet ack = Packet.helloACK();
                    dbg("INIT: recv HELLO -> send HELLO_ACK, next state=" + state);
                    return ProtocolEffect.send(List.of(ack));
                }

                dbg("INIT: unexpected type=" + type + " -> CLOSE_ERROR");
                return ProtocolEffect.closeError();
            case NEGOTIATING:
                if(packet.getHeader().getType() == PacketType.HELLO_ACK)
                {

                    state = ProtocolState.READY;
                    dbg("NEGOTIATING: recv HELLO_ACK -> NONE, next state=" + state);
                    return ProtocolEffect.none();
                }
                // 最小容错：握手阶段收到应用层 DATA 时先忽略，避免直接关闭连接
                if (packet.getHeader().getType() == PacketType.DATA) {
                    dbg("NEGOTIATING: recv DATA -> NONE (ignored)");
                    return ProtocolEffect.none();
                }
                dbg("NEGOTIATING: unexpected type=" + type + " -> CLOSE_ERROR");
                return ProtocolEffect.closeError();
            case READY:
                if(packet.getHeader().getType() == PacketType.DATA)
                {
                    dbg("READY: recv DATA -> NONE (no output)");
                    return ProtocolEffect.none();
                }
                if(packet.getHeader().getType() == PacketType.CLOSE) {

                    state = ProtocolState.CLOSE;
                    dbg("READY: recv CLOSE -> CLOSE_GRACEFUL, next state=" + state);
                    return ProtocolEffect.closeGraceful();
                }
                dbg("READY: unexpected type=" + type + " -> CLOSE_ERROR");
                return ProtocolEffect.closeError();
            default:
                dbg("DEFAULT: state=" + state + ", type=" + type + " -> CLOSE_ERROR");
                return ProtocolEffect.closeError();


        }
    }



}
