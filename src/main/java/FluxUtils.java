import reactor.core.CoreSubscriber;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import java.time.Duration;
import java.util.function.Function;
import java.util.function.Supplier;

public class FluxUtils {

    /**
     * Creates a flux that polls the supplied flux as fast as it can but waits for delay
     * if no items are requested from downstream.
     */
    public static <T> Flux<T> observeRequested(Duration delay, int bufferSize, int limit, Function<Integer, Flux<T>> flux) {
        return new Flux<T>() {
            @Override
            public void subscribe(CoreSubscriber<? super T> actual) {
                EmitterProcessor<T> emitter = EmitterProcessor.create(bufferSize, true);
                FluxSink<T> sink = emitter.sink(FluxSink.OverflowStrategy.ERROR);
                observeRequestedLoop(sink, delay, limit, flux)
                        .takeUntil(x -> sink.isCancelled())
                        .subscribe(sink::next, sink::error);
                emitter.subscribe(actual);
            }
        };
    }

    private static <T> Flux<T> observeRequestedLoop(FluxSink<T> sink, Duration delay, int limit, Function<Integer, Flux<T>> flux) {
        if (sink.isCancelled()) {
            return Flux.empty();
        }
        Supplier<Flux<T>> observe = () -> observeRequestedLoop(sink, delay, limit, flux);
        int count = Math.min(limit, (int) sink.requestedFromDownstream());
        if (count == 0) {
            return Flux.defer(observe).delaySubscription(delay);
        }
        Flux<T> next = Flux.defer(() -> sink.isCancelled() ? Flux.empty() : observe.get());
        return Flux.concat(flux.apply(count), next); // add artificial relay and the stack overflow disappears
    }
}
