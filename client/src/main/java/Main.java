import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application{

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("geek_cloud.fxml"));
        primaryStage.setTitle("GeekCloud Client");
        primaryStage.setScene(new Scene(root, 700, 600));
        primaryStage.show();
    }

}
