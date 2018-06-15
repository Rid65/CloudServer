

import java.io.Serializable;

public class AuthMessage extends AbstractMessage {

    private String username;
    private String pass;

    public AuthMessage(String username, String pass) {
        this.username = username;
        this.pass = pass;
    }

    public String getUsername() {
        return username;
    }

    public String getPass() {
        return pass;
    }
}
