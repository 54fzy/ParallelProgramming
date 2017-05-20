// $Id: MessageSuite.java 60 2017-03-26 21:08:15Z abcdef $

package grading;

import cs735_835.noc.Message;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.testng.Assert.*;

public class MessageSuite {

  ExecutorService exec;

  @AfterMethod
  void shutdown() {
    if (exec != null) {
      exec.shutdownNow();
      exec = null;
    }
  }

  @Test(description = "message properly initialized")
  void test01() throws Exception {
    Message m1 = new Message(1, 1, 2, 3, 4, 5);
    Message m2 = new Message(2, 1, 2, 3, 4, 5);
    assertNotEquals(m1, m2);
    assertNotEquals(m1.getId(), m2.getId());
    assertEquals(m1.getId(), m1.hashCode());
    assertEquals(m2.getId(), m2.hashCode());
    assertEquals(m1.sourceRow(), 2);
    assertEquals(m1.sourceCol(), 3);
    assertEquals(m1.destRow(), 4);
    assertEquals(m1.destCol(), 5);
  }

  @Test(description = "tracking set and get")
  void test02() throws Exception {
    Message m1 = new Message(1, 5, 4, 3, 2, 1);
    Message m2 = new Message(2, 1, 2, 3, 4, 5);
    assertFalse(m1.isTracked());
    assertFalse(m2.isTracked());
    m1.setTracked(true);
    assertTrue(m1.isTracked());
    assertFalse(m2.isTracked());
    m1.setTracked(false);
    assertFalse(m1.isTracked());
    assertFalse(m2.isTracked());
  }

  @Test(description = "send and receive, single thread [3pts]")
  void test03() throws Exception {
    Message m1 = new Message(1, 5, 4, 3, 2, 1);
    Message m2 = new Message(2, 1, 2, 3, 4, 5);
    assertEquals(m1.getScheduledTime(), 5);
    assertEquals(m2.getScheduledTime(), 1);
    assertEquals(m1.getSendTime(), -1);
    assertEquals(m2.getSendTime(), -1);
    assertEquals(m1.getReceiveTime(), -1);
    assertEquals(m2.getReceiveTime(), -1);
    assertFalse(m1.hasBeenSent());
    assertFalse(m1.hasBeenReceived());
    assertFalse(m2.hasBeenSent());
    assertFalse(m2.hasBeenReceived());
    m1.send(7);
    assertEquals(m1.getSendTime(), 7);
    assertEquals(m2.getSendTime(), -1);
    assertEquals(m1.getReceiveTime(), -1);
    assertEquals(m2.getReceiveTime(), -1);
    assertTrue(m1.hasBeenSent());
    assertFalse(m1.hasBeenReceived());
    assertFalse(m2.hasBeenSent());
    assertFalse(m2.hasBeenReceived());
    m1.receive(10);
    assertEquals(m1.getSendTime(), 7);
    assertEquals(m2.getSendTime(), -1);
    assertEquals(m1.getReceiveTime(), 10);
    assertEquals(m2.getReceiveTime(), -1);
    assertTrue(m1.hasBeenSent());
    assertTrue(m1.hasBeenReceived());
    assertFalse(m2.hasBeenSent());
    assertFalse(m2.hasBeenReceived());
  }

  @Test(description = "toString")
  void test04() throws Exception {
    Message m = new Message(42, 5, 4, 3, 2, 1);
    int id = m.getId();
    assertEquals(m.toString(), "msg " + id + " (never sent)");
    m.send(11);
    assertEquals(m.toString(), "msg " + id + " sent by (4, 3) at 11 (never delivered)");
    m.receive(20);
    assertEquals(m.toString(), "msg " + id + " sent by (4, 3) at 11, delivered to (2, 1) at 20");
  }

  @Test(description = "exception (constructor)", expectedExceptions = IllegalArgumentException.class)
  void test05() throws Exception {
    new Message(1, -5, 4, 3, 2, 1);
  }

  @Test(description = "exception (receive without send)",
      expectedExceptions = IllegalStateException.class)
  void test06() throws Exception {
    Message m = new Message(1, 5, 4, 3, 2, 1);
    m.receive(10);
  }

  @Test(description = "exception (illegal send)",
      expectedExceptions = IllegalArgumentException.class)
  void test07() throws Exception {
    Message m = new Message(1, 5, 4, 3, 2, 1);
    m.send(-10);
  }

  @Test(description = "exception (illegal receive)",
      expectedExceptions = IllegalArgumentException.class)
  void test08() throws Exception {
    Message m = new Message(1, 5, 4, 3, 2, 1);
    m.send(10);
    m.receive(-20);
  }

  @Test(description = "exception (double send)",
      expectedExceptions = IllegalStateException.class)
  void test09() throws Exception {
    Message m = new Message(1, 5, 4, 3, 2, 1);
    m.send(10);
    m.send(11);
  }

