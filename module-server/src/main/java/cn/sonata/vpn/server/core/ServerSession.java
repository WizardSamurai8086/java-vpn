package cn.sonata.vpn.server.core;

import cn.sonata.vpn.common.protocol.ProtocolState;
import cn.sonata.vpn.common.session.DefaultSession;
import cn.sonata.vpn.common.session.SessionState;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

/**
 * ServerSession
 *
 * server 侧对 DefaultSession 的生命周期与调度封装。
 *
 * 设计目标：
 * - 明确 start / driveOnce / awaitReady 的语义边界
 * - 不在此处引入 IO / selector / proxy
 * - 保证 driveOnce 永不阻塞
 */
public final class ServerSession {

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

    private ServerSession(DefaultSession session) {
        this.session = Objects.requireNonNull(session, "session must not be null");
    }

    /**
     * 静态工厂方法
     *
     * 作用：
     * - 创建 ServerSession 实例
     * - 校验 session 不为 null
     *
     * 用法：
     * - 传入已构建完成、已建立连接的 DefaultSession
     */
    public static ServerSession create(DefaultSession session) {
        return new ServerSession(session);
    }

    /**
     * 暴露内部 DefaultSession
     *
     * 作用：
     * - 只读访问底层 session（状态 / connection / fsm 等）
     *
     * 注意：
     * - 是否允许修改，取决于 DefaultSession 的调用约束
     */
    public DefaultSession getSession() {
        return session;
    }

    /**
     * 启动入口（语义边界方法）
     *
     * 作用：
     * - 只做前置条件检查
     * - 满足条件时，将 session 从 INIT 激活为“可被 drive 的状态”
     *
     * 前置条件（任一不满足则直接 return）：
     * - connection 已连接且未关闭
     * - ProtocolFSM 状态为 INIT
     * - Session 状态为 INIT
     *
     * 注意：
     * - 不推进协议
     * - 不阻塞
     * - 可安全重复调用（非 INIT 状态会被忽略）
     */
    public void start() {
        if (!session.getConnection().isConnected()
                || session.getConnection().isClosed()) {
            return;
        }

        if (session.getFsm().getState() != ProtocolState.INIT
                || session.getState() != SessionState.INIT) {
            return;
        }

        session.startPassive();
    }

    /**
     * driveOnce 当前语义：
     *
     * 【阶段性实现】
     * - 本实现中，driveOnce 直接调用 session.onReadable()
     * - 即 driveOnce 同时承担 IO 拉取 + 协议推进的职责
     *
     * 【设计说明】
     * - 严格模型中，IO 拉取、Packet dispatch、FSM 推进应拆分
     * - 当前实现为保证教学演示与验收进度，暂不做语义拆分
     *
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
     *
     * 作用：
     * - 非阻塞方式判断 session 是否“对外语义就绪”
     */
    public boolean isReady() {
        return ready;
    }

    /**
     * 阻塞等待 ready == true
     *
     * 作用：
     * - 将“阻塞语义”限制在调用方
     * - 支持超时
     *
     * 注意：
     * - 本方法不推进协议
     * - 必须配合外部持续调用 driveOnce()
     *
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
