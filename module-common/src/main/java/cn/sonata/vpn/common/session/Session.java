package cn.sonata.vpn.common.session;

public interface Session {

    /**
     * 启动session
     * 触发FSM的onStrat语义
     */
    void start();

    /**
     * 外部驱动底层transport 可读
     */
    void onReadable();


    /**
     * 外部强制关闭
     * 对应SessionCloseReason: LOCAL_CLOSE
     */
    void close();
}

