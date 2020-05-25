package cn.daccc.model;

import cn.daccc.controller.Controller;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class MyDataHandler implements FTPServer.DataHandler {
    private Controller controller;

    public MyDataHandler (Controller controller) {
        this.controller = controller;
    }

    @Override
    public void read (SelectionKey key) throws IOException {
        SocketChannel dataChannel = ((SocketChannel) key.channel());
        System.out.println("DataChannel-Read");
    }

    @Override
    public void startSuccess () {
        controller.appendToScreen("20端口已开启\n");
    }

    @Override
    public void stopSuccess () {
        controller.appendToScreen("20端口已关闭\n");
    }

    @Override
    public void outPrint (String msg) {
        controller.appendToScreen("port 20:" + msg);
    }
}
