package cn.sonata.vpn.server.io;

import cn.sonata.vpn.common.packet.Packet;

import java.util.List;

public interface AppIO {
    /**
     * 处理session层收到的packet，生成IO和应用层对接
     * @param packet 单包
     */
    void onPacket(Packet packet);

    List<String> stringSnapshot();
    Packet packetSnapshot();
}