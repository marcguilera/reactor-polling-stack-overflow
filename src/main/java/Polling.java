import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.time.Duration;
import java.util.function.Function;
import java.util.function.Supplier;

public class Polling {
    public static <T> Flux<T> createEager(Duration delay, int limit, Supplier<Publisher<T>> publisher) {
        return createEager(delay, limit, function(limit, publisher));
    }
    public static <T> Flux<T> createEager(Duration delay, int limit, Function<Integer, Publisher<T>> publisher) {
        return Flux.create(sink -> eagerLoop(sink, delay, limit, publisher).subscribe(sink::next, sink::error));
    }

    private static <T> Flux<T> eagerLoop(FluxSink<T> sink, Duration delay, int limit, Function<Integer, Publisher<T>> publisher) {
        if (sink.isCancelled()) {
            return Flux.empty();
        }
        Supplier<Flux<T>> next = () -> eagerLoop(sink, delay, limit, publisher);
        int count = getCount(sink, limit);
        if (count > 0) {
            return Flux.concat(publisher.apply(count), Flux.defer(next));
        }
        return Flux.defer(next).delaySubscription(delay);
    }

    private static int getCount(FluxSink<?> sink, int limit) {
        return (int) Math.min(sink.requestedFromDownstream(), limit);
    }

    private static <T> Function<Integer, Publisher<T>> function(int limit, Supplier<Publisher<T>> publisher) {
        return count -> Flux.defer(publisher).limitRequest(limit).take(limit);
    }
}
