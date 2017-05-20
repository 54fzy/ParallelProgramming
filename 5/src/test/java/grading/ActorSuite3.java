// $Id: ActorSuite3.java 68 2017-04-27 16:02:21Z abcdef $

package grading;

import cs735_835.actors.Actor;
import cs735_835.actors.ActorSystem;
import cs735_835.actors.Mailbox;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static grading.TestBehaviors.Sleeper;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class ActorSuite3 { // parallelism

  static final int RUNS = 10;

  @DataProvider
  Object[][] test1data() {
    return test1data;
  }

  static final Object[][] test1data;

  static {
    Random rand = new Random(2017);
    test1data = new Object[RUNS][];
    for (int run = 0; run < RUNS; run++) {
      int p = rand.nextInt(32) + 1;
      int n = rand.nextInt(4);
      if (n == 0)
        n = rand.nextInt(p) + 1;
      else
        n *= p;
      int m = rand.nextInt(20) + 1;
      test1data[run] = new Object[]{p, n, m};
    }
  }

  ExecutorService exec;

  @AfterMethod
  void stopExecutor() {
    if (!exec.isTerminated())
      exec.shutdownNow();
  }

  @Test(dataProvider = "test1data",
      description = "sleeping tasks take predictable time based on parallelism")
  void test1(int p, int n, int m) throws Exception {
    assert n < p || n % p == 0;
    double d = 1;
    double expected = d;
    if (n <= p)
      expected *= m;
    else
      expected *= m * n / p;
    Object done = new Object();
    exec = Executors.newFixedThreadPool(p);
    ActorSystem system = new ActorSystem(exec);
    Mailbox mailbox = new Mailbox("mailbox");
    long nanos = System.nanoTime();
    for (int i = 0; i < n; i++) {
      Actor a = system.register(new Sleeper(), "sleeper-" + i);
      for (int j = 0; j < m; j++)
        a.tell(d, null);
      a.tell(done, mailbox);
    }
    for (int i = 0; i < n; i++)
      assertNotNull(mailbox.take(2 * expected), "timeout");
    nanos = System.nanoTime() - nanos;
    double duration = nanos / 1e9;
    System.out.printf("p=%d, n=%d, m=%d%n", p, n, m);
    assertTrue(duration >= expected && duration < 1.05 * expected, // 5% margin
        String.format("incorrect time %.1f, expected %.1f", duration, expected));
    exec.shutdown();
    assertTrue(exec.awaitTermination(5, SECONDS));
  }
}
