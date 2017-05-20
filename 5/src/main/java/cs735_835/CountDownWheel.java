// $Id: CountDownWheel.java 65 2017-04-09 15:34:25Z abcdef $

package cs735_835;

import cs735_835.actors.Actor;
import cs735_835.actors.ActorSystem;
import cs735_835.actors.Mailbox;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * A command-line application to run instances of the "countdown wheel" system.
 * Of particular interest is the type and size of thread pool that produces the best performance.
 * For instance, Akka uses {@link java.util.concurrent.ForkJoinPool} because of the work-stealing
 * pattern, which makes sense for an actor system implementation.  It is doubtful that they need
 * the fork/join mechanism.  The newer {@link Executors#newWorkStealingPool(int)} seems to offer
 * the same work-stealing feature without the fork/join mechanism.  Should it be used instead?
 */
public class CountDownWheel {

    private CountDownWheel() {
        throw new AssertionError("This class cannot be instantiated");
    }

    /**
     * Command-line application:
     *
     * @param args <pre>&lt;#actors&gt; &lt;#message&gt; &lt;maxMsgLen&gt; &lt;poolSize&gt; [#runs]</pre>
     */
    public static void main(String[] args) throws Exception {
        int size, count, max, workers, runs = 1;
        try {
            size = Integer.parseInt(args[0]);
            count = Integer.parseInt(args[1]);
            max = Integer.parseInt(args[2]);
            workers = Integer.parseInt(args[3]);
            if (args.length > 4)
                runs = Integer.parseInt(args[4]);
        } catch (Exception e) {
            System.err.println("usage: <#actors> <#message> <maxMsgLen> <poolSize> [#runs]");
            return;
        }
        double sum = 0;
        for (int i = 0; i < runs; i++) {
            Random rand = new Random(2017);
            ExecutorService exec = (workers > 0)
                    ? Executors.newFixedThreadPool(workers)
                    : Executors.newWorkStealingPool(-workers);
            ActorSystem system = new ActorSystem(exec, 64);
            Mailbox mailbox = new Mailbox("mailbox");
            Actor master = system.register(
                    new CountDownWheelCenter(size, () -> rand.nextInt(max)), "master"
            );
            System.gc();
            master.tell(count, mailbox);
            double duration = (Double) mailbox.take();
            exec.shutdown();
            if (!exec.awaitTermination(1, MINUTES)) {
                System.err.println("EXECUTOR DOES NOT TERMINATE!");
                System.exit(1);
            }
            System.out.printf("%.3f%n", duration);
            sum += duration;
        }
        System.out.printf("average: %.3f%n", sum / runs);
    }
}