package cn.daccc.model;

import cn.daccc.util.TimeUtil;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

public class FTPServer {
    private String ip;
    private int port;
    private String serverRoot = "serverRoot";
    private String currentWorkingDirectory = "/";
    private String uploadFile = "";
    /* 服务器 21控制端口*/
    private static ServerHandler serverHandler;
    private static Selector serverSelector;
    private static ServerThread serverThread;
    private static ServerSocketChannel serverSocketChannel;

    /*服务器 20数据端口*/
    private static DataHandler dataHandler;
    private static Selector dataSelector;
    private static DataChannelSelectorThread dataThread;
    private static SocketChannel dataSocketChannel;
    private boolean hasFinishUpload;
    private String clientIP;
    private int clientPort;

    public static class Command {
        public static final String WELCOME = "220 Welcome to FTPServer. \r\n";
        public static final String UTF8_REQ = "OPTS UTF8 ON\r\n";
        public static final String UTF8_RESP = "200 OPTS UTF8 is set to ON.\r\n";
        public static final String USER_REQ = "USER \r\n";
        public static final String USER_RESP = "331 User name okay，need password. \r\n";
        public static final String PASS_REQ = "PASS \r\n";
        public static final String PASS_RESP = "230 User logged in.proceed. \r\n";
        public static final String PORT_REQ = "PORT";
        public static final String PORT_RESP_SUCCESS = "200 PORT command successful. \r\n";
        public static final String PORT_RESP_FAIL = "425 Can not open data connection. \r\n";
        public static final String LIST_REQ = "LIST";
        public static final String LIST_RESP_MODE = "150 Opening ASCII mode data connection. \r\n";
        public static final String GET_REQ = "RETR";
        public static final String GET_RESP_FAIL = "550 It's a directory. \r\n";
        public static final String PUT_REQ = "STOR";
        public static final String FILE_RESP_MODE = "150 Opening Binary mode data connection. \r\n";
        public static final String FILE_RESP_FAIL = "550 No such file or directory. \r\n";

        /* LIST GET PUT共用的响应 */
        public static final String TRANSFER_RESP = "226 Transfer complete. \r\n";

        public static final String QUIT_REQ = "QUIT\r\n";
        public static final String QUIT_RESP = "221 GoodBye, closing session. \r\n";
        public static final String COMMAND_RESP_FAIL = "202 Command not implemented,superfluous at this site. \r\n";
    }

    public interface ServerHandler {
        void accept(SelectionKey key) throws IOException;
        void read(SelectionKey key) throws IOException;
        void startSuccess();
        void stopSuccess();
        void fileProcess (double process);
        void outPrint (String msg);
    }

    public class DataHandler {
        private SelectionKey serverKey;

        public void setServerKey (SelectionKey serverKey) {
            this.serverKey = serverKey;
        }

        public void read (SelectionKey key) throws IOException {
            serverHandler.outPrint("客户端正在上传文件:" + uploadFile);
            SocketChannel socketChannel = ((SocketChannel) key.channel());
            File file = new File(uploadFile);
            FileChannel fileChannel = new FileOutputStream(file).getChannel();
            ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
            long totalLen = 0;
            int offset;
            while ((offset = socketChannel.read(byteBuffer)) != -1) {
                byteBuffer.flip();
                fileChannel.write(byteBuffer);
                byteBuffer.clear();
                totalLen += offset;
            }
            fileChannel.close();
            hasFinishUpload = true;
            sendResponse(serverKey, Command.TRANSFER_RESP);
            serverHandler.outPrint("文件传输完成！总大小:" + totalLen + "B≈" + totalLen / 1024 + "KB≈" + totalLen / 1024 / 1024 + "MB");
        }
    }

    public FTPServer (String ip, int port, ServerHandler serverHandler) {
        this.ip = ip;
        this.port = port;
        this.serverHandler = serverHandler;
    }

    public void sendData (SocketChannel channel, String command) throws IOException {
        System.out.println("发送:" + command);
        byte[] bytes = command.getBytes();
        ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
        byteBuffer.put(bytes);
        byteBuffer.flip();
        channel.write(byteBuffer);
    }

