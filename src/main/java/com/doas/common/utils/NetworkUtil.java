package com.doas.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 获取局域网地址
 */
@Slf4j
public class NetworkUtil {
    public static Map<String,String> getInet4Address() {
        Map<String,String> addressMap = new HashMap<>();
        // 获得本机的所有网络接口
        Enumeration<NetworkInterface> nifs = null;
        try {
            nifs = NetworkInterface.getNetworkInterfaces();
            while (nifs.hasMoreElements()) {
                NetworkInterface nif = nifs.nextElement();

                // 获得与该网络接口绑定的 IP 地址，一般只有一个
                Enumeration<InetAddress> addresses = nif.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address) { // 只关心 IPv4 地址
                        if(!"127.0.0.1".equals(addr.getHostAddress())) {
                            addressMap.put(nif.getName(), addr.getHostAddress());
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
            log.error("获取局域网地址异常！");
        }
        return addressMap;
    }
}
