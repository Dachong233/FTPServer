package cn.daccc.controller;

import cn.daccc.util.TimeUtil;
import cn.daccc.model.FTPServer;
import cn.daccc.model.MyServerHandler;
import cn.daccc.util.IPUtil;
import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

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

    public Controller() { }

    public static void setStage (Stage aStage) {
        stage = aStage;
    }

    @Override
    public void initialize (URL location, ResourceBundle resources) {
        initView();
//        new ScreenRefreshThread().start();
    }

    public void initView() {
        textField_ipAddress.setText(IPUtil.getIPAddress());
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

    private class ScreenRefreshThread extends Thread{
        @Override
        public void run () {
            while (!isInterrupted()) {
                try {
                    Thread.sleep(5000);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
