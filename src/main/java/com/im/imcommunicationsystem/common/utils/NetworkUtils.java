package com.im.imcommunicationsystem.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.net.*;
import java.util.Enumeration;
import java.util.regex.Pattern;

/**
 * 网络工具类
 * 提供IP地址处理、HTTP请求等网络相关功能
 * 
 * @author IM Team
 * @version 1.0
 * @since 2024-01-01
 */
@Slf4j
public class NetworkUtils {

    /**
     * IP地址正则表达式
     */
    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
    
    private static final Pattern IPV6_PATTERN = Pattern.compile(
            "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$");

    /**
     * 私有IP地址范围
     */
    private static final String[] PRIVATE_IP_PREFIXES = {
            "10.",
            "172.16.", "172.17.", "172.18.", "172.19.", "172.20.", "172.21.", "172.22.", "172.23.",
            "172.24.", "172.25.", "172.26.", "172.27.", "172.28.", "172.29.", "172.30.", "172.31.",
            "192.168.",
            "127.",
            "169.254."
    };

    /**
     * 代理服务器IP头信息
     */
    private static final String[] IP_HEADER_NAMES = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR",
            "X-Cluster-Client-IP"
    };

    /**
     * 获取客户端真实IP地址
     * 
     * @param request HTTP请求对象
     * @return 客户端IP地址
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String ip = null;
        
        // 尝试从各种代理头中获取IP
        for (String header : IP_HEADER_NAMES) {
            ip = request.getHeader(header);
            if (isValidIp(ip)) {
                break;
            }
        }
        
        // 如果代理头中没有有效IP，则从远程地址获取
        if (!isValidIp(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // 处理多个IP的情况（X-Forwarded-For可能包含多个IP）
        if (StringUtils.hasText(ip) && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        // IPv6本地回环地址转换为IPv4
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }
        
        return StringUtils.hasText(ip) ? ip : "unknown";
    }

    /**
     * 获取当前请求的客户端IP地址
     * 
     * @return 客户端IP地址
     */
    public static String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = 
                    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            return getClientIpAddress(request);
        } catch (Exception e) {
            log.warn("获取客户端IP地址失败", e);
            return "unknown";
        }
    }

    /**
     * 获取客户端IP地址（别名方法）
     * 
     * @param request HTTP请求对象
     * @return 客户端IP地址
     */
    public static String getClientIp(HttpServletRequest request) {
        return getClientIpAddress(request);
    }

    /**
     * 验证IP地址是否有效
     * 
     * @param ip IP地址字符串
     * @return 是否有效
     */
    private static boolean isValidIp(String ip) {
        return StringUtils.hasText(ip) && 
               !"unknown".equalsIgnoreCase(ip) && 
               !"null".equalsIgnoreCase(ip) &&
               (isValidIPv4(ip) || isValidIPv6(ip));
    }

    /**
     * 验证IPv4地址格式
     * 
     * @param ip IP地址
     * @return 是否为有效的IPv4地址
     */
    public static boolean isValidIPv4(String ip) {
        if (!StringUtils.hasText(ip)) {
            return false;
        }
        return IPV4_PATTERN.matcher(ip.trim()).matches();
    }

    /**
     * 验证IPv6地址格式
     * 
     * @param ip IP地址
     * @return 是否为有效的IPv6地址
     */
    public static boolean isValidIPv6(String ip) {
        if (!StringUtils.hasText(ip)) {
            return false;
        }
        return IPV6_PATTERN.matcher(ip.trim()).matches();
    }

    /**
     * 判断是否为私有IP地址
     * 
     * @param ip IP地址
     * @return 是否为私有IP
     */
    public static boolean isPrivateIP(String ip) {
        if (!isValidIPv4(ip)) {
            return false;
        }
        
        for (String prefix : PRIVATE_IP_PREFIXES) {
            if (ip.startsWith(prefix)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 判断是否为公网IP地址
     * 
     * @param ip IP地址
     * @return 是否为公网IP
     */
    public static boolean isPublicIP(String ip) {
        return isValidIPv4(ip) && !isPrivateIP(ip);
    }

    /**
     * 判断是否为本地回环地址
     * 
     * @param ip IP地址
     * @return 是否为本地回环地址
     */
    public static boolean isLoopbackIP(String ip) {
        if (!StringUtils.hasText(ip)) {
            return false;
        }
        return ip.startsWith("127.") || "::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip);
    }

    /**
     * 获取本机IP地址
     * 
     * @return 本机IP地址
     */
    public static String getLocalIPAddress() {
        try {
            // 优先获取非回环地址
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                
                // 跳过回环接口和未启用的接口
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    
                    // 只处理IPv4地址，跳过回环地址和链路本地地址
                    if (address instanceof Inet4Address && 
                        !address.isLoopbackAddress() && 
                        !address.isLinkLocalAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
            
            // 如果没有找到合适的地址，返回本地回环地址
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            log.warn("获取本机IP地址失败", e);
            return "127.0.0.1";
        }
    }

    /**
     * 获取本机主机名
     * 
     * @return 主机名
     */
    public static String getLocalHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            log.warn("获取主机名失败", e);
            return "unknown";
        }
    }

    /**
     * 获取MAC地址
     * 
     * @return MAC地址
     */
    public static String getMacAddress() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(localHost);
            
            if (networkInterface != null) {
                byte[] macBytes = networkInterface.getHardwareAddress();
                if (macBytes != null) {
                    StringBuilder macAddress = new StringBuilder();
                    for (int i = 0; i < macBytes.length; i++) {
                        macAddress.append(String.format("%02X", macBytes[i]));
                        if (i < macBytes.length - 1) {
                            macAddress.append(":");
                        }
                    }
                    return macAddress.toString();
                }
            }
        } catch (Exception e) {
            log.warn("获取MAC地址失败", e);
        }
        return "unknown";
    }

    /**
     * 检查端口是否可用
     * 
     * @param port 端口号
     * @return 是否可用
     */
    public static boolean isPortAvailable(int port) {
        if (port < 1 || port > 65535) {
            return false;
        }
        
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查主机端口是否可达
     * 
     * @param host 主机地址
     * @param port 端口号
     * @param timeout 超时时间（毫秒）
     * @return 是否可达
     */
    public static boolean isHostReachable(String host, int port, int timeout) {
        if (!StringUtils.hasText(host) || port < 1 || port > 65535 || timeout < 0) {
            return false;
        }
        
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            return true;
        } catch (Exception e) {
            log.debug("主机{}:{}不可达", host, port);
            return false;
        }
    }

    /**
     * 检查主机是否可达（使用ping）
     * 
     * @param host 主机地址
     * @param timeout 超时时间（毫秒）
     * @return 是否可达
     */
    public static boolean pingHost(String host, int timeout) {
        if (!StringUtils.hasText(host) || timeout < 0) {
            return false;
        }
        
        try {
            InetAddress address = InetAddress.getByName(host);
            return address.isReachable(timeout);
        } catch (Exception e) {
            log.debug("主机{}不可达", host);
            return false;
        }
    }

    /**
     * 获取用户代理信息
     * 
     * @param request HTTP请求对象
     * @return 用户代理字符串
     */
    public static String getUserAgent(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String userAgent = request.getHeader("User-Agent");
        return StringUtils.hasText(userAgent) ? userAgent : "unknown";
    }

    /**
     * 获取当前请求的用户代理信息
     * 
     * @return 用户代理字符串
     */
    public static String getUserAgent() {
        try {
            ServletRequestAttributes attributes = 
                    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            return getUserAgent(request);
        } catch (Exception e) {
            log.warn("获取用户代理信息失败", e);
            return "unknown";
        }
    }

    /**
     * 获取请求来源URL
     * 
     * @param request HTTP请求对象
     * @return 来源URL
     */
    public static String getReferer(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String referer = request.getHeader("Referer");
        return StringUtils.hasText(referer) ? referer : "";
    }

    /**
     * 获取请求的完整URL
     * 
     * @param request HTTP请求对象
     * @return 完整URL
     */
    public static String getFullRequestUrl(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        
        StringBuilder url = new StringBuilder();
        url.append(request.getScheme()).append("://")
           .append(request.getServerName());
        
        int port = request.getServerPort();
        if (port != 80 && port != 443) {
            url.append(":").append(port);
        }
        
        url.append(request.getRequestURI());
        
        String queryString = request.getQueryString();
        if (StringUtils.hasText(queryString)) {
            url.append("?").append(queryString);
        }
        
        return url.toString();
    }

    /**
     * 判断是否为AJAX请求
     * 
     * @param request HTTP请求对象
     * @return 是否为AJAX请求
     */
    public static boolean isAjaxRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        
        String requestedWith = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equals(requestedWith);
    }

    /**
     * 判断是否为移动设备
     * 
     * @param userAgent 用户代理字符串
     * @return 是否为移动设备
     */
    public static boolean isMobileDevice(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return false;
        }
        
        String ua = userAgent.toLowerCase();
        String[] mobileKeywords = {
            "mobile", "android", "iphone", "ipad", "ipod", "blackberry", 
            "windows phone", "opera mini", "opera mobi", "palm", "symbian"
        };
        
        for (String keyword : mobileKeywords) {
            if (ua.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 判断当前请求是否来自移动设备
     * 
     * @return 是否为移动设备
     */
    public static boolean isMobileDevice() {
        return isMobileDevice(getUserAgent());
    }

    /**
     * 获取浏览器类型
     * 
     * @param userAgent 用户代理字符串
     * @return 浏览器类型
     */
    public static String getBrowserType(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return "unknown";
        }
        
        String ua = userAgent.toLowerCase();
        
        if (ua.contains("edge")) {
            return "Edge";
        } else if (ua.contains("chrome")) {
            return "Chrome";
        } else if (ua.contains("firefox")) {
            return "Firefox";
        } else if (ua.contains("safari")) {
            return "Safari";
        } else if (ua.contains("opera")) {
            return "Opera";
        } else if (ua.contains("msie") || ua.contains("trident")) {
            return "Internet Explorer";
        } else {
            return "unknown";
        }
    }

    /**
     * 获取操作系统类型
     * 
     * @param userAgent 用户代理字符串
     * @return 操作系统类型
     */
    public static String getOperatingSystem(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return "unknown";
        }
        
        String ua = userAgent.toLowerCase();
        
        if (ua.contains("windows")) {
            return "Windows";
        } else if (ua.contains("mac os")) {
            return "macOS";
        } else if (ua.contains("linux")) {
            return "Linux";
        } else if (ua.contains("android")) {
            return "Android";
        } else if (ua.contains("iphone") || ua.contains("ipad")) {
            return "iOS";
        } else {
            return "unknown";
        }
    }

    /**
     * IP地址转换为长整型
     * 
     * @param ip IP地址
     * @return 长整型值
     */
    public static long ipToLong(String ip) {
        if (!isValidIPv4(ip)) {
            return 0;
        }
        
        String[] parts = ip.split("\\.");
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result = result * 256 + Integer.parseInt(parts[i]);
        }
        return result;
    }

    /**
     * 长整型转换为IP地址
     * 
     * @param longIp 长整型IP
     * @return IP地址字符串
     */
    public static String longToIp(long longIp) {
        return ((longIp >> 24) & 0xFF) + "." +
               ((longIp >> 16) & 0xFF) + "." +
               ((longIp >> 8) & 0xFF) + "." +
               (longIp & 0xFF);
    }

    /**
     * 判断IP是否在指定网段内
     * 
     * @param ip 待检查的IP
     * @param cidr CIDR格式的网段（如：192.168.1.0/24）
     * @return 是否在网段内
     */
    public static boolean isIpInCidr(String ip, String cidr) {
        if (!isValidIPv4(ip) || !StringUtils.hasText(cidr)) {
            return false;
        }
        
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) {
                return false;
            }
            
            String networkIp = parts[0];
            int prefixLength = Integer.parseInt(parts[1]);
            
            if (!isValidIPv4(networkIp) || prefixLength < 0 || prefixLength > 32) {
                return false;
            }
            
            long ipLong = ipToLong(ip);
            long networkLong = ipToLong(networkIp);
            long mask = (0xFFFFFFFFL << (32 - prefixLength)) & 0xFFFFFFFFL;
            
            return (ipLong & mask) == (networkLong & mask);
        } catch (Exception e) {
            log.warn("检查IP是否在网段内失败: ip={}, cidr={}", ip, cidr, e);
            return false;
        }
    }

    /**
     * 网络工具使用说明：
     * 
     * 1. IP地址处理：
     *    - getClientIpAddress() - 获取客户端真实IP（处理代理）
     *    - isValidIPv4() / isValidIPv6() - 验证IP地址格式
     *    - isPrivateIP() / isPublicIP() - 判断IP类型
     *    - getLocalIPAddress() - 获取本机IP
     * 
     * 2. 网络连接：
     *    - isPortAvailable() - 检查端口可用性
     *    - isHostReachable() - 检查主机端口可达性
     *    - pingHost() - ping主机
     * 
     * 3. HTTP请求信息：
     *    - getUserAgent() - 获取用户代理
     *    - getReferer() - 获取来源URL
     *    - getFullRequestUrl() - 获取完整请求URL
     *    - isAjaxRequest() - 判断是否为AJAX请求
     * 
     * 4. 设备识别：
     *    - isMobileDevice() - 判断是否为移动设备
     *    - getBrowserType() - 获取浏览器类型
     *    - getOperatingSystem() - 获取操作系统类型
     * 
     * 5. IP地址计算：
     *    - ipToLong() / longToIp() - IP地址与长整型互转
     *    - isIpInCidr() - 判断IP是否在网段内
     * 
     * 使用示例：
     * 
     * // 获取客户端IP
     * String clientIp = NetworkUtils.getClientIpAddress(request);
     * 
     * // 判断是否为移动设备
     * if (NetworkUtils.isMobileDevice()) {
     *     // 返回移动端页面
     * }
     * 
     * // 检查服务可用性
     * if (NetworkUtils.isHostReachable("localhost", 8080, 3000)) {
     *     // 服务可用
     * }
     * 
     * // IP白名单检查
     * if (NetworkUtils.isIpInCidr(clientIp, "192.168.1.0/24")) {
     *     // 允许访问
     * }
     * 
     * 注意事项：
     * 1. 代理环境：getClientIpAddress()会尝试从多个代理头获取真实IP
     * 2. 安全考虑：IP地址可能被伪造，不应作为唯一的安全验证
     * 3. 性能影响：网络连接检查可能较慢，建议异步执行
     * 4. 异常处理：网络操作可能失败，已包含适当的异常处理
     * 5. 线程安全：所有方法都是静态的，线程安全
     */

}