package cn.sonata.vpn.common.session;

public interface SessionListener {

    void onSessionClosed(SessionCloseReason reason);

}
