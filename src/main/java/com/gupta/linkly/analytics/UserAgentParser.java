package com.gupta.linkly.analytics;

import java.util.Locale;

/** Deliberately small User-Agent classifier. Order of checks matters (see comments). */
public final class UserAgentParser {

    private UserAgentParser() {
    }

    /**
     * Crawlers and link-preview fetchers (WhatsApp, Slack, Twitter...) request a link when it is
     * shared, not when a person clicks it. Counting them would inflate analytics.
     */
    public static boolean isBot(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return true;
        }
        String ua = userAgent.toLowerCase(Locale.ROOT);
        return ua.contains("bot") || ua.contains("crawler") || ua.contains("spider") || ua.contains("preview")
                || ua.contains("facebookexternalhit") || ua.contains("whatsapp") || ua.contains("slackbot")
                || ua.contains("curl/") || ua.contains("wget/") || ua.contains("python-requests")
                || ua.contains("headlesschrome");
    }

    public static String deviceType(String userAgent) {
        if (userAgent == null) {
            return "Unknown";
        }
        String ua = userAgent.toLowerCase(Locale.ROOT);
        // Tablets first: iPad and many Android tablets also contain "mobile".
        if (ua.contains("ipad") || ua.contains("tablet") || (ua.contains("android") && !ua.contains("mobile"))) {
            return "Tablet";
        }
        if (ua.contains("mobi") || ua.contains("iphone") || ua.contains("android")) {
            return "Mobile";
        }
        return "Desktop";
    }

    public static String browser(String userAgent) {
        if (userAgent == null) {
            return "Unknown";
        }
        String ua = userAgent.toLowerCase(Locale.ROOT);
        // Edge and Opera include "chrome"; Chrome includes "safari": check the specific ones first.
        if (ua.contains("edg/") || ua.contains("edga/") || ua.contains("edgios/")) return "Edge";
        if (ua.contains("opr/") || ua.contains("opera")) return "Opera";
        if (ua.contains("samsungbrowser")) return "Samsung Internet";
        if (ua.contains("firefox") || ua.contains("fxios")) return "Firefox";
        if (ua.contains("chrome") || ua.contains("crios")) return "Chrome";
        if (ua.contains("safari")) return "Safari";
        return "Other";
    }
}
