package cn.sonata.vpn.upstream.io;

import cn.sonata.vpn.common.packet.Packet;
import cn.sonata.vpn.common.packet.PacketHeader;
import cn.sonata.vpn.common.packet.PacketType;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Builds a minimal DATA notification packet (ack) using existing common API.
 */
public final class AckFactory {

    private AckFactory() {
    }

    /**
     * Creates a DATA ack packet with body like: OK:<fileName>
     */
    public static Packet okFileSaved(PacketHeader requestHeader, String fileName) {
        byte[] ackBytes = ("OK:" + fileName).getBytes(StandardCharsets.UTF_8);
        PacketHeader ackHeader = new PacketHeader(
                requestHeader.getMagic(),
                requestHeader.getVersion(),
                PacketType.DATA,
                ackBytes.length
        );
        return new Packet(ackHeader, ByteBuffer.wrap(ackBytes));
    }
}

