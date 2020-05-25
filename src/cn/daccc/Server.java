package cn.daccc;

import cn.daccc.controller.Controller;
import cn.daccc.model.FTPServer;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class Server extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("view/view_server.fxml"));
        primaryStage.setTitle("FTPServer By zzc");
        primaryStage.setScene(new Scene(root, 600, 450));
        primaryStage.show();
        Controller.setStage(primaryStage);
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle (WindowEvent event) {
                System.exit(0);
            }
        });
        //FTPServer.queryFileList(null);
    }


    public static void main(String[] args) {
        launch(args);
    }
}
