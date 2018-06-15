import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class Client {

    private static Client ourInstance = new Client();
    private ObjectEncoderOutputStream oeos;
    private ObjectDecoderInputStream odis;
    private Socket socket;
    private boolean isLogged;
    public String username;

    public static Client getInstance() {
        return ourInstance;
    }


    private Client() {
    }

    public void startListenChannel(Controller controller) {
        this.oeos = null;
        this.odis = null;
        try {
            socket = new Socket("localhost", Config.PORT);
            oeos = new ObjectEncoderOutputStream(socket.getOutputStream());
            odis = new ObjectDecoderInputStream(socket.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
            disconnect();
        }

        Thread t1 = new Thread(() -> {
            try {
                while (true) {
                    Object message = odis.readObject();
                    controller.processMessage(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        });
        t1.setDaemon(true);
        t1.start();
    }

    public void sendMessage(Object obj) {
        try {
            oeos.writeObject(obj);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public void disconnect() {
        try {
            oeos.close();
            odis.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isLogged() {
        return isLogged;
    }

    public void setLogged() {
        isLogged = true;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public ObjectEncoderOutputStream getEncoder() {
        return oeos;
    }
}
