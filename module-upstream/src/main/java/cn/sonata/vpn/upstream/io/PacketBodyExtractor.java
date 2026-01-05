package cn.sonata.vpn.upstream.io;

import cn.sonata.vpn.common.packet.Packet;

import java.nio.ByteBuffer;

/**
 * 把Packet -> byte[]
 */
public final class PacketBodyExtractor {

    private PacketBodyExtractor() {
    }

    public static byte[] toBytes(Packet packet) {
        if (packet == null) {
            return new byte[0];
        }
        ByteBuffer body = packet.getBody();
        if (body == null) {
            return new byte[0];
        }

        ByteBuffer ro = body.asReadOnlyBuffer();
        byte[] bytes = new byte[ro.remaining()];
        ro.get(bytes);
        return bytes;
    }
}

