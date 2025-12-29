package cn.sonata.vpn.common.packet;

import java.nio.ByteBuffer;


//Packet编解码工具类
public class PacketCodec {



    /**
     * Packet encoder
     * @param packet 传入待编码packet
     * @return buffer 返回一个ByteBuffer
     */
    public static ByteBuffer PacketEncode(Packet packet) {


        PacketHeader header = packet.getHeader();
        ByteBuffer body = packet.getBody();
        //packet总长
        int totalLength = Integer.BYTES + Short.BYTES + Short.BYTES + Integer.BYTES + body.remaining();

        ByteBuffer buffer = ByteBuffer.allocate(totalLength);

        buffer.putInt(header.getMagic());
        buffer.putShort(header.getVersion());
        buffer.putShort(header.getType().getCode());
        buffer.putInt(header.getLength());
        buffer.put(body.slice());

        buffer.flip();  //Change to write.
        return buffer;


    }


    /**
     * Packet decoder
     * @param buffer    传入byteBuffer
     * @return packet 如出现错误返回空值
     */
    public static Packet PacketDecode(ByteBuffer buffer) {

        int headerLength = Integer.BYTES + Short.BYTES + Short.BYTES + Integer.BYTES;

        //Guard clauses
        if (buffer.remaining() < headerLength) {
            return null;
        }

        //make mark locates position 0
        buffer.mark();

        //header capture
        int magic = buffer.getInt();
        short version = buffer.getShort();
        short typeCode = buffer.getShort();
        int length = buffer.getInt();

        if (buffer.remaining() < length) {
            buffer.reset();
            return null;
        }

        //body capture
        byte[] bodyBytes = new byte[length];

        buffer.get(bodyBytes);

        PacketHeader header= new PacketHeader(magic, version, PacketType.getPacketType(typeCode) , length);

        return new Packet(header, ByteBuffer.wrap(bodyBytes));


    }


}
