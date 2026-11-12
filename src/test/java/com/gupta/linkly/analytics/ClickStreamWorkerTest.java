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
        RedisGateway gateway = new RedisGateway(template, true, Duration.ofSeconds(30), Clock.systemUTC());
        ClickEventPublisher publisher = new ClickEventPublisher(gateway, new LocalClickBuffer(10), stats, json, "clicks", 1000);
        worker = new ClickStreamWorker(gateway, publisher, new LocalClickBuffer(10), processor, stats, json,
                "group", "consumer", 100, Duration.ofMinutes(5), 3);
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
        ClickEvent event = new ClickEvent(UUID.randomUUID(), "8.8.8.8", "Mozilla", 1L);
        streamReturns(record("1-0", json.writeValueAsString(event)));

        worker.drainStream();

        verify(processor).process(List.of(event));
        verify(stream).acknowledge(eq("clicks"), eq("group"), eq(RecordId.of("1-0")));
    }

    @Test
    void leavesRecordPendingOnFailureThenDeadLettersAfterMaxAttempts() {
        ClickEvent event = new ClickEvent(UUID.randomUUID(), "8.8.8.8", "Mozilla", 1L);
        streamReturns(record("1-0", json.writeValueAsString(event)));
        when(processor.process(anyList())).thenThrow(new RuntimeException("database down"));

        worker.drainStream();                       // attempt 1: not acknowledged
        verify(stream, never()).acknowledge(anyString(), anyString(), any(RecordId[].class));

        forceSweep(); worker.drainStream();         // attempt 2
        forceSweep(); worker.drainStream();         // attempt 3 = max: give up

        verify(stream).acknowledge(eq("clicks"), eq("group"), eq(RecordId.of("1-0")));
        assertThat(stats.getDeadLettered()).hasValue(1);
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

    private void forceSweep() {
        try {
            var field = ClickStreamWorker.class.getDeclaredField("nextSweepAt");
            field.setAccessible(true);
            field.set(worker, java.time.Instant.EPOCH);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
