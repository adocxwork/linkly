package com.gupta.linkly.analytics;

import com.gupta.linkly.redis.RedisGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ClickStreamWorkerTest {

    private final JsonMapper json = JsonMapper.builder().build();
    private final StringRedisTemplate template = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final StreamOperations<String, Object, Object> stream = mock(StreamOperations.class);
    private final ClickProcessor processor = mock(ClickProcessor.class);
    private final PipelineStats stats = new PipelineStats();
    private ClickStreamWorker worker;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(template.opsForStream()).thenReturn(stream);
        when(template.execute(any(RedisCallback.class))).thenReturn("OK"); // XGROUP CREATE
        when(stream.acknowledge(anyString(), anyString(), any(RecordId[].class))).thenReturn(1L);
        RedisGateway gateway = new RedisGateway(template, true, Duration.ofSeconds(30), Clock.systemUTC());
        ClickEventPublisher publisher = new ClickEventPublisher(gateway, new LocalClickBuffer(10), stats, json, "clicks", 1000, Runnable::run);
        worker = new ClickStreamWorker(gateway, publisher, new LocalClickBuffer(10), processor, stats, json,
                "group", "consumer", 100, Duration.ofMinutes(5));
    }

    /**
     * Simulates Redis: the record is first delivered as a NEW record (ID ">"); until it is
     * acknowledged, later runs see it again in this consumer's PENDING list (ID "0").
     */
    @SuppressWarnings("unchecked")
    private void streamReturns(MapRecord<String, Object, Object> record) {
        java.util.concurrent.atomic.AtomicBoolean delivered = new java.util.concurrent.atomic.AtomicBoolean();
        when(stream.read(any(Consumer.class), any(StreamReadOptions.class), any(StreamOffset.class))).thenAnswer(inv -> {
            StreamOffset<String> offset = inv.getArgument(2);
            boolean pendingRead = "0".equals(offset.getOffset().getOffset());
            if (pendingRead) {
                return delivered.get() ? List.of(record) : List.of();
            }
            return delivered.getAndSet(true) ? List.of() : List.of(record);
        });
    }

    private MapRecord<String, Object, Object> record(String id, String payload) {
        return StreamRecords.newRecord().in("clicks").withId(RecordId.of(id)).ofMap(Map.<Object, Object>of("payload", payload));
    }

    @Test
    void acknowledgesAfterSuccessfulProcessing() {
        ClickEvent event = new ClickEvent(UUID.randomUUID(), UUID.randomUUID(), "8.8.8.8", "Mozilla", 1L);
        streamReturns(record("1-0", json.writeValueAsString(event)));

        worker.drainStream();

        verify(processor).process(List.of(event));
        verify(stream).acknowledge(eq("clicks"), eq("group"), eq(RecordId.of("1-0")));
    }

    @Test
    void leavesRecordPendingDuringDatabaseFailureAndRetriesAfterRecovery() throws Exception {
        ClickEvent event = new ClickEvent(UUID.randomUUID(), UUID.randomUUID(), "8.8.8.8", "Mozilla", 1L);
        streamReturns(record("1-0", json.writeValueAsString(event)));
        when(processor.process(anyList())).thenThrow(new RuntimeException("database down")).thenReturn(1);

        worker.drainStream();
        worker.drainStream();
        verify(processor, times(1)).process(List.of(event));
        verify(stream, never()).acknowledge(anyString(), anyString(), any(RecordId[].class));

        var retry = ClickStreamWorker.class.getDeclaredField("retryStreamAt");
        retry.setAccessible(true);
        retry.set(worker, java.time.Instant.EPOCH);
        worker.drainStream();

        verify(stream).acknowledge(eq("clicks"), eq("group"), eq(RecordId.of("1-0")));
        assertThat(stats.getDeadLettered()).hasValue(0);
    }

    @Test
    @SuppressWarnings("unchecked")
    void derivesTheSameIdWhenAnOlderRecordIsRedelivered() {
        streamReturns(record("1720000000000-0", """
                {"linkId":"4bcea688-fb56-4814-aa03-68d8df8cb39c","ip":"8.8.8.8","userAgent":"Mozilla"}
                """));
        when(stream.acknowledge(anyString(), anyString(), any(RecordId[].class))).thenReturn(null, 1L);

        worker.drainStream();
        worker.drainStream();

        var first = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(processor, times(2)).process(first.capture());
        assertThat(first.getAllValues().get(0)).isEqualTo(first.getAllValues().get(1));
        ClickEvent event = (ClickEvent) first.getAllValues().get(0).get(0);
        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredAtMillis()).isEqualTo(1720000000000L);
    }

    @Test
    void malformedPayloadIsAcknowledgedImmediately() {
        streamReturns(record("2-0", "{not json"));
        worker.drainStream();
        verify(processor, never()).process(anyList());
        verify(stream).acknowledge(eq("clicks"), eq("group"), eq(RecordId.of("2-0")));
        assertThat(stats.getDeadLettered()).hasValue(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void doesNotTouchRedisWhenThereIsNothingToDo() {
        worker.drainStream(); // first call: periodic sweep is due
        clearInvocations(stream);
        worker.drainStream(); // no new events, sweep not due
        verify(stream, never()).read(any(Consumer.class), any(StreamReadOptions.class), any(StreamOffset.class));
    }

}
