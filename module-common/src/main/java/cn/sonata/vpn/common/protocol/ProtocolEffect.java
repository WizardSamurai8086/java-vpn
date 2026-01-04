package cn.sonata.vpn.common.protocol;

import cn.sonata.vpn.common.packet.Packet;

import java.util.List;

public final class ProtocolEffect {

    public enum Action {
        NONE,
        SEND,
        CLOSE_GRACEFUL,
        CLOSE_ERROR
    }

    private final Action action;
    private final List<Packet> outputs;

    private ProtocolEffect(Action action, List<Packet> outputs) {
        this.action = action;
        this.outputs = outputs;
    }
    /**
     * NONE: 不发送、不关闭，连接保持，Session 继续接收
     * @return ProtocolEffect
     */
    public static ProtocolEffect none() {
        return new ProtocolEffect(Action.NONE, null);
    }

    public static ProtocolEffect send(List<Packet> outputs) {
        return new ProtocolEffect(Action.SEND, outputs);
    }

    public static ProtocolEffect closeGraceful() {
        return new ProtocolEffect(Action.CLOSE_GRACEFUL, null);
    }

    public static ProtocolEffect closeError() {
        return new ProtocolEffect(Action.CLOSE_ERROR, null);
    }

    /**
     * 向 outputs 添加一条应用层数据
     */
    public void addOutput(Packet packet) {
        if (packet != null) {
            outputs.add(packet);
        }
    }

    /**
     * 向 outputs 批量添加应用层数据
     */
    public void addOutputs(List<Packet> packets) {
        if (packets != null && !packets.isEmpty()) {
            outputs.addAll(packets);
        }
    }
    public List<Packet> getOutputs() {
        return outputs;
    }

    public Action getAction() {
        return action;
    }
}

