import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.Duration;
import java.util.function.Supplier;

public class PollingTest {
    @Test
    public void createPeriod_shouldPollEagerly() {
        assertPoll(Polling::createEager);
    }
    private static void assertPoll(PollingFactory<String> factory) {
        TestPublisher<String> publisher = TestPublisher.<String>create();
        Flux<String> flux = factory.apply(Duration.ofSeconds(1), 1, () -> publisher);
        StepVerifier.create(flux)
                .then(() -> publisher.next("first"))
                .expectNext("first")
                .then(() -> publisher.next("second"))
                .expectNext("second")
                .then(publisher::complete)
                .verifyComplete();
    }
    private interface PollingFactory<T> {
        Flux<T> apply(Duration delay, int limit, Supplier<Publisher<T>> publisher);
    }
}
