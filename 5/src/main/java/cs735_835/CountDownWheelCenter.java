// $Id: CountDownWheelCenter.java 65 2017-04-09 15:34:25Z abcdef $

package cs735_835;

import cs735_835.actors.Actor;
import cs735_835.actors.ActorSystem;

import java.util.Arrays;
import java.util.function.IntSupplier;

/**
 * A wheel of messages.  This purposeless application can be used to generate large numbers of
 * actors and messages and thus evaluate the performance of an actor system.
 * <p>
 * The behavior of this system is as follows:
 * <ul>
 * <li>The first message received by the center of the wheel is an integer: the number of messages
 * to generate.  The center then generates worker agents at the periphery of the wheel and sends
 * them numbers in a round-robin fashion.</li>
 * <li>When a worker receives a number {@code n}:
 * <ul>
 * <li>it sends {@code n-1} to the next worker on the ring if {@code n} is positive, or</li>
 * <li>it sends back a zero to the center otherwise.</li>
 * </ul></li>
 * <li>In the meantime, the center is collecting those zeros.  When it receives the last zero,
 * it sends a final message to the agent that initiated the computation (the sender of the very
 * first integer).  This last message is the time elapsed, in seconds, between the initial
 * integer and the final zero.</li>
 * </ul>
 * </p>
 *
 * @author Michel Charpentier
 */
public class CountDownWheelCenter extends ActorSystem.Behavior {

    private final int size;
    private final IntSupplier numbers;

    /**
     * Creates the behavior of a new center agent.
     *
     * @param n the size of the wheel (the number of worker agents).
     * @param s a function used to generate integers to inject into the wheel.
     */
    public CountDownWheelCenter(int n, IntSupplier s) {
        size = n;
        numbers = s;
    }

    protected void onReceive(Object message) {
        long time = System.nanoTime();
        int n = (Integer) message;
        if (n < 0)
            throw new IllegalArgumentException("number of messages cannot be negative");
        ActorSystem system = system();
        Actor[] workers = new Actor[size];
        Arrays.setAll(workers, i -> system.register(new WorkerInit(), "worker-" + i));
        for (int i = 1; i < size; i++)
            workers[i - 1].tell(workers[i], self());
        workers[size - 1].tell(workers[0], self());
        int next = 0;
        for (int i = 0; i < n; i++) {
            workers[next].tell(numbers.getAsInt(), self());
            if (++next == size)
                next = 0;
        }
        become(new CenterWait(n, sender(), time));
    }

    private static class CenterWait extends ActorSystem.Behavior {

        private final Actor replyTo;
        private final long time;
        private int count;

        public CenterWait(int count, Actor replyTo, long time) {
            this.count = count;
            this.replyTo = replyTo;
            this.time = time;
        }

        protected void onReceive(Object message) {
            assert count > 0 && message.equals(0);
            count -= 1;
            if (count == 0) {
                double duration = (System.nanoTime() - time) / 1e9;
                replyTo.tell(duration, self());
                stop();
            }
        }
    }

    private static class WorkerInit extends ActorSystem.Behavior {

        protected void onReceive(Object message) {
            Actor center = sender();
            Actor next = (Actor) message;
            become(new WorkerRun(center, next));
        }
    }

    private static class WorkerRun extends ActorSystem.Behavior {

        private final Actor center, next;

        public WorkerRun(Actor center, Actor next) {
            this.center = center;
            this.next = next;
        }

        protected void onReceive(Object message) {
            int n = (Integer) message;
            assert n >= 0;
            if (n > 0)
                next.tell(n - 1, self());
            else
                center.tell(0, self());
        }
    }
}