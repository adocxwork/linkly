package com.gupta.linkly.analytics;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeoIpServiceTest {
    private static final String ENDPOINT = "https://geo.example.test/geo.json";
    private final RestClient.Builder client = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(client).build();

    private GeoIpService service(boolean enabled) {
        return new GeoIpService(enabled, ENDPOINT, JsonMapper.builder().build(), client.build());
    }

    @Test
    void resolvesSingletonRepliesAndReusesCachedResults() {
        server.expect(requestTo(ENDPOINT + "?ip=8.8.8.8"))
                .andRespond(withSuccess("""
                        {"ip":"8.8.8.8","country":"United States","city":"Unknown"}
                        """, MediaType.APPLICATION_JSON));
        GeoIpService service = service(true);
        var expected = new GeoIpService.Location("United States", "Unknown");
        assertThat(service.lookup(List.of("10.0.0.5", "8.8.8.8")))
                .containsEntry("8.8.8.8", expected)
                .containsEntry("10.0.0.5", GeoIpService.Location.LOCAL);
        assertThat(service.lookup(List.of("8.8.8.8"))).containsEntry("8.8.8.8", expected);
        assertThat(service.getLastSuccessAt()).isNotNull();
        server.verify();
    }

    @Test
    void matchesExpandedIpv6InBatchReplies() {
        server.expect(requestTo(ENDPOINT + "?ip=8.8.8.8%2C2001%3A4860%3A%3A8888"))
                .andRespond(withSuccess("""
                        [{"ip":"8.8.8.8","country":"United States"},
                         {"ip":"2001:4860:0:0:0:0:0:8888","country":"United States"}]
                        """, MediaType.APPLICATION_JSON));
        assertThat(service(true).lookup(List.of("8.8.8.8", "2001:4860::8888")))
                .containsEntry("2001:4860::8888", new GeoIpService.Location("United States", "Unknown"));
        server.verify();
    }

    @Test
    void pausesLookupsOnProviderFailureAndLeavesAnalyticsUsable() {
        server.expect(requestTo(ENDPOINT + "?ip=8.8.8.8")).andRespond(withServerError());
        GeoIpService service = service(true);
        assertThat(service.lookup(List.of("8.8.8.8"))).containsValue(GeoIpService.Location.UNKNOWN);
        assertThat(service.isPaused()).isTrue();
        assertThat(service.lookup(List.of("1.1.1.1"))).containsValue(GeoIpService.Location.UNKNOWN);
        server.verify();
    }

    @Test
    void sendsNoRequestsWhenLookupIsDisabled() {
        assertThat(service(false).lookup(List.of("8.8.8.8"))).containsValue(GeoIpService.Location.UNKNOWN);
        server.verify();
    }
}
