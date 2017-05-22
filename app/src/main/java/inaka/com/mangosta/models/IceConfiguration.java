package inaka.com.mangosta.models;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by rafalslota on 22/05/2017.
 */

public class IceConfiguration extends RealmObject {
    private static final String DEFAULT_TURN_ADDRESS = "127.0.0.1";
    private static final int DEFAULT_TURN_PORT = 3478;
    private static final String DEFAULT_TURN_REALM = "localhost";
    private static final String DEFAULT_TURN_USERNAME = "username";
    private static final String DEFAULT_TURN_PASSWORD = "secret";

    @PrimaryKey
    private int turnServerId = 1;

    private String turnAddress;
    private int turnPort;
    private String turnRealm;
    private String turnUsername;
    private String turnPassword;

    public static IceConfiguration defaultConfiguration() {
        IceConfiguration conf = new IceConfiguration();
        conf.setTurnAddress(DEFAULT_TURN_ADDRESS);
        conf.setTurnPort(DEFAULT_TURN_PORT);
        conf.setTurnRealm(DEFAULT_TURN_REALM);
        conf.setTurnUsername(DEFAULT_TURN_USERNAME);
        conf.setTurnPassword(DEFAULT_TURN_PASSWORD);

        return conf;
    }

    public String getTurnAddress() {
        return turnAddress;
    }

    public void setTurnAddress(String turnAddress) {
        this.turnAddress = turnAddress;
    }

    public int getTurnPort() {
        return turnPort;
    }

    public void setTurnPort(int turnPort) {
        this.turnPort = turnPort;
    }

    public String getTurnRealm() {
        return turnRealm;
    }

    public void setTurnRealm(String turnRealm) {
        this.turnRealm = turnRealm;
    }

    public String getTurnUsername() {
        return turnUsername;
    }

    public void setTurnUsername(String turnUsername) {
        this.turnUsername = turnUsername;
    }

    public String getTurnPassword() {
        return turnPassword;
    }

    public void setTurnPassword(String turnPassword) {
        this.turnPassword = turnPassword;
    }
}