  @Test(description = "exception (double receive)",
      expectedExceptions = IllegalStateException.class)
  void test10() throws Exception {
    Message m = new Message(1, 5, 4, 3, 2, 1);
    m.send(10);
    m.receive(20);
    m.receive(21);
  }

  @Test(description = "exception (receive before send)",
      expectedExceptions = IllegalArgumentException.class)
  void test11() throws Exception {
    Message m = new Message(1, 5, 4, 3, 2, 1);
    m.send(10);
    m.receive(9);
  }

  @DataProvider
  Object[][] test12data() {
    return new Object[][]{
        {10, 1000}, {10, 100000}, {100, 1000}, {100, 100000}, {250, 1000}, {250, 10000}
    };
  }

  @Test(dataProvider = "test12data", description = "thread-safety of send", timeOut = 60000)
  void test12b(int N, int M) throws Exception { // N threads, M messages
    exec = Executors.newCachedThreadPool();

    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch ready = new CountDownLatch(N);
    CountDownLatch done = new CountDownLatch(N);
    AtomicInteger failures = new AtomicInteger();
    Message[] messages = new Message[M];

    for (int i = 0; i < M; i++)
      messages[i] = new Message(i + 1, i + 1, 2, 3, 4, 5);

    class Task implements Runnable {
      public void run() {
        try {
          int f = 0;
          ready.countDown();
          start.await();
          for (int i = 0; i < M; i++) {
            try {
              messages[i].send(i + 1);
            } catch (IllegalStateException e) {
              f++;
            }
          }
          failures.addAndGet(f);
          done.countDown();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }

    for (int i = 0; i < N; i++)
      exec.execute(new Task());
    exec.shutdown();

    assertTrue(ready.await(1, SECONDS));
    start.countDown();
    assertTrue(done.await(50, SECONDS));
    for (Message m : messages) {
      assertTrue(m.hasBeenSent());
      assertEquals(m.getSendTime(), m.getScheduledTime());
    }
    assertEquals(failures.get(), M * (N - 1));
    assertTrue(exec.awaitTermination(1, SECONDS));
  }

  @Test(dataProvider = "test12data", description = "thread-safety of receive", timeOut = 60000)
  void test12c(int N, int M) throws Exception { // N threads, M messages
    exec = Executors.newCachedThreadPool();

    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch ready = new CountDownLatch(N);
    CountDownLatch done = new CountDownLatch(N);
    AtomicInteger failures = new AtomicInteger();
    Message[] messages = new Message[M];

    for (int i = 0; i < M; i++)
      (messages[i] = new Message(i + 1, i + 1, 2, 3, 4, 5)).send(i + 1);

    class Task implements Runnable {
      public void run() {
        try {
          int f = 0;
          ready.countDown();
          start.await();
          for (int i = 0; i < M; i++) {
            try {
              messages[i].receive(i + 11);
            } catch (IllegalStateException e) {
              f++;
            }
          }
          failures.addAndGet(f);
          done.countDown();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }

    for (int i = 0; i < N; i++)
      exec.execute(new Task());
    exec.shutdown();

    assertTrue(ready.await(1, SECONDS));
    start.countDown();
    assertTrue(done.await(50, SECONDS));
    for (Message m : messages) {
      assertTrue(m.hasBeenSent());
      assertTrue(m.hasBeenReceived());
      assertEquals(m.getReceiveTime(), m.getScheduledTime() + 10);
    }
    assertEquals(failures.get(), M * (N - 1));
    assertTrue(exec.awaitTermination(1, SECONDS));
  }

  void checkMessages(List<Message> messages) {
    assertEquals(messages.size(), 2);
    Message m1 = messages.get(0);
    Message m2 = messages.get(1);
    assertEquals(m1.getId(), 735);
    assertEquals(m2.getId(), 835);
    assertEquals(m1.getScheduledTime(), 100);
    assertEquals(m2.getScheduledTime(), 101);
    assertEquals(m1.getSendTime(), -1);
    assertEquals(m2.getSendTime(), -1);
    assertEquals(m1.getReceiveTime(), -1);
    assertEquals(m2.getReceiveTime(), -1);
    assertEquals(m1.sourceRow(), 1);
    assertEquals(m2.sourceRow(), 4);
    assertEquals(m1.sourceCol(), 2);
    assertEquals(m2.sourceCol(), 3);
    assertEquals(m1.destRow(), 3);
    assertEquals(m2.destRow(), 2);
    assertEquals(m1.destCol(), 4);
    assertEquals(m2.destCol(), 1);
    assertFalse(m1.isTracked());
    assertTrue(m2.isTracked());
  }

  @Test(description = "parsing (correct)")
  void test13a() throws Exception {
    URL url = MessageSuite.class.getResource("/messages1.txt");
    checkMessages(Message.readMessagesFromURL(url));
  }

  @Test(description = "parsing (whitespace)")
  void test13b() throws Exception {
    URL url = MessageSuite.class.getResource("/messages2.txt");
    checkMessages(Message.readMessagesFromURL(url));
  }
}
