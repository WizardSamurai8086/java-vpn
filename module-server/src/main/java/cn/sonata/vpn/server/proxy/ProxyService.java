package cn.sonata.vpn.server.proxy;

import cn.sonata.vpn.common.packet.Packet;
import cn.sonata.vpn.common.packet.PacketCodec;
import cn.sonata.vpn.common.transport.TransportException;
import cn.sonata.vpn.common.transport.tcp.TcpConnection;
import cn.sonata.vpn.server.io.AppIO;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * 此类用于构造一个最小代理供server使用
 *<p>
 * ddl取舍暂时将从AppIO里取得的数据转存到文件
 *<p>转发到一个本地端口
 */
public class ProxyService {

    private final TcpConnection upstreamConnection;
    private final TcpConnection clientConnection;

    // upstream 回包是 TCP stream：需要累积处理半包/粘包
    private ByteBuffer upstreamAcc = ByteBuffer.allocate(64 * 1024);
    private final ByteBuffer upstreamReadBuf = ByteBuffer.allocate(8 * 1024);

    private volatile boolean pumpStarted = false;

    public ProxyService(TcpConnection upstreamConnection, TcpConnection clientConnection) {
        this.upstreamConnection = upstreamConnection;
        this.clientConnection = clientConnection;
    }

    public void send(AppIO appIO) throws TransportException {
        //捕捉从session捕捉到的packet
        Packet packet = appIO.packetSnapshot();
        if (packet == null) {
            System.err.println("[proxy] skip send: packetSnapshot() returned null");
            return;
        }

        startPumpIfNeeded();

        upstreamConnection.sendAsync(PacketCodec.encode(packet));
    }

    private void startPumpIfNeeded() {
        if (pumpStarted) {
            return;
        }
        pumpStarted = true;

        /**
         * 新开线程
         */
        Thread t = new Thread(this::pumpLoop, "proxy-upstream-pump");
        t.setDaemon(true);
        t.start();
    }

    private void pumpLoop() {
        while (true) {
            try {
                if (clientConnection == null || clientConnection.isClosed()) {
                    return;
                }
                if (upstreamConnection == null || upstreamConnection.isClosed()) {
                    return;
                }

                upstreamReadBuf.clear();
                //阻塞线程
                int n = upstreamConnection.receiveAsync(upstreamReadBuf).get();
                if (n <= 0) {
                    // 0: 暂时没读到；<0: closed（实现可能不同，但都退出）
                    if (n < 0) {
                        return;
                    }
                    continue;
                }

                upstreamReadBuf.flip();
                upstreamAcc = append(upstreamAcc, upstreamReadBuf);

                upstreamAcc.flip();
                List<Packet> packets = PacketCodec.decode(upstreamAcc);
                upstreamAcc.compact();

                if (packets.isEmpty()) {
                    continue;
                }
                for (Packet p : packets) {
                    if (p == null) continue;
                    System.out.println("[proxy] upstream->client packet: " + p);
                    clientConnection.sendAsync(PacketCodec.encode(p));
                }
            } catch (Exception e) {
                System.err.println("[proxy] pump upstream->client failed: " + e.getMessage());
                return;
            }
        }
    }

    /**
     * 把新数据追加到累积缓冲区；空间不足时扩容。
     */
    private static ByteBuffer append(ByteBuffer acc, ByteBuffer incoming) {
        if (incoming == null || !incoming.hasRemaining()) {
            return acc;
        }

        int need = incoming.remaining();

        // acc write mode
        if (acc.remaining() >= need) {
            acc.put(incoming);
            return acc;
        }

        int minCap = acc.position() + need;
        int newCap = Math.max(acc.capacity() * 2, minCap);
        ByteBuffer bigger = ByteBuffer.allocate(newCap);

        acc.flip();
        bigger.put(acc);
        bigger.put(incoming);
        return bigger;
    }
}
