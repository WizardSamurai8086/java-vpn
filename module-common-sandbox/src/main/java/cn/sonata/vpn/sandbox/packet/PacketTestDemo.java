package cn.sonata.vpn. sandbox.packet;

import cn. sonata.vpn.common. packet.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;


public class PacketTestDemo {

    public static void main(String[] args) {
        int magic = 0x56504E44;
        short version = 0x00;
        PacketType data = PacketType.DATA;
        int length = 8;

        PacketHeader h = new PacketHeader(magic, version, data, length);

        String s = "abcdefgh";
        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.put(s.getBytes());
        buf.flip();

        Packet pkt = new Packet(h, buf);
        System.out.println(s + " has been packed");

        // ===== 编码阶段 =====
        ByteBuffer byteBuffer = PacketCodec.encode(pkt);
        System.out.println("Encode completed");




        // Debug输出：编码后的内容（16进制）
        byte[] encodedBytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(encodedBytes);
        System.out.println("Encoded hex: " + bytesToHex(encodedBytes));
        System.out.println("Breakdown:");
        System.out.println("  Magic (4 bytes): " + String.format("0x%02X%02X%02X%02X",
                encodedBytes[0], encodedBytes[1], encodedBytes[2], encodedBytes[3]));
        System.out.println("  Version (2 bytes): " + String.format("0x%02X%02X",
                encodedBytes[4], encodedBytes[5]));
        System.out.println("  Type (2 bytes): " + String.format("0x%02X%02X",
                encodedBytes[6], encodedBytes[7]));
        System.out.println("  Length (4 bytes): " + String.format("0x%02X%02X%02X%02X",
                encodedBytes[8], encodedBytes[9], encodedBytes[10], encodedBytes[11]));
        System.out.println("  Body (8 bytes): " + new String(encodedBytes, 12, 8));

        // ===== 解码阶段 =====
        // 重新准备buffer，模拟从网络接收数据的场景
        byteBuffer. clear();
        byteBuffer. put(encodedBytes);
        byteBuffer.flip();

        System.out.println("\nBefore decode - position: " + byteBuffer.position());
        System.out.println("Before decode - remaining: " + byteBuffer. remaining());

        List<Packet> packets = PacketCodec.decode(byteBuffer);
        System.out.println("Decode completed");

        if (packets == null) {
            System.out.println("❌ ERROR: PacketDecode returned null!");
            System.out.println("After decode - remaining: " + byteBuffer.remaining());
            System.out.println("After decode - position: " + byteBuffer. position());
            return;
        }

        if (packets.isEmpty()) {
            System.out.println("❌ ERROR: PacketDecode returned empty list (possibly incomplete frame)");
            System.out.println("After decode - remaining: " + byteBuffer.remaining());
            System.out.println("After decode - position: " + byteBuffer.position());
            return;
        }

        Packet packet = packets.get(0);

        System. out.println("✅ Packet decoded successfully!");
        System.out. println("Decoded packet object: " + packet);

        // ===== 打印解码后的数据 =====
        PacketHeader h2 = packet.getHeader();
        System.out.println("\n--- Decoded Packet Header ---");
        System.out. println("Magic: " + String.format("0x%08X", h2.getMagic()) +
                " (" + h2.getMagic() + ")");
        System.out.println("Version: " + h2.getVersion());
        System.out.println("Type: " + h2.getType());
        System.out.println("Length: " + h2.getLength());

        ByteBuffer bodyBuffer = packet.getBody();
        if (bodyBuffer != null) {
            System.out.println("\n--- Decoded Packet Body ---");
            // Packet.getBody() 已经返回 readOnlyBuffer 且 position=0；这里无需 flip()
            byte[] data1 = new byte[bodyBuffer.remaining()];
            bodyBuffer.get(data1);
            String s1 = new String(data1, StandardCharsets.UTF_8);
            System.out.println("Body content: " + s1);
            System.out.println("Body bytes (hex): " + bytesToHex(data1));
        } else {
            System.out.println("Body为空");
        }
    }

    // 辅助方法：将字节数组转为16进制字符串
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }
}