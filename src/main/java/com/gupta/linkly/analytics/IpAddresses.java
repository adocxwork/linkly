package com.gupta.linkly.analytics;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

/** Validation and classification of IP address strings (no DNS lookups). */
public final class IpAddresses {

    private static final Pattern IPV4 = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$");
    private static final Pattern IPV6 = Pattern.compile("^[0-9a-fA-F:]{2,39}$");

    private IpAddresses() {
    }

    /** Accept address literals only. No hostnames are resolved. */
    public static boolean isValid(String ip) {
        return normalize(ip) != null;
    }

    static String normalize(String ip) {
        if (ip == null) return null;
        if (IPV4.matcher(ip).matches()) return ip;
        if (!ip.contains(":") || !IPV6.matcher(ip).matches()) return null;
        try {
            return InetAddress.getByName(ip).getHostAddress();
        } catch (UnknownHostException e) {
            return null;
        }
    }

    /** Loopback, private and link-local ranges have no meaningful geo location. */
    public static boolean isPrivateOrLocal(String ip) {
        ip = normalize(ip);
        if (ip == null) {
            return true;
        }
        if (ip.contains(":")) {
            int firstGroup = Integer.parseInt(ip.substring(0, ip.indexOf(':')), 16);
            return ip.startsWith("0:0:0:0:0:0:0:") || (firstGroup & 0xffc0) == 0xfe80
                    || (firstGroup & 0xfe00) == 0xfc00 || (firstGroup & 0xff00) == 0xff00;
        }
        String[] p = ip.split("\\.");
        int a = Integer.parseInt(p[0]);
        int b = Integer.parseInt(p[1]);
        return a == 10 || a == 127 || a == 0 || a >= 224
                || (a == 172 && b >= 16 && b <= 31)
                || (a == 192 && b == 168)
                || (a == 169 && b == 254)
                || (a == 100 && b >= 64 && b <= 127);
    }
}
