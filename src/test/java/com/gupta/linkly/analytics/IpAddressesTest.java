package com.gupta.linkly.analytics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IpAddressesTest {

    @Test
    void validatesIpStrings() {
        assertThat(IpAddresses.isValid("8.8.8.8")).isTrue();
        assertThat(IpAddresses.isValid("2001:4860:4860::8888")).isTrue();
        assertThat(IpAddresses.isValid("999.1.1.1")).isFalse();
        assertThat(IpAddresses.isValid("8.8.8.8/../../admin")).isFalse();
        assertThat(IpAddresses.isValid(null)).isFalse();
    }

    @Test
    void detectsPrivateRanges() {
        assertThat(IpAddresses.isPrivateOrLocal("10.1.2.3")).isTrue();
        assertThat(IpAddresses.isPrivateOrLocal("172.20.0.1")).isTrue();
        assertThat(IpAddresses.isPrivateOrLocal("192.168.1.1")).isTrue();
        assertThat(IpAddresses.isPrivateOrLocal("127.0.0.1")).isTrue();
        assertThat(IpAddresses.isPrivateOrLocal("::1")).isTrue();
        assertThat(IpAddresses.isPrivateOrLocal("8.8.8.8")).isFalse();
        assertThat(IpAddresses.isPrivateOrLocal("172.32.0.1")).isFalse();
    }

    @Test
    void hashIsStableAndDoesNotContainTheIp() {
        IpHasher hasher = new IpHasher("secret");
        assertThat(hasher.hash("8.8.8.8")).isEqualTo(hasher.hash("8.8.8.8")).doesNotContain("8.8.8.8").hasSize(32);
        assertThat(new IpHasher("other").hash("8.8.8.8")).isNotEqualTo(hasher.hash("8.8.8.8"));
    }
}
