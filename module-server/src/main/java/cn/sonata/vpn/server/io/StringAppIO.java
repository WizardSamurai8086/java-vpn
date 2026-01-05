package cn.sonata.vpn.server.io;

import cn.sonata.vpn.common.packet.Packet;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A teaching-level AppIO implementation.
 * NOTE:这里是弱O语义的，只存到内存，不进行转发
 * TODO:有proxy模块后直接调用来实现转发效果
 * It treats packet payload as UTF-8 text,
 * prints it, and stores it in memory.
 */
public class StringAppIO implements AppIO {

    private final List<String> receivedMessages = new ArrayList<>();
    private volatile Packet receivedPacket;
    public StringAppIO() {

    }

    @Override
    public void onPacket(Packet packet) {
        if (packet == null || packet.getBody() == null) {
            return;
        }

        ByteBuffer readOnlyBuffer = packet.getBody();

        String message = StandardCharsets.UTF_8.decode(readOnlyBuffer).toString();

        // 打印
        System.out.println("[AppIO] received message: " + message);

        //存到内存
        receivedMessages.add(message);

        // 让 proxy 层能拿到“最近一次入站包”用于转发
        receivedPacket = packet;
    }


    @Override
    public List<String> stringSnapshot() {
        return List.copyOf(receivedMessages);
    }

    @Override
    public Packet packetSnapshot() {
        Packet snapshot = receivedPacket;
        if (snapshot == null || snapshot.getBody() == null) {
            System.err.println("[AppIO] receivedPacket body is null");
            return null;
        }
        return snapshot;
    }
}