    public void sendResponse (SelectionKey key, String resp) throws IOException {
        SocketChannel channel = (SocketChannel) (key.channel());
        ByteBuffer byteBuffer = (ByteBuffer) key.attachment();
        byteBuffer.clear();
        byteBuffer.put(resp.getBytes());
        byteBuffer.flip();
        channel.write(byteBuffer);
        switch (resp) {
            case Command.QUIT_RESP:{
                channel.socket().close();
                channel.close();
                key.cancel();
                break;
            }
        }
    }

    public void sendResponseWithExtra (SelectionKey key, String resp, String extraInfo) throws IOException {
        SocketChannel channel = (SocketChannel) (key.channel());
        ByteBuffer byteBuffer = (ByteBuffer) key.attachment();
        byteBuffer.clear();
        byteBuffer.put(resp.getBytes());
        byteBuffer.flip();
        channel.write(byteBuffer);
        switch (resp) {
            case Command.LIST_RESP_MODE: {
                connectDataSocket();
                String fileList = queryFileList(extraInfo);
                if (!"".equals(fileList)) {
                    sendData(dataSocketChannel, fileList);
                    closeDataSocket();
                    sendResponse(key, Command.TRANSFER_RESP);
                }else{
                    closeDataSocket();
                    sendResponse(key, Command.FILE_RESP_FAIL);
                }
                break;
            }

        }
    }

