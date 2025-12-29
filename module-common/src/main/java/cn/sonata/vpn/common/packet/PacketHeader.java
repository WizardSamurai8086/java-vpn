package cn.sonata.vpn.common.packet;




public class PacketHeader {

    private final int magic;          //    0x56504E44
    private final short version;      //    协议版本
    private final PacketType type;         //    包类型
    private final int length;         //    MAX16kb


    public PacketHeader(int magic, short version, PacketType type, int length) {
        this.magic = magic;
        this.version = version;
        this.type = type;
        this.length = length;
    }


    //getters
    public int getMagic() {
        return magic;
    }

    public short getVersion() {
        return version;
    }

    public PacketType getType() {
        return type;
    }

    public int getLength() {
        return length;
    }
}
