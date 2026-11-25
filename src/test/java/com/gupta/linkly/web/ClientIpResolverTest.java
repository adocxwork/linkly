package com.gupta.linkly.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    private MockHttpServletRequest request(String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5"); // the proxy's internal address
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        return request;
    }

    @Test
    void readsTheClientHeaderOnlyBehindAnExplicitlyTrustedEdge() {
        var request = request("203.0.113.7, 10.0.0.5");
        request.addHeader("CF-Connecting-IP", "8.8.8.8");

        assertThat(new ClientIpResolver(1, true).resolve(request)).isEqualTo("8.8.8.8");
        assertThat(new ClientIpResolver(1, false).resolve(request)).isEqualTo("10.0.0.5");
    }

    @Test
    void rejectsMultipleAddressesInTheClientHeader() {
        var request = request("203.0.113.7");
        request.addHeader("CF-Connecting-IP", "8.8.8.8, 1.1.1.1");

        assertThat(new ClientIpResolver(1, true).resolve(request)).isEqualTo("203.0.113.7");
    }

    @Test
    void usesTheEntryAddedByOurProxy() {
        ClientIpResolver resolver = new ClientIpResolver(1, false);
        assertThat(resolver.resolve(request("203.0.113.7"))).isEqualTo("203.0.113.7");
    }

    @Test
    void forgedLeftEntriesAreIgnored() {
        ClientIpResolver resolver = new ClientIpResolver(1, false);
        // attacker sent "X-Forwarded-For: 1.2.3.4"; Render appended the real IP
        assertThat(resolver.resolve(request("1.2.3.4, 203.0.113.7"))).isEqualTo("203.0.113.7");
    }

    @Test
    void supportsTwoProxyHops() {
        ClientIpResolver resolver = new ClientIpResolver(2, false);
        assertThat(resolver.resolve(request("1.2.3.4, 203.0.113.7, 76.76.21.21"))).isEqualTo("203.0.113.7");
    }

    @Test
    void rejectsAnIncompleteChainAndDoesNotShiftAcrossEmptyHops() {
        ClientIpResolver resolver = new ClientIpResolver(2, false);
        assertThat(resolver.resolve(request("203.0.113.7"))).isEqualTo("10.0.0.5");
        assertThat(resolver.resolve(request("203.0.113.7, , 76.76.21.21"))).isEqualTo("10.0.0.5");
    }

    @Test
    void fallsBackToRemoteAddrWithoutHeaderOrForGarbage() {
        ClientIpResolver resolver = new ClientIpResolver(1, false);
        assertThat(resolver.resolve(request(null))).isEqualTo("10.0.0.5");
        assertThat(resolver.resolve(request("not-an-ip"))).isEqualTo("10.0.0.5");
        assertThat(new ClientIpResolver(0, false).resolve(request("203.0.113.7"))).isEqualTo("10.0.0.5");
    }
}
