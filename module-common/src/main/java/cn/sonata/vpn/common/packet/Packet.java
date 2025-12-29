package cn.sonata.vpn.common.packet;


import java.nio.ByteBuffer;

public class Packet {
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
     * @return body
     */
    public ByteBuffer getBody() {
        return body.asReadOnlyBuffer();
    }
}