    public void start () throws IOException {
        if (serverThread == null) {
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(ip, port));
            serverSocketChannel.configureBlocking(false);
            serverSelector = Selector.open();
            serverSocketChannel.register(serverSelector, SelectionKey.OP_ACCEPT);
            serverThread = new ServerThread();
            serverThread.start();
            serverHandler.startSuccess();
        }
    }

    public void stop() throws IOException {
        if (serverThread != null) {
            serverThread.interrupt();
            serverThread = null;
            for (SelectionKey key : serverSelector.keys()) {
                key.channel().close();
                key.cancel();
            }
            serverSelector.close();
            serverSocketChannel.close();
            serverHandler.stopSuccess();
        }
    }

    /**
     * 开启20端口
     */
    public void openDataSocket (SelectionKey key, String clientIP, int clientPort) {
        try {
            dataSocketChannel = SocketChannel.open();
            dataSocketChannel.bind(new InetSocketAddress(ip, 20));
            this.clientIP = clientIP;
            this.clientPort = clientPort;
            sendResponse(key, Command.PORT_RESP_SUCCESS);
        } catch (IOException e) {
            try {
                sendResponse(key, Command.PORT_RESP_FAIL);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    /**
     * 主动连接客户端指定端口
     */
    public void connectDataSocket () {
        try {
            dataSocketChannel.connect(new InetSocketAddress(clientIP, clientPort));
            dataSocketChannel.configureBlocking(false);
            dataSelector = Selector.open();
            dataSocketChannel.register(dataSelector, SelectionKey.OP_READ);
            dataThread = new DataChannelSelectorThread();
            dataThread.start();
            dataHandler = new DataHandler();
        } catch (IOException e) {
            serverHandler.outPrint(e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * 关闭20端口
     */
    public void closeDataSocket () {
        try {
            if (dataThread != null) {
                dataThread.interrupt();
                dataThread = null;
            }
            dataSocketChannel.socket().close();
            dataSocketChannel.close();
            dataSelector.close();
        } catch (IOException e) {
            serverHandler.outPrint(e.getMessage());
            e.printStackTrace();
        }
    }

    public String queryFileList (String directory) {
        File serverRoot = new File(this.serverRoot);
        File filePath = new File(this.serverRoot + currentWorkingDirectory + directory);
        if (!serverRoot.exists()) {
            if (serverRoot.mkdir()) {
                serverHandler.outPrint("创建FTP服务器根目录成功");
            } else {
                serverHandler.outPrint("创建FTP服务器根目录失败");
                return "";
            }
        }
        //-rw-rw-rw-   1 user     group           0 May 21 12:01 hello.txt\r\n
        //drwxrwxrwx   1 user     group           0 May 21 12:02 directory\r\n
        StringBuilder fileListString = new StringBuilder();
        File[] files = filePath.listFiles();
        if (files == null) {
            files = new File[]{filePath};
        }
        for (File file : files) {
            StringBuilder fileInfo = new StringBuilder(file.isFile() ? "-" : "d");
            //获取文件权限名称
            StringBuilder authority = new StringBuilder();
            authority.append(file.canRead() ? "r" : "-")
                    .append(file.canWrite() ? "w" : "-")
                    .append(file.canExecute() ? "-" : "-");
            fileInfo.append(authority).append(authority).append(authority).append("   ");
            fileInfo.append("1 user     group ");
            long fileLen = file.length();
            for (int i = 0; i < 11 - String.valueOf(fileLen).length(); i++) {
                fileInfo.append(" ");
            }
            fileInfo.append(fileLen).append(" ");
            fileInfo.append(TimeUtil.getLastModifiedTime(file.lastModified())).append(" ");
            fileInfo.append(file.getName()).append("\r\n");
            fileListString.append(fileInfo.toString());
        }
        return fileListString.toString();
    }

    public void fileUpload (SelectionKey key, String filePath) throws IOException {
        connectDataSocket();
        uploadFile = serverRoot + currentWorkingDirectory + filePath;
        System.out.println("上传:" + serverRoot + currentWorkingDirectory + filePath);
        dataHandler.setServerKey(key);
        sendResponse(key, Command.FILE_RESP_MODE);
    }

    public void fileDownload (SelectionKey key, String filePath) throws IOException {
        connectDataSocket();
        File file = new File(serverRoot + currentWorkingDirectory + filePath);
        System.out.println("下载:" + serverRoot + currentWorkingDirectory + filePath);
        if (file.isDirectory()) {
            sendResponse(key, Command.GET_RESP_FAIL);
            closeDataSocket();
            return;
        }
        if (!file.exists()) {
            sendResponse(key, Command.FILE_RESP_FAIL);
            closeDataSocket();
            return;
        }
        sendResponse(key, Command.FILE_RESP_MODE);
        new FileDownloadThread(key, file).start();
    }

    /**
     * 文件下载线程，将服务器文件传输到客户端
     */
    private class FileDownloadThread extends Thread{
        private SelectionKey key;
        private File file;

        public FileDownloadThread (SelectionKey key, File file) {
            this.key = key;
            this.file = file;
        }

        @Override
        public void run () {
            try {
                long totalLen = file.length(), currentLen = 0;
                int offset;
                int count = 1;
                FileChannel fileChannel = new FileInputStream(file).getChannel();
                ByteBuffer byteBuffer = ByteBuffer.allocate(1460);
                serverHandler.outPrint("客户端正在下载文件，总大小:" + totalLen + "B≈" + totalLen / 1024 + "KB≈" + totalLen / 1024 / 1024 + "MB");
                while ((offset = fileChannel.read(byteBuffer)) != -1) {
                    byteBuffer.flip();
                    dataSocketChannel.write(byteBuffer);
                    byteBuffer.clear();
                    currentLen += offset;
                    serverHandler.fileProcess(currentLen / (1.0 * totalLen));
                    if ((count = count << 2) > 32767) {
                        //若不休眠，一次性发送过多数据容易造成流量控制，从而丢包
                        count = 1;
                        Thread.sleep(4);
                    }
                }
                fileChannel.close();
                closeDataSocket();
                sendResponse(key, Command.TRANSFER_RESP);
                serverHandler.outPrint("文件传输完成!");
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class DataChannelSelectorThread extends Thread{
        @Override
        public void run () {
            while (!isInterrupted()) {
                if (hasFinishUpload) {
                    closeDataSocket();
                    hasFinishUpload = false;
                    break;
                }
                try {
                    int selectCount = dataSelector.select();
                    if (selectCount <= 0) {
                        continue;
                    }
                    Iterator<SelectionKey> iterator = dataSelector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        try {
                            SelectionKey key = iterator.next();
                            if (key.isReadable()) {
                                dataHandler.read(key);
                            }
                            iterator.remove();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }



    private class ServerThread extends Thread {
        @Override
        public void run () {
            while (!isInterrupted()) {
                try {
                    int selectCount = serverSelector.select();
                    if (selectCount <= 0) {
                        continue;
                    }
                    Iterator<SelectionKey> iterator = serverSelector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        try {
                            SelectionKey key = iterator.next();
                            if (key.isAcceptable()) {
                                serverHandler.accept(key);
                            }
                            if (key.isReadable()) {
                                serverHandler.read(key);
                            }
                            iterator.remove();
                        } catch (IOException e) {
                            serverHandler.outPrint(e.getMessage());
                            e.printStackTrace();
                        }

                    }
                } catch (IOException e) {
                    serverHandler.outPrint(e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

}
