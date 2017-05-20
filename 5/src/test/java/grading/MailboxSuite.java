// $Id: MailboxSuite.java 66 2017-04-20 19:14:26Z abcdef $

package grading;

import cs735_835.actors.Mailbox;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.testng.Assert.*;

public class MailboxSuite {

  static final int WORKERS = 12;

  ExecutorService exec;

  @AfterMethod
  void stopExecutor() {
    if (exec != null && !exec.isTerminated())
      exec.shutdownNow();
  }

  @Test(description = "few messages, single threaded")
  void test1() throws Exception {
    Mailbox mailbox = new Mailbox("mailbox");
    mailbox.tell("foo", null);
    mailbox.tell("bar", null);
    assertEquals(mailbox.take(), "foo");
    assertEquals(mailbox.take(), "bar");
    assertNull(mailbox.take(0.0));
  }

  @Test(description = "few messages, timeouts, single threaded")
  void test2() throws Exception {
    Mailbox mailbox = new Mailbox("mailbox");
    mailbox.tell("foo", null);
    mailbox.tell("bar", null);
    assertEquals(mailbox.take(1.0), "foo");
    assertEquals(mailbox.take(0.0), "bar");
    long time = System.nanoTime();
    Object o = mailbox.take(0.5);
    time = System.nanoTime() - time;
    assertNull(o);
    assertTrue(time >= 500e6 && time < 600e6, "incorrect time: " + time);
  }

  @Test(description = "toString")
  void test3() throws Exception {
    assertEquals(new Mailbox("Joe").toString(), "Joe");
  }

  @Test(description = "many messages in parallel")
  void test4() throws Exception {
    int n = 1000;
    int m = 10000;
    Mailbox mailbox = new Mailbox("mailbox");
    AtomicInteger ids = new AtomicInteger();
    exec = Executors.newFixedThreadPool(WORKERS);
    for (int i = 0; i < n; i++)
      exec.execute(() -> {
        Integer id = ids.getAndIncrement();
        for (int j = 0; j < m; j++)
          mailbox.tell(id, null);
      });
    int[] counts = new int[n];
    for (int i = 0, l = m * n; i < l; i++)
      counts[(Integer) mailbox.take()] += 1;
    for (int c : counts)
      assertEquals(c, m);
    exec.shutdown();
    assertTrue(exec.awaitTermination(5, SECONDS));
    assertNull(mailbox.take(0.0));
  }
}