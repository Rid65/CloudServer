import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML
    TextField loginField;
    @FXML
    PasswordField passField;
    @FXML
    HBox actionPanel1;
    @FXML
    HBox actionPanel2;
    @FXML
    HBox authPanel;
    @FXML
    ListView<String> localList;
    @FXML
    ListView<String> cloudList;
    @FXML
    Label currentPathClient;
    @FXML
    Label currentPathCloud;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateLocalList();
    }

    /***************************************
     *  Область обработчиков событий
     **************************************/

    public void AuthAction(ActionEvent actionEvent) throws Exception {
        Client.getInstance().startListenChannel(this);
        AuthMessage message = new AuthMessage(loginField.getText(), passField.getText());
        Client.getInstance().sendMessage(message);
    }

    //-- Отправка файла с клиента на сервер
    public void LocalSendFile(ActionEvent actionEvent) {
        sendFileToServer();
    }

    public void LocalDeleteFile(ActionEvent actionEvent) {
        deleteFileLocal();
        updateLocalList();
    }

    public void LocalUpdateFile(ActionEvent actionEvent) {
        updateLocalList();
    }

    public void ServerDownloadFile(ActionEvent actionEvent) {
        downloadFileFromServer();
    }

    public void ServerDeleteFile(ActionEvent actionEvent) {
        deleteFileServer();
    }

    public void ServerUpdateFile(ActionEvent actionEvent) {
        updateCloudList();
    }

    /********************************
     *  Область служебных методов
     *******************************/

    //-- Обновление списка файлов на клиенте
    public void updateLocalList() {
        Platform.runLater(()->{
            ObservableList<String> listItems = FXCollections.observableArrayList();
            localList.setItems(listItems);
            List<String> fileList = new ArrayList<>();
            if (currentPathClient.getText().length() > 0) {
                fileList = Common.getFileList(Config.rootClient + "\\" + currentPathClient.getText(), true);
            } else {
                fileList = Common.getFileList(Config.rootClient, true);
            }
            localList.getItems().addAll(fileList);
        });
    }

    //-- Обновляет текущую директорию облачного хранилища
    public void updateCloudList() {
        Platform.runLater(()->{
            String cloudFullPath = Config.rootCloudServer + "\\" + Client.getInstance().username + currentPathCloud.getText();
            CommandMessage commandMessage = new CommandMessage(Commands.GET_CLOUD_DIR_TREE, cloudFullPath);
            Client.getInstance().sendMessage(commandMessage);
        });
    }

    //-- Обновляет переданную директорию облачного хранилища
    public void updateCloudList(String cloudFullPath) {
        Platform.runLater(()->{
            CommandMessage commandMessage = new CommandMessage(Commands.GET_CLOUD_DIR_TREE, cloudFullPath);
            Client.getInstance().sendMessage(commandMessage);
        });
    }

    public void updateLists() {
        updateLocalList();
        updateCloudList();
    }

    public void initializeCloudList(List<String> fileList) {
        Platform.runLater(()->{
            ObservableList<String> listItems = FXCollections.observableArrayList();
            cloudList.setItems(listItems);
            cloudList.getItems().addAll(fileList);
        });
    }

    public void updateInterface() {
        if (Client.getInstance().isLogged()) {
            actionPanel1.setVisible(true);
            actionPanel2.setVisible(true);
            authPanel.setVisible(false);
        }
    }

    //-- Переход по папкам в глубину на клиенте
    public void ClickedLocalList(MouseEvent mouseEvent) {
        if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
            if (mouseEvent.getClickCount() == 2) {
                ListView<String> localList = (ListView<String>) mouseEvent.getSource();
                String selectedItem = localList.getSelectionModel().getSelectedItem();
                String fullPath = getCurrentPathClient() + "\\" + selectedItem;
                Path path = Paths.get(fullPath);
                if (Files.isDirectory(path)) {
                    currentPathClient.setText(currentPathClient.getText() + "\\" + selectedItem);
                    updateLocalList();
                }
            }
        }
    }

    //-- Переход по папкам на сервере.
    //-- При двойном клике по папке с клиента отправляется запрос на получение списка файлов новой директории
    public void ClickedCloudList(MouseEvent mouseEvent) {
        if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
            if (mouseEvent.getClickCount() == 2) {
               Platform.runLater(()->{
                    ListView<String> cloudList = (ListView<String>) mouseEvent.getSource();
                    String selectedItem = cloudList.getSelectionModel().getSelectedItem();
                    String fullPath = getCurrentPathCloud() + "\\" + selectedItem;
                    updateCloudList(fullPath);
               });
            }
        }
    }

    //-- Загружаем файл с облака на клиент
    public void downloadFileFromServer() {
        String selectedItem = cloudList.getSelectionModel().getSelectedItem();
        String pathToFileCloud = getCurrentPathCloud() + "\\" + selectedItem;
        String pathToFileClient = getCurrentPathClient() + "\\" + selectedItem;
        Path path = Paths.get(pathToFileCloud);
        if (Files.isDirectory(path)) {

        } else {
            CommandMessage commandMessage = new CommandMessage(Commands.DOWNLOAD_FILE, pathToFileCloud, pathToFileClient);
            Client.getInstance().sendMessage(commandMessage);
        }
    }

    //-- Отправить файл с клиента в облако
    public void sendFileToServer() {
        String selectedItem = localList.getSelectionModel().getSelectedItem();
        Path pathToFileClient = Paths.get(getCurrentPathClient() + "\\" + selectedItem);
        if (Files.isDirectory(pathToFileClient)) {

        } else {
            sendChunkFileToServer(pathToFileClient);
        }
    }

    //-- Отправить файл в облако по частям
    public void sendChunkFileToServer(Path pathToFileClient) {
        String filename = pathToFileClient.getFileName().toString();
        String pathToSave = getCurrentPathCloud() + "\\" + filename;
        Common.sendChunkFile(pathToFileClient, pathToSave, Client.getInstance().getEncoder());
    }

    //-- Метод обрабатывает сообщение от сервера. Проверяет тип пришедщего объекта и решает, с ним делать
    public void processMessage(Object object) {
        if (object == null) {
            return;
        } else if (object instanceof FileMessage) {
            processFileMessage(object);
        } else if (object instanceof CommandMessage) {
            processCommandMessage(object);
        }
    }

    //-- обработка команд
    private void processCommandMessage(Object object) {
        CommandMessage message = (CommandMessage) object;
        if (message.getCommand() == Commands.AUTH_OK) {
            Client.getInstance().setLogged();
            Client.getInstance().setUsername((String) message.getParameter(0));
            updateInterface();
            initializeCloudList((List<String>) message.getParameter(1));
            System.out.println("Auth success!");
        } else if (message.getCommand() == Commands.AUTH_ERROR) {
            System.out.println("Auth failed!");
        } else if (message.getCommand() == Commands.FILE_TRANSFER_SUCCESS) {
            //System.out.println("File transfer success!");
            updateLists();
        } else if (message.getCommand() == Commands.FILE_TRANSFER_ERROR) {
            System.out.println("File transfer error!");
        } else if (message.getCommand() == Commands.GET_CLOUD_DIR_TREE) {
            initializeCloudList((List<String>) message.getParameter(0));
            setCurrentPathCloud((String) message.getParameter(1));
        } else if (message.getCommand() == Commands.DELETE_FILE_SUCCESS) {
            updateCloudList();
        } else if (message.getCommand() == Commands.DELETE_FILE_ERROR) {
            System.out.println("Ошибка при удалении файла на сервере!");
        } else if (message.getCommand() == Commands.UPDATE_LISTS) {
            updateLists();
        }
    }

    private void processFileMessage(Object object) {
        FileMessage msg = (FileMessage) object;
        Path path = Paths.get(msg.getPathToSave());
        Common.writeChunkFile(path, msg.getPartNumber(), msg.getFiledata());
        if (msg.getPartNumber() == msg.getPartCount() - 1) {
            updateLocalList();
        }
    }

    public String getCurrentPathCloud() {
        String fullPath = "";
        if (currentPathCloud.getText().length() > 0) {
            fullPath = Config.rootCloudServer + "\\" + Client.getInstance().username + currentPathCloud.getText();
        } else {
            fullPath = Config.rootCloudServer + "\\" + Client.getInstance().username;
        }
        return fullPath;
    }

    public String getCurrentPathClient() {
        String fullPath = "";
        if (currentPathClient.getText().length() > 0) {
            fullPath = Config.rootClient + currentPathClient.getText();
        } else {
            fullPath = Config.rootClient;
        }
        return fullPath;
    }

    public void setCurrentPathCloud(String path) {
        Platform.runLater(()->{
            currentPathCloud.setText(path);
        });
    }

    public void setCurrentPathClient(String path) {
        Platform.runLater(()->{
            currentPathClient.setText(path);
        });
    }

    public void deleteFileLocal() {
        String selectedItem = localList.getSelectionModel().getSelectedItem();
        String pathToFile = getCurrentPathClient() + "\\" + selectedItem;
        try {
            Files.delete(Paths.get(pathToFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteFileServer() {
        String selectedItem = cloudList.getSelectionModel().getSelectedItem();
        String pathToFile = getCurrentPathCloud() + "\\" + selectedItem;
        CommandMessage commandMessage = new CommandMessage(Commands.DELETE_FILE, pathToFile);
        Client.getInstance().sendMessage(commandMessage);
    }
}
