package cn.sonata.vpn.client.core;

import cn.sonata.vpn.common.protocol.ProtocolState;
import cn.sonata.vpn.common.session.DefaultSession;
import cn.sonata.vpn.common.session.SessionState;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * NOTE: 此处有重复设计之嫌，ddl-counts
 */
public class ClientSession {

    private final DefaultSession session;


    /**
     * ready 表示“对外语义准备就绪”
     * 例如：握手完成、可以对外提供服务
     * 注意：
     * - 本类本身不决定 ready 何时变为 true
     * - ready 必须由协议推进路径中的其他代码设置
     */
    private volatile boolean ready = false;
    private final Object sessionLock = new Object();

    public enum StepResult {
        /** 本次未推进（例如 session 未 RUNNING） */
        NOOP,

        /** 成功尝试推进了一步协议处理 */
        PROGRESSED,

        /** 连接已关闭或不可用，session 不可继续 */
        CLOSED,

        /** 驱动过程中发生异常 */
        ERROR
    }
    private ClientSession(DefaultSession session) {
        this.session = session;
    }

    public static ClientSession create(DefaultSession session) {
        return new ClientSession(session);
    }

    public DefaultSession getSession() {
        return session;
    }

    public void start() {
        if (!session.getConnection().isConnected()
                || session.getConnection().isClosed()) {
            return;
        }

        if (session.getFsm().getState() != ProtocolState.INIT
                || session.getState() != SessionState.INIT) {
            return;
        }

        session.start();


    }


    /**
     * driveOnce 当前语义：
     * <p>
     * 【阶段性实现】
     * - 本实现中，driveOnce 直接调用 session.onReadable()
     * - 即 driveOnce 同时承担 IO 拉取 + 协议推进的职责
     * <p>
     * 【设计说明】
     * - 严格模型中，IO 拉取、Packet dispatch、FSM 推进应拆分
     * - 当前实现为保证教学演示与验收进度，暂不做语义拆分
     * <p>
     * 【后续优化方向】
     * - onReadable 仅产出 Packet
     * - driveOnce 显式消费 Packet 并驱动 FSM
     */
    public StepResult driveOnce() {
        try {
            if (!session.getConnection().isConnected()
                    || session.getConnection().isClosed()) {
                return StepResult.CLOSED;
            }

            if (session.getState() != SessionState.RUNNING) {
                return StepResult.NOOP;
            }

            session.onReadable();
            return StepResult.PROGRESSED;

        } catch (Throwable t) {
            return StepResult.ERROR;
        }
    }


    /**
     * 查询 ready 状态
     * <p>
     * 作用：
     * - 非阻塞方式判断 session 是否“对外语义就绪”
     */
    public boolean isReady() {
        return ready;
    }

    /**
     * 阻塞等待 ready == true
     * <p>
     * 作用：
     * - 将“阻塞语义”限制在调用方
     * - 支持超时
     * <p>
     * 注意：
     * - 本方法不推进协议
     * - 必须配合外部持续调用 driveOnce()
     * <p>
     * 行为：
     * - 超时抛 TimeoutException
     * - 中断抛 InterruptedException
     */
    public void awaitReady(Duration timeout)
            throws TimeoutException, InterruptedException {

        long deadline = System.currentTimeMillis() + timeout.toMillis();

        synchronized (sessionLock) {
            while (!ready) {
                long remain = deadline - System.currentTimeMillis();
                if (remain <= 0) {
                    throw new TimeoutException("awaitReady timeout");
                }
                sessionLock.wait(remain);
            }
        }
    }

    /**
     * 由协议推进路径调用，用于标记 ready 并唤醒等待线程
     * 说明：
     * - 这是一个“钩子”，不一定现在用
     * - 握手完成 / 状态切换时可以调用
     */
    public void markReady() {
        synchronized (sessionLock) {
            ready = true;
            sessionLock.notifyAll();
        }
    }

}
