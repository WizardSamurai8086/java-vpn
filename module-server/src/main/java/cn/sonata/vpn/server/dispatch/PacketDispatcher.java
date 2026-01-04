package cn.sonata.vpn.server.dispatch;

import cn.sonata.vpn.common.packet.Packet;

import java.util.List;

public class PacketDispatcher {

    private final List<Packet> packets;

    private PacketDispatcher(List<Packet> packets) {
        this.packets = packets;
    }

    public static PacketDispatcher create(List<Packet> packets) {
        return new PacketDispatcher(packets);
    }

    //TODO :处理特殊语义，调用AppIO
}
