package logics.calendar;

import com.typesafe.config.ConfigFactory;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.stream.Materializer;
import org.apache.pekko.stream.javadsl.Sink;
import org.apache.pekko.stream.javadsl.Source;
import org.apache.pekko.util.ByteString;

import java.util.concurrent.ExecutionException;

/**
 * Runs a streamed iCalendar to a string, for assertions.
 */
final class Ics {

    // daemonic: its threads do not keep the test JVM alive
    private static final ActorSystem SYSTEM = ActorSystem.create("ics-test", ConfigFactory.parseString("pekko.daemonic = on"));

    private Ics() {
    }

    static String text(Source<ByteString, ?> ics) {
        try {
            return ics.runWith(Sink.fold(ByteString.emptyByteString(), ByteString::concat), Materializer.matFromSystem(SYSTEM))
                    .toCompletableFuture().get().utf8String();
        } catch (InterruptedException | ExecutionException e) {
            throw new AssertionError(e);
        }
    }
}
