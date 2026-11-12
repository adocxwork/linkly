package com.gupta.linkly.analytics;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class UserAgentParserTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0 Safari/537.36|Desktop|Chrome",
            "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 Chrome/120.0 Safari/537.36 Edg/120.0|Desktop|Edge",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) Version/17.0 Mobile/15E148 Safari/604.1|Mobile|Safari",
            "Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X) Version/17.0 Mobile/15E148 Safari/604.1|Tablet|Safari",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) FxiOS/120.0 Mobile/15E148 Safari/605.1.15|Mobile|Firefox",
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) Chrome/120.0 Mobile Safari/537.36|Mobile|Chrome",
            "Mozilla/5.0 (Linux; Android 13; SM-X700) Chrome/120.0 Safari/537.36|Tablet|Chrome",
    })
    void classifiesDeviceAndBrowser(String ua, String device, String browser) {
        assertThat(UserAgentParser.deviceType(ua)).isEqualTo(device);
        assertThat(UserAgentParser.browser(ua)).isEqualTo(browser);
        assertThat(UserAgentParser.isBot(ua)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"Googlebot/2.1", "WhatsApp/2.23.20", "facebookexternalhit/1.1", "curl/8.4.0", "Slackbot-LinkExpanding 1.0"})
    void detectsBotsAndLinkPreviews(String ua) {
        assertThat(UserAgentParser.isBot(ua)).isTrue();
    }
}
