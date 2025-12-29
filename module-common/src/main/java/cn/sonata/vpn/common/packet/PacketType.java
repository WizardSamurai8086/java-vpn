package cn.sonata.vpn.common.packet;

public enum PacketType {
    HELLO((short) 1),
    HELLO_ACK((short) 2),
    DATA((short) 3),
    CLOSE((short) 4);

    private final short code;

    static PacketType getPacketType(short code) {
        switch (code) {
            case 1:
                return HELLO;
            case 2:
                return HELLO_ACK;
            case 3:
                return DATA;
            case 4:
                return CLOSE;
            default:
                 throw new IllegalArgumentException("Invalid packet type" + code);
        }
    }
    PacketType(short code) {
        this.code = code;
    }
    public short getCode() {
        return code;
    }
}
