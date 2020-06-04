package cn.daccc.model;

import cn.daccc.controller.Controller;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import static cn.daccc.model.FTPServer.Command.*;

public class MyServerHandler implements FTPServer.ServerHandler {
    private Controller controller;
    private FTPServer ftpServer;

    public MyServerHandler (Controller controller) {
        this.controller = controller;
    }

    public void setFtpServer (FTPServer ftpServer) {
        this.ftpServer = ftpServer;
    }

    @Override
    public void accept (SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        //获取客户端连接，并注册到Selector中
        SocketChannel clineChannel = serverSocketChannel.accept();
        clineChannel.configureBlocking(false);
        //将通道注册到selector，并设置为读操作
        clineChannel.register(key.selector(), SelectionKey.OP_READ, ByteBuffer.allocate(1024));
        controller.appendToScreen("<"
                + clineChannel.socket().getRemoteSocketAddress().toString().replace("/", "")
                + "> 已连接到服务器\n");
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        byteBuffer.put(FTPServer.Command.WELCOME.getBytes());
        byteBuffer.flip();
        clineChannel.write(byteBuffer);
    }

    @Override
    public void read (SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer byteBuffer = (ByteBuffer) key.attachment();
        byteBuffer.clear();
        long flag = -1;
        try {
            flag = clientChannel.read(byteBuffer);
        } catch (IOException e) {
            clientChannel.close();
            controller.appendToScreen("客户端已断开连接\n");
            return;
        }

        if (flag != -1) {
            //读取客户端发送的消息，并响应
            byteBuffer.flip();
            String receiveMsg = new String(byteBuffer.array(), 0, byteBuffer.limit());
            controller.appendToScreen("<"
                    + clientChannel.socket().getRemoteSocketAddress().toString().replace("/", "")
                    + ">:"
                    + receiveMsg);
            if (receiveMsg.lastIndexOf("\r\n") != -1) {
                receiveCommand(key, receiveMsg);
            }
        } else {
            //客户端的连接已经断开，关闭这个通道
            clientChannel.close();
            controller.appendToScreen("客户端已断开连接\n");
        }
    }

    public void receiveCommand (SelectionKey key, String command) throws IOException {
        switch (command) {
            case UTF8_REQ: {
                ftpServer.sendResponse(key, UTF8_RESP);
                break;
            }
            case SYST_REQ: {
                ftpServer.sendResponse(key, SYST_RESP);
                break;
            }
            case TYPE_REQ: {
                ftpServer.sendResponse(key, TYPE_RESP);
                break;
            }
            case PWD_REQ:{
                ftpServer.printWorkingDirectory(key);
                break;
            }
            case QUIT_REQ: {
                ftpServer.sendResponse(key, QUIT_RESP);
                break;
            }
            default: {
                String subCommand = command.substring(0, 4);
                switch (subCommand){
                    case USER_REQ: {
                        ftpServer.user(key, command.replace(subCommand + " ", "").replaceAll("[\r\n]", ""));
                        break;
                    }
                    case PASS_REQ: {
                        ftpServer.password(key, command.replace(subCommand + " ", "").replaceAll("[\r\n]", ""));
                        break;
                    }
                    case PASV_REQ: {
                        ftpServer.setPORTMode(false);
                        ftpServer.openDataSocketInPASV(key);
                        break;
                    }
                    case PORT_REQ:{
                        ftpServer.setPORTMode(true);
                        String[] clientInfo = command.replaceAll("[\r\n]", "").substring(5).split(",");
                        if (clientInfo.length < 6) {
                            ftpServer.sendResponse(key, PORT_RESP_FAIL);
                            return;
                        }
                        String clientIP = clientInfo[0] + "." + clientInfo[1] + "." + clientInfo[2] + "." + clientInfo[3];
                        int clientPort = Integer.parseInt(clientInfo[4]) * 256 + Integer.parseInt(clientInfo[5]);
                        ftpServer.openDataSocketInPORT(key, clientIP, clientPort);
                        break;
                    }
                    case LIST_REQ: {
                        if (command.length() >= "LIST \r\n".length()) {
                            subCommand = subCommand + " ";
                        }
                        String filePath = command.replaceAll("[\r\n]", "").replace(subCommand, "");
                        if (!"-l".equals(filePath)) {
                            ftpServer.sendResponseWithExtra(key, LIST_RESP_MODE, filePath);
                        } else {
                            ftpServer.sendResponseWithExtra(key, LIST_RESP_MODE, "\\");
                        }
                        break;
                    }
                    case GET_REQ: {
                        String filePath = command.replace(subCommand + " ", "").replace("\r\n", "");
                        ftpServer.fileDownload(key, filePath);
                        break;
                    } case PUT_REQ: {
                        String filePath = command.replace(subCommand + " ", "").replace("\r\n", "");
                        ftpServer.fileUpload(key, filePath);
                        break;
                    } case CWD_REQ: {
                        String directory = command.replace(subCommand, "").replace("\r\n", "");
                        ftpServer.changeWorkingDirectory(key, directory);
                        break;
                    } case SIZE_REQ: {
                        String filePath = command.replace(subCommand + " ", "").replace("\r\n", "");
                        ftpServer.fileSize(key, filePath);
                        break;
                    }
                    default:{
                        ftpServer.sendResponse(key, COMMAND_RESP_FAIL);
                        controller.appendToScreen("接收到未知命令:" + command.replaceAll("\r\n", "13,10\n"));
                    }
                }

            }
        }
    }


    @Override
    public void startSuccess () {
        controller.appendToScreen("服务器开启成功\n");
    }

    @Override
    public void stopSuccess () {
        controller.appendToScreen("服务器已关闭\n");
    }

    @Override
    public void fileProcess (double process) {
        controller.setProcess(process);
    }

    @Override
    public void outPrint (String msg) {
        controller.appendToScreen(msg + "\n");
    }


}
