package cn.daccc.controller;

import cn.daccc.util.TimeUtil;
import cn.daccc.model.FTPServer;
import cn.daccc.model.MyServerHandler;
import cn.daccc.util.IPUtil;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    public static Stage stage;
    public Pane pane;
    public TextArea textArea_serverScreen;
    public TextField textField_ipAddress;
    public TextField textField_port;
    public Button btn_startServer;

    private static final String TITLE = "FTPServer By zzc";
    private static boolean hasOpen = false;
    private static FTPServer ftpServer;
    private static StringBuffer serverScreen = new StringBuffer();
    private static double process = 0;
    public ProgressBar process_file;
    public TextField textField_serverRoot;
    public TextField textField_username;
    public TextField textField_password;
    public Button btn_openServerRoot;

    public TextField[] textFields;


    public Controller() { }

    public static void setStage (Stage aStage) {
        stage = aStage;
    }

    @Override
    public void initialize (URL location, ResourceBundle resources) {
        initView();
        new initThread().start();
    }

    public void initView() {
        textFields = new TextField[]{textField_ipAddress, textField_port,
                textField_username, textField_password};
    }

    public void setEditable (boolean clickable) {
        for (TextField textField : textFields) {
            textField.setEditable(clickable);
        }
    }

    /**
     * 控制服务器的开启与关闭
     */
    public void controlServer () {
        if (!hasOpen) {
            try {
                String ip = textField_ipAddress.getText();
                String port = textField_port.getText();
                if (ip.isEmpty() || port.isEmpty()) {
                    return;
                }
                clearScreen();
                MyServerHandler handler = new MyServerHandler(this);
                ftpServer = new FTPServer(ip, Integer.parseInt(port), handler);
                ftpServer.setUser(textField_username.getText(), textField_password.getText());
                ftpServer.start();
                handler.setFtpServer(ftpServer);
                hasOpen = true;
                btn_startServer.setText("关闭服务器");
                stage.setTitle(TITLE + " ——ip:" + ip + "    port:" + port + "——已开启");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            try {
                ftpServer.stop();
                btn_startServer.setText("开启服务器");
                stage.setTitle(TITLE);
                hasOpen = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        setEditable(!hasOpen);
    }


    public void appendToScreen (String text) {
        System.out.println(text);
        serverScreen.append(TimeUtil.getTime()).append(" ").append(text);
        textArea_serverScreen.setText(serverScreen.toString());
        textArea_serverScreen.positionCaret(textArea_serverScreen.getLength());
    }

    public void clearScreen() {
        serverScreen.delete(0, serverScreen.length());
    }

    public void setProcess (double aProcess) {
        process = aProcess;
        process_file.setProgress(aProcess);
    }

    public void openServerRoot () {
        try {
            Desktop.getDesktop().open(new File(textField_serverRoot.getText()));
        } catch (IOException e) {
            File serverRoot = new File("serverRoot");
            if (!serverRoot.exists()) {
                if (serverRoot.mkdir()) {
                    appendToScreen("创建FTP服务器根目录成功\n");
                } else {
                    appendToScreen("创建FTP服务器根目录失败\n");
                }
            }
            textField_serverRoot.setText(serverRoot.getAbsolutePath());
        }
    }

    private class initThread extends Thread{
        @Override
        public void run () {
            textField_ipAddress.setText(IPUtil.getIPAddress());
            File serverRoot = new File("serverRoot");
            if (!serverRoot.exists()) {
                if (serverRoot.mkdir()) {
                    appendToScreen("创建FTP服务器根目录成功\n");
                } else {
                    appendToScreen("创建FTP服务器根目录失败\n");
                }
            }
            textField_serverRoot.setText(serverRoot.getAbsolutePath());
        }
    }
}
