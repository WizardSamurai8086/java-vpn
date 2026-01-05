package cn.sonata.vpn.upstream.app;

import cn.sonata.vpn.common.packet.Packet;
import cn.sonata.vpn.common.packet.PacketCodec;
import cn.sonata.vpn.common.packet.PacketHeader;
import cn.sonata.vpn.common.packet.PacketType;
import cn.sonata.vpn.common.transport.TransportException;
import cn.sonata.vpn.common.transport.tcp.JdkTcpServer;
import cn.sonata.vpn.common.transport.tcp.TcpConnection;
import cn.sonata.vpn.common.transport.tcp.TcpServer;
import cn.sonata.vpn.upstream.io.AckFactory;
import cn.sonata.vpn.upstream.io.PacketBodyExtractor;
import cn.sonata.vpn.upstream.io.PacketFileWriter;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class Main {

    private static final InetSocketAddress LISTEN = new InetSocketAddress("127.0.0.1", 9001);

    // MAX16kb
    private static final int MAX_BODY_BYTES = 16 * 1024;

    // TCP 是流：需要累积 buffer 处理半包/粘包
    private static final int ACC_INITIAL_CAPACITY = 64 * 1024;

    public static void main(String[] args) {
        try {
            run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void run() throws TransportException, IOException, ExecutionException, InterruptedException {
        TcpServer server = new JdkTcpServer();
        server.bind(LISTEN);
        System.out.println("[upstream] listening on " + LISTEN);

        TcpConnection conn = server.accept();
        System.out.println("[upstream] accepted: " + conn.getRemoteAddress());

        File outDir = new File("upstream-out");
        if (!outDir.exists() && !outDir.mkdirs()) {
            throw new IOException("Failed to create dir: " + outDir.getAbsolutePath());
        }

        ByteBuffer acc = ByteBuffer.allocate(ACC_INITIAL_CAPACITY);
        ByteBuffer readBuf = ByteBuffer.allocate(8 * 1024);

        while (true) {
            //把packet写到readBuf里做进一步处理
            readBuf.clear();
            int n = conn.receiveAsync(readBuf).get();
            if (n < 0) {
                System.out.println("[upstream] stream closed");
                break;
            }
            if (n == 0) {
                continue;
            }

            //读取完成，切换读取模式
            readBuf.flip();
            acc = append(acc, readBuf);

            // 尝试从累积区解出尽可能多的 packet
            acc.flip();
            List<Packet> packets;
            try {
                packets = PacketCodec.decode(acc);
            } catch (Exception decodeError) {
                // 解码异常：为了不死循环，丢弃当前累积数据
                System.out.println("[upstream] decode failed, drop accumulator: " + decodeError.getMessage());
                acc.clear();
                continue;
            }

            // decode 内部使用 mark/reset，退出时 position 会停在“已消费”的末尾
            // compact 把剩余未消费的数据前移，继续累积
            acc.compact();

            if (packets.isEmpty()) {
                continue;
            }

            for (Packet packet : packets) {
                PacketHeader header = (packet == null) ? null : packet.getHeader();
                if (header == null) {
                    continue;
                }

                // 只处理 DATA，其它忽略
                if (header.getType() != PacketType.DATA) {
                    continue;
                }

                if (header.getLength() < 0 || header.getLength() > MAX_BODY_BYTES) {
                    System.out.println("[upstream] drop abnormal DATA length=" + header.getLength());
                    continue;
                }

                byte[] bodyBytes = PacketBodyExtractor.toBytes(packet);

                String fileName = "packet-" + Instant.now().toEpochMilli() + ".txt";
                File outFile = new File(outDir, fileName);
                PacketFileWriter.writeBytes(outFile, bodyBytes);
                System.out.println("[upstream] saved " + outFile.getName() + " (" + bodyBytes.length + " bytes)");

                Packet ack = AckFactory.okFileSaved(header, fileName);
                conn.sendAsync(PacketCodec.encode(ack));
            }
        }
    }

    /**
     * 把新读到的数据追加到累积缓冲区；空间不足时自动扩容。
     */
    private static ByteBuffer append(ByteBuffer acc, ByteBuffer incoming) {
        if (incoming == null || !incoming.hasRemaining()) {
            return acc;
        }

        int need = incoming.remaining();

        // acc 此时应为 write mode（position=已写入，limit=capacity）
        if (acc.remaining() >= need) {
            acc.put(incoming);
            return acc;
        }


        //确保最小容量
        int minCap = acc.position() + need;
        int newCap = Math.max(acc.capacity() * 2, minCap);
        ByteBuffer bigger = ByteBuffer.allocate(newCap);


        acc.flip();
        bigger.put(acc);
        bigger.put(incoming);
        return bigger;
    }
}
