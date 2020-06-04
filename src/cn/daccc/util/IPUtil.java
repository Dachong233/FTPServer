package cn.daccc.util;

import java.net.*;
import java.util.Enumeration;
import java.util.Random;

public class IPUtil {
    private static InetAddress ip;
    static {
        try {
            ip = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
    private IPUtil (){ }

    public static String getIPAddress() {
        return getRealIP();
    }

    public static String getHostName() {
        return ip.getHostName();
    }

    public static String getRealIP() {
        try {
            Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = (NetworkInterface) allNetInterfaces
                        .nextElement();

                // 去除回环接口，子接口，未运行的接口
                if (netInterface.isLoopback() || netInterface.isVirtual()
                        || !netInterface.isUp()) {
                    continue;
                }

                if (!netInterface.getDisplayName().contains("Intel")
                        && !netInterface.getDisplayName().contains("Realtek")) {
                    continue;
                }
                Enumeration<InetAddress> addresses = netInterface
                        .getInetAddresses();
                System.out.println(netInterface.getDisplayName());
                while (addresses.hasMoreElements()) {
                    InetAddress ip = addresses.nextElement();
                    if (ip != null) {
                        // ipv4
                        if (ip instanceof Inet4Address) {
                            System.out.println("ipv4 = " + ip.getHostAddress());
                            return ip.getHostAddress();
                        }
                    }
                }
                break;
            }
        } catch (SocketException e) {
            System.err.println("Error when getting host ip address"
                    + e.getMessage());
        }
        return null;
    }

    public static int getRandomPort() {
        Random r = new Random(System.currentTimeMillis());
        return r.nextInt(65534 - 49152) + 49152;
    }

}
