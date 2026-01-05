package cn.sonata.vpn.server.dispatch;

import cn.sonata.vpn.common.packet.Packet;

import java.util.List;

/**
 * TODO:用于解析包内容含有的特殊语义
 * ddl下暂时仅关注直接转发，不对特殊语义进行操作。
 */
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
