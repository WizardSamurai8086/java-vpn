package cn.sonata.vpn.common.packet;


import java.nio.ByteBuffer;

public class    Packet {
    private final PacketHeader header;
    private final ByteBuffer body;

    /**
     * 构造函数
     * body.asReadOnlyBuffer()确保传输数据不被随意篡改。
     * @param header packet属性
     * @param body  数据本体
     */
    public Packet(PacketHeader header, ByteBuffer body) {
        //Header can't be null
        if (header == null) {
            throw new NullPointerException("header is null exception");
        }

        //ensure consistency
        if (body == null) {
            if (header.getLength() != 0) {
                throw new IllegalArgumentException(
                        "Header length must be 0 when body is null exception"
                );
            }
            this.header = header;
            this.body = null;
            return;
        }
        //ensure consistency
        if (header.getLength() != body.remaining()) {
            throw new IllegalArgumentException(
                    "Packet header length and body remaining mismatch exception"
            );
        }
        this.header = header;
        this.body = body.asReadOnlyBuffer();
    }

    /**
     *  header getter
     * @return  PacketHeader
     */
    public PacketHeader getHeader() {
        return header;
    }

    /**
     * body getter
     * @return body (may be null)
     */
    public ByteBuffer getBody() {
        //修复了因body为空导致的NPE
        if (body == null) {
            return null;
        }
        return body.asReadOnlyBuffer();
    }

    /**
     * 工厂方法构建特殊包
     * TYPE:HELLO
     * @return Packet
     */
    public static Packet hello() {
        PacketHeader header = new PacketHeader(0x56504E44, (short) 0x00, PacketType.HELLO, 0);
        return new Packet(header, null);
    }
    /**
     * 工厂方法构建特殊包
     * TYPE:HELLO_ACK
     * @return Packet
     */
    public static Packet helloACK() {

        PacketHeader header = new PacketHeader(0x56504E44, (short) 0x00, PacketType.HELLO_ACK, 0);
        return new Packet(header,null);
    }
    /**
     * 工厂方法构建特殊包
     * TYPE:CLOSE
     * @return Packet
     */
    public static Packet close() {
        PacketHeader header = new PacketHeader(0x56504E44, (short) 0x00, PacketType.CLOSE, 0);
        return new Packet(header,null);
    }


}
