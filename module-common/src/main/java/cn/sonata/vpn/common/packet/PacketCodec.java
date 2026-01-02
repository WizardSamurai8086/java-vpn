package cn.sonata.vpn.common.packet;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;


//Packet编解码工具类
public class PacketCodec {



    /**
     * Packet encoder
     * @param packet 传入待编码packet
     * @return buffer 返回一个ByteBuffer
     */
    public static ByteBuffer encode(Packet packet) {

        PacketHeader header = packet.getHeader();
        ByteBuffer body = packet.getBody();

        int bodyLength = (body == null) ? 0 : body.remaining();     //修复前直接remaining可能会导致NPE

        //packet总长
        int totalLength = Integer.BYTES + Short.BYTES + Short.BYTES + Integer.BYTES + bodyLength;

        ByteBuffer buffer = ByteBuffer.allocate(totalLength);

        buffer.putInt(header.getMagic());
        buffer.putShort(header.getVersion());
        buffer.putShort(header.getType().getCode());
        buffer.putInt(header.getLength());

        //同上，防止NPE
        if (bodyLength > 0) {
            buffer.put(body.slice());
        }

        buffer.flip();  //Change to write.
        return buffer;


    }


    /**
     * Packet decoder
     * @param buffer    传入byteBuffer
     * @return List<Packet> 如出现错误返回空值
     */
    public static List<Packet> decode(ByteBuffer buffer) {
        List<Packet> packets = new ArrayList<>();

        int headerLength = Integer.BYTES + Short.BYTES + Short.BYTES + Integer.BYTES;

        while (true) {
            if (buffer.remaining() < headerLength) {
                break;
            }

            buffer.mark();

            int magic = buffer.getInt();
            short version = buffer.getShort();
            short typeCode = buffer.getShort();
            int length = buffer.getInt();

            if (buffer.remaining() < length) {
                buffer.reset();
                break;
            }

            byte[] bodyBytes = new byte[length];
            buffer.get(bodyBytes);

            PacketHeader header = new PacketHeader(
                    magic,
                    version,
                    PacketType.getPacketType(typeCode),
                    length
            );

            packets.add(new Packet(header, ByteBuffer.wrap(bodyBytes)));
        }

        return packets;
    }



}
