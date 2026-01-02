package cn.sonata.vpn.common.protocol;

/**
 * this used to define the state of connection
 *
 */
public enum ProtocolState {
    INIT((short) 1),
    NEGOTIATING((short) 2),
    READY((short) 3),
    CLOSE((short) 4);


    private final short code;


    static ProtocolState getStateByCode(short code) {

        switch (code) {
            case 1:
                return INIT;
            case 2:
                return NEGOTIATING;
            case 3:
                return READY;
            case 4:
                return CLOSE;
            default:
                throw new IllegalArgumentException(
                        "Invalid protocol state exception"
                );
        }
    }


    ProtocolState(short code) {
        this.code = code;
    }
    public short getCode() {
        return code;
    }
}
