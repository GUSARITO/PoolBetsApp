package sample;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import sample.models.MySQLServer;
import sample.models.Server;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main extends Application {

    private ListView<String> onlineList, messagesList;
    private Server server = null;
    private MySQLServer sql = null;
    private int port;

    private static final String IPADDRESS_PATTERN =
            "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";


    private VBox createVBoxLayout(ListView<String> list) {

        Label label = new Label();

        if (list == onlineList)
            label.setText("Online:");
        else
            label.setText("Messages:");

        list.setPrefHeight(460);
        list.getItems().addListener(new ListChangeListener<String>(){
            @Override
            public void onChanged(javafx.collections.ListChangeListener.Change<? extends String> c) {
                list.scrollTo(c.getList().size() - 1);
            }
        });

        VBox box = new VBox(label, list);
        box.setSpacing(5);

        return box;
    }

    private HBox createHBoxLayout(Node... lists) {

        HBox box = new HBox(lists);
        box.setSpacing(10);
        box.setPadding(new Insets(10));
        box.setAlignment(Pos.CENTER);

        for (Node i: lists)
            HBox.setHgrow(i, Priority.ALWAYS);

        return box;
    }

    private HBox createHBoxLayout(String textLabel) {

        Label serverLabel = new Label(textLabel);
        serverLabel.setPrefWidth(100);
        serverLabel.setAlignment(Pos.CENTER);

        Label ipLabel = new Label("IP Address:");
        TextField ipTextField = new TextField();
        ipTextField.setText((textLabel.charAt(0) == 'M') ? "127.0.0.1" : "192.168.1.100");

        Label portLabel = new Label("Port:");
        TextField portTextField = new TextField();
        portTextField.setText((textLabel.charAt(0) == 'M') ? "3306" : "11296");

        ToggleButton launchButton = (new ToggleButton());
        launchButton.setText((textLabel.charAt(0) == 'M') ? "Connect" : "Launch");
        launchButton.setMinWidth(100);

        HBox box = new HBox(serverLabel, ipLabel, ipTextField, portLabel, portTextField, launchButton);
        box.setSpacing(10);
        box.setPadding(new Insets(10));
		box.setAlignment(Pos.CENTER);

        HBox.setHgrow(ipLabel, Priority.ALWAYS);
        HBox.setHgrow(ipTextField, Priority.ALWAYS);
        HBox.setHgrow(portLabel, Priority.ALWAYS);
        HBox.setHgrow(portTextField, Priority.ALWAYS);
        HBox.setHgrow(launchButton, Priority.ALWAYS);

        launchButton.setOnMouseClicked(event -> {
            if (launchButton.getText().equals("Launch") || launchButton.getText().equals("Connect")) {
                if (validateIP(ipTextField.getText()) && (validatePort(portTextField.getText()))) {
                    try {
                        if (textLabel.charAt(0) == 'M') {
                            sql = new MySQLServer(ipTextField.getText(), port);

                            launchButton.setText((textLabel.charAt(0) == 'M') ? "Disconnect" : "Stop");

                            DateTimeFormatter formatter = DateTimeFormatter.ISO_TIME;
                            String time = LocalTime.now().format(formatter).substring(0, 8);

                            ObservableList<String> items = messagesList.getItems();
                            items.addAll(FXCollections.observableArrayList("[" + time + "] " +
                                    textLabel +
                                    " (" + ipTextField.getText() + ":" + portTextField.getText() + ") " +
                                    "was " + ((textLabel.charAt(0) == 'M') ? "connected" : "launched")));
                            messagesList.setItems(items);

                            ipTextField.setDisable(true);
                            portTextField.setDisable(true);
                        }
                        else {
                            if (sql != null) {
                                if (!sql.getConnection().isClosed()) {
                                    server = new Server(Main.this, sql, port, ipTextField.getText());

                                    launchButton.setText((textLabel.charAt(0) == 'M') ? "Disconnect" : "Stop");

                                    DateTimeFormatter formatter = DateTimeFormatter.ISO_TIME;
                                    String time = LocalTime.now().format(formatter).substring(0, 8);

                                    ObservableList<String> items = messagesList.getItems();
                                    items.addAll(FXCollections.observableArrayList("[" + time + "] " +
                                            textLabel +
                                            " (" + ipTextField.getText() + ":" + portTextField.getText() + ") " +
                                            "was " + ((textLabel.charAt(0) == 'M') ? "connected" : "launched")));
                                    messagesList.setItems(items);

                                    ipTextField.setDisable(true);
                                    portTextField.setDisable(true);
                                } else launchButton.setSelected(false);
                            } else launchButton.setSelected(false);
                        }
                    } catch (Throwable throwable) {
                        DateTimeFormatter formatter = DateTimeFormatter.ISO_TIME;
                        String time = LocalTime.now().format(formatter).substring(0, 8);

                        ObservableList<String> items = messagesList.getItems();
                        items.addAll(FXCollections.observableArrayList("[" + time + "] " +
                                textLabel +
                                " (" + ipTextField.getText() + ":" + portTextField.getText() + ") " +
                                "wasn't " + ((textLabel.charAt(0) == 'M') ? "connected" : "launched")));
                        messagesList.setItems(items);

                        launchButton.setSelected(false);
                    }
                } else {
                    DateTimeFormatter formatter = DateTimeFormatter.ISO_TIME;
                    String time = LocalTime.now().format(formatter).substring(0, 8);

                    ObservableList<String> items = messagesList.getItems();
                    items.addAll(FXCollections.observableArrayList("[" + time + "] " +
                            "Uncorrected IP Address or port" +
                            " (" + ipTextField.getText() + ":" + portTextField.getText() + ") " +
                            "for working of " + textLabel));
                    messagesList.setItems(items);

                    launchButton.setSelected(false);
                }
            }
            else {
                try {
                    if (textLabel.charAt(0) == 'M') {
                        if (server.getServerSocket().isClosed()) {
                            sql.stopConnection();

                            launchButton.setText((textLabel.charAt(0) == 'M') ? "Connect" : "Launch");

                            DateTimeFormatter formatter = DateTimeFormatter.ISO_TIME;
                            String time = LocalTime.now().format(formatter).substring(0, 8);

                            ObservableList<String> items = messagesList.getItems();
                            items.addAll(FXCollections.observableArrayList("[" + time + "] " +
                                    textLabel +
                                    " (" + ipTextField.getText() + ":" + portTextField.getText() + ") " +
                                    "was " + ((textLabel.charAt(0) == 'M') ? "disconnected" : "stopped")));
                            messagesList.setItems(items);

                            ipTextField.setDisable(false);
                            portTextField.setDisable(false);
                        }
                        else launchButton.setSelected(true);
                    }
                    else {
                        server.stopServer();
                        clearOnlineList();

                        launchButton.setText((textLabel.charAt(0) == 'M') ? "Connect" : "Launch");

                        DateTimeFormatter formatter = DateTimeFormatter.ISO_TIME;
                        String time = LocalTime.now().format(formatter).substring(0, 8);

                        ObservableList<String> items = messagesList.getItems();
                        items.addAll(FXCollections.observableArrayList("[" + time + "] " +
                                textLabel +
                                " (" + ipTextField.getText() + ":" + portTextField.getText() + ") " +
                                "was " + ((textLabel.charAt(0) == 'M') ? "disconnected" : "stopped")));
                        messagesList.setItems(items);

                        ipTextField.setDisable(false);
                        portTextField.setDisable(false);
                    }
                } catch (Throwable throwable) {
                    DateTimeFormatter formatter = DateTimeFormatter.ISO_TIME;
                    String time = LocalTime.now().format(formatter).substring(0, 8);

                    ObservableList<String> items = messagesList.getItems();
                    items.addAll(FXCollections.observableArrayList("[" + time + "] " +
                            textLabel +
                            " (" + ipTextField.getText() + ":" + portTextField.getText() + ") " +
                            "wasn't " + ((textLabel.charAt(0) == 'M') ? "disconnected" : "stopped")));
                    messagesList.setItems(items);

                    launchButton.setSelected(true);
                }
            }
        });

        return box;
    }

    private boolean validateIP(String ipAddress){

        Pattern pattern = Pattern.compile(IPADDRESS_PATTERN);
        Matcher matcher = pattern.matcher(ipAddress);

        return matcher.matches() || ipAddress.equals("localhost");
    }

    private boolean validatePort(String port) {

        try {
            int i = Integer.parseInt(port);
            if ((i >= 1025) && (i < 65536)) {
                this.port = i;
                return true;
            }
        } catch (Exception e) {
            return false;
        }

        return false;
    }

    public void connectInfo(String login, String id, boolean registration) {

        for (String i : onlineList.getItems())
            if (i.equals(login)) return;

        DateTimeFormatter formatter = DateTimeFormatter.ISO_TIME;
        String time = LocalTime.now().format(formatter).substring(0, 8);

        ObservableList<String> items = messagesList.getItems();

        items.addAll(FXCollections.observableArrayList("[" + time + "] " +
                login + " (ID: " + id +  ") " +
                (registration ? "registered on" : "connected to") + " PoolBets Server"));

        messagesList.setItems(items);

        items = onlineList.getItems();
        items.addAll(FXCollections.observableArrayList(login));
        onlineList.setItems(items);
    }

    public void disconnectInfo(String login, String id) {

        boolean flag = false;

        for (int i = 0; i < onlineList.getItems().size(); i++) {
            if (onlineList.getItems().get(i).equals(login)) break;
            if (i == onlineList.getItems().size() - 1) flag = true;
        }

        if (!flag) {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_TIME;
            String time = LocalTime.now().format(formatter).substring(0, 8);

            ObservableList<String> items = messagesList.getItems();

            items.addAll(FXCollections.observableArrayList("[" + time + "] " +
                    login + " (ID: " + id + ") disconnected from PoolBets Server"));
            messagesList.setItems(items);

            items = onlineList.getItems();
            items.remove(login);
            onlineList.setItems(items);
        }
    }

    private void clearOnlineList() {

        ObservableList<String> items = onlineList.getItems();
        items.remove(0, items.size());
        onlineList.setItems(items);
    }

    @Override
    public void start(Stage primaryStage) {

        onlineList = new ListView<>();
        messagesList = new ListView<>();

        HBox listsHBox = createHBoxLayout(createVBoxLayout(onlineList), createVBoxLayout(messagesList));

        HBox connectorHBox[] = {createHBoxLayout("MySQL Server"), createHBoxLayout("PoolBets Server")};

        VBox connectorsVBox = new VBox(connectorHBox[0], connectorHBox[1]);

        VBox root = new VBox();

        root.getChildren().addAll(listsHBox, connectorsVBox);
        root.setAlignment(Pos.CENTER);

        primaryStage.setTitle("PoolBets Server");
        primaryStage.setResizable(false);
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();

        primaryStage.setOnCloseRequest(event -> {
            if (sql != null)
                try {
                    if (!sql.getConnection().isClosed()) sql.stopConnection();
                } catch (SQLException ignored) {}

            if (server != null)
                try {
                    if (!server.getServerSocket().isClosed()) server.stopServer();
                } catch (IOException ignored) {}
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
