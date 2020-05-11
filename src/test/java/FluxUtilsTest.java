import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

public class FluxUtilsTest {
    @Test
    public void createPeriod_shouldPollEagerly() {
        AtomicInteger i = new AtomicInteger();
        FluxUtils.observeRequested(Duration.ofMillis(1000), 5, 5,  limit -> {
            System.out.println("--------------------------> " + i.get());
            return Flux.range(i.get(), limit).doOnNext(i::set);
        }).blockLast();
    }
}
