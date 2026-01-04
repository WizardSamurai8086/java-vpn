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
     * 主动发收请求
     * @return  ProtocolEffect
     */
    public ProtocolEffect onSessionStart() {
        if (state != ProtocolState.INIT) {
            return ProtocolEffect.closeError();
        }
        // 客户端侧协议：主动发 HELLO
        // IMPORTANT: after sending HELLO, we must expect HELLO_ACK next.
        // So we advance to NEGOTIATING here.
        state = ProtocolState.NEGOTIATING;  //激活本地fsm
        return ProtocolEffect.send(List.of(Packet.hello()));
    }

    /**
     *
     * 协议状态机
     * 实现响应式输出
     * @param packet 待处理包
     * @return  ProtocolEffect to Session
     */
    public ProtocolEffect handlePacket(Packet packet) {
        switch (this.state) {
            case INIT:
                if(packet.getHeader().getType() == PacketType.HELLO)
                {
                    state = ProtocolState.NEGOTIATING;
                    Packet ack = Packet.helloACK();
                    return ProtocolEffect.send(List.of(ack));
                }

                return ProtocolEffect.closeError();
            case NEGOTIATING:
                if(packet.getHeader().getType() == PacketType.HELLO_ACK)
                {

                    state = ProtocolState.READY;
                    return ProtocolEffect.none();
                }
                return ProtocolEffect.closeError();
            case READY:
                if(packet.getHeader().getType() == PacketType.DATA)
                {
                    return ProtocolEffect.none();
                }
                if(packet.getHeader().getType() == PacketType.CLOSE) {

                    state = ProtocolState.CLOSE;
                    return ProtocolEffect.closeGraceful();
                }
                return ProtocolEffect.closeError();
            default:
                return ProtocolEffect.closeError();


        }
    }



}
