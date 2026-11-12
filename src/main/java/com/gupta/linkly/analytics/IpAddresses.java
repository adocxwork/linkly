package com.gupta.linkly.analytics;

import java.util.Locale;
import java.util.regex.Pattern;

/** Validation and classification of IP address strings (no DNS lookups). */
public final class IpAddresses {

    private static final Pattern IPV4 = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$");
    private static final Pattern IPV6 = Pattern.compile("^[0-9a-fA-F:]{2,39}$");

    private IpAddresses() {
    }

    /** Header values are attacker-controlled; only accept strings that look like an IP. */
    public static boolean isValid(String ip) {
        return ip != null && (IPV4.matcher(ip).matches() || (ip.contains(":") && IPV6.matcher(ip).matches()));
    }

    /** Loopback, private and link-local ranges have no meaningful geo location. */
    public static boolean isPrivateOrLocal(String ip) {
        if (!isValid(ip)) {
            return true;
        }
        if (ip.contains(":")) {
            String v6 = ip.toLowerCase(Locale.ROOT);
            return v6.equals("::1") || v6.startsWith("fe80") || v6.startsWith("fc") || v6.startsWith("fd");
        }
        String[] p = ip.split("\\.");
        int a = Integer.parseInt(p[0]);
        int b = Integer.parseInt(p[1]);
        return a == 10 || a == 127 || a == 0
                || (a == 172 && b >= 16 && b <= 31)
                || (a == 192 && b == 168)
                || (a == 169 && b == 254)
                || (a == 100 && b >= 64 && b <= 127);
    }
}
