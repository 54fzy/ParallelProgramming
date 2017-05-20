// $Id: ActorSuite2.java 68 2017-04-27 16:02:21Z abcdef $

package grading;

import cs735_835.CountDownWheelCenter;
import cs735_835.actors.Actor;
import cs735_835.actors.Mailbox;
import cs735_835.actors.ActorSystem;
import org.testng.annotations.*;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static grading.TestBehaviors.*;
import static grading.TestBehaviors.TreeNode.*;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.testng.Assert.*;

public class ActorSuite2 { // fancy actors (mailbox, become, children, etc.)

  @DataProvider
  static Object[][] poolSizes() {
    return new Object[][]{
        {1}, {2}, {4}, {8}, {16}, {256}
    };
  }

  @Factory(dataProvider = "poolSizes")
  public ActorSuite2(int n) {
    workers = n;
  }

  final int workers;
  ExecutorService exec;

  @BeforeMethod
  void startExecutor() {
    exec = Executors.newFixedThreadPool(workers);
  }

  @AfterMethod
  void stopExecutor() {
    if (!exec.isTerminated())
      exec.shutdownNow();
  }

  @Test(description = "actor converts strings to uppercase [3pts]")
  void test1() throws Exception {
    ActorSystem system = new ActorSystem(exec);
    Mailbox mailbox = new Mailbox("mailbox");
    Actor actor = system.register(new ToUpper(), "uppercase converter");
    actor.tell("foo", mailbox);
    actor.tell("bar", mailbox);
    Object reply = mailbox.take(1.0);
    assertNotNull(reply, "timeout");
    assertEquals(reply, "FOO");
    reply = mailbox.take(1.0);
    assertNotNull(reply, "timeout");
    assertEquals(reply, "BAR");
    exec.shutdown();
    assertTrue(exec.awaitTermination(5, SECONDS));
    assertNull(mailbox.take(0.0));
  }

  @DataProvider
  Object[][] test2data() {
    return new Object[][]{
        {1}, {10}, {100}
    };
  }

  @Test(dataProvider = "test2data",
      description = "actor repeats its initial message")
  void test2(int n) throws Exception {
    int sum = (n * (n + 1)) / 2;
    ActorSystem system = new ActorSystem(exec);
    Mailbox mailbox = new Mailbox("mailbox");
    Actor actor = system.register(new FixedRepeater(), "repeater");
    actor.tell("blah", null);
    for (int i = 1; i <= n; i++)
      actor.tell(i, mailbox);
    for (int i = 0; i < sum; i++) {
      Object msg = mailbox.take(10.0);
      assertNotNull(msg, "timeout");
      assertEquals(msg, "blah");
    }
    actor.tell(0, mailbox); // stops the actor
    actor.tell(1, mailbox); // this message is never read
    exec.shutdown();
    assertTrue(exec.awaitTermination(5, SECONDS));
    assertNull(mailbox.take(0.0));
  }

  @DataProvider
  Object[][] test3data() {
    return new Object[][]{
        {"foo", 1, 3, "bar", 3, 1, 1},
        {"first", 101, 3002, "second", 3, 1001, 2017},
        test3dataArray
    };
  }

  static final Object[] test3dataArray;

  static {
    int n = 10000;
    test3dataArray = new Object[3 * n];
    int i = 0;
    while (n-- > 0) {
      test3dataArray[i++] = "M" + n;
      test3dataArray[i++] = 1;
      test3dataArray[i++] = 1001;
    }
  }

  @Test(dataProvider = "test3data",
      description = "actor repeats message until reset to a new message")
  void test3(Object... messages) throws Exception {
    ActorSystem system = new ActorSystem(exec);
    Mailbox mailbox = new Mailbox("mailbox");
    Actor actor = system.register(new Repeater(), "repeater");
    actor.tell(messages[0], null);
    for (int i = 1; i < messages.length; i++) {
      Object message = messages[i];
      if (message instanceof Integer) {
        actor.tell(message, mailbox);
      } else {
        actor.tell(0, mailbox);
        actor.tell(message, null);
      }
    }
    Object target = null;
    for (Object message : messages) {
      if (message instanceof Integer) {
        for (int i = 0; i < (Integer) message; i++) {
          Object msg = mailbox.take(1.0);
          assertNotNull(msg, "timeout");
          assertEquals(msg, target);
        }
      } else {
        target = message;
      }
    }
    exec.shutdown();
    assertTrue(exec.awaitTermination(5, SECONDS));
    assertNull(mailbox.take(0.0));

  }

  @Test(description = "simple stop test [3pts]")
  void test4() throws Exception {
    ActorSystem system = new ActorSystem(exec);
    Mailbox mailbox = new Mailbox("mailbox");
    class B extends ActorSystem.Behavior {
      protected void onReceive(Object message) {
        if (message.equals("STOP")) {
          sender().tell("OK", self());
          stop();
        } else {
          sender().tell(message, self());
        }
      }
    }
    Actor actor = system.register(new B(), "test4 actor");
    actor.tell("BEFORE", mailbox);
    actor.tell("STOP", mailbox);
    actor.tell("AFTER", mailbox);
    assertEquals(mailbox.take(1.0), "BEFORE");
    assertEquals(mailbox.take(1.0), "OK");
    exec.shutdown();
    assertTrue(exec.awaitTermination(5, SECONDS));
    assertNull(mailbox.take(0.0));
  }

  @Test(description = "simple stop test with actions after stop [2pts]")
  void test5() throws Exception {
    ActorSystem system = new ActorSystem(exec);
    Mailbox mailbox = new Mailbox("mailbox");
    class B extends ActorSystem.Behavior {
      protected void onReceive(Object message) {
        if (message.equals("STOP"))
          stop();
        sender().tell(message, self());
      }
    }
    Actor actor = system.register(new B(), "test5 actor");
    actor.tell("BEFORE", mailbox);
    actor.tell("STOP", mailbox);
    actor.tell("AFTER", mailbox);
    assertEquals(mailbox.take(1.0), "BEFORE");
    assertEquals(mailbox.take(1.0), "STOP");
    exec.shutdown();
    assertTrue(exec.awaitTermination(5, SECONDS));
    assertNull(mailbox.take(0.0));
  }

  @Test(description = "simple become test [3pts]")
  void test6() throws Exception {
    ActorSystem system = new ActorSystem(exec);
    Mailbox mailbox = new Mailbox("mailbox");
    class B2 extends ActorSystem.Behavior {
      protected void onReceive(Object message) {
        sender().tell(message + "2", self());
      }
    }
    class B1 extends ActorSystem.Behavior {
      protected void onReceive(Object message) {
        if (message.equals("CHANGE"))
          become(new B2());
        else
          sender().tell(message + "1", self());
      }
    }
    Actor actor = system.register(new B1(), "test6 actor");
    actor.tell("A", mailbox);
    actor.tell("B", mailbox);
    actor.tell("CHANGE", mailbox);
    actor.tell("C", mailbox);
    actor.tell("D", mailbox);
    assertEquals(mailbox.take(1.0), "A1");
    assertEquals(mailbox.take(1.0), "B1");
    assertEquals(mailbox.take(1.0), "C2");
    assertEquals(mailbox.take(1.0), "D2");
    exec.shutdown();
    assertTrue(exec.awaitTermination(5, SECONDS));
    assertNull(mailbox.take(0.0));
  }

  @Test(description = "simple become test with actions after become [2pts]")
  void test7() throws Exception {
    ActorSystem system = new ActorSystem(exec);
    Mailbox mailbox = new Mailbox("mailbox");
    class B2 extends ActorSystem.Behavior {
      protected void onReceive(Object message) {
        sender().tell(message + "2", self());
      }
    }
    class B1 extends ActorSystem.Behavior {
      protected void onReceive(Object message) {
        if (message.equals("CHANGE"))
          become(new B2());
        sender().tell(message + "1", self());
      }
    }
    Actor actor = system.register(new B1(), "test7 actor");
    actor.tell("A", mailbox);
    actor.tell("B", mailbox);
    actor.tell("CHANGE", mailbox);
    actor.tell("C", mailbox);
    actor.tell("D", mailbox);
    assertEquals(mailbox.take(1.0), "A1");
    assertEquals(mailbox.take(1.0), "B1");
    assertEquals(mailbox.take(1.0), "CHANGE1");
    assertEquals(mailbox.take(1.0), "C2");
    assertEquals(mailbox.take(1.0), "D2");
    exec.shutdown();
    assertTrue(exec.awaitTermination(5, SECONDS));
    assertNull(mailbox.take(0.0));
  }

  @Test(description = "actor sends messages to itself [3pts]")
  void test8() throws Exception {
    ActorSystem system = new ActorSystem(exec);
    Mailbox mailbox = new Mailbox("mailbox");
    class B extends ActorSystem.Behavior {
      Actor replyTo;

      protected void onReceive(Object message) {
        if (replyTo == null)
          replyTo = sender();
        int n = (Integer) message;
        if (n > 0)
          self().tell(n - 1, self());
        else
          replyTo.tell("DONE", self());
      }
    }
    Actor actor = system.register(new B(), "test8 actor");
    actor.tell(2017, mailbox);
    assertEquals(mailbox.take(5.0), "DONE");
    exec.shutdown();
    assertTrue(exec.awaitTermination(5, SECONDS));
    assertNull(mailbox.take(0.0));
  }

  @DataProvider
  Object[][] test9data() {
    return new Object[][]{
        {10}, {100}, {1000}, {10_000}, {100_000}, {1000_000}
    };
  }

  @Test(dataProvider = "test9data",
      description = "lines of actors")
  void test9(int n) throws Exception {
    ActorSystem system = new ActorSystem(exec);
    Mailbox mailbox = new Mailbox("mailbox");
    class B2 extends ActorSystem.Behavior {
      final Actor replyTo;

      public B2(Actor a) {
        replyTo = a;
      }

      protected void onReceive(Object message) {
        replyTo.tell((Integer) message + 1, self());
      }
    }
    class B1 extends ActorSystem.Behavior {
      protected void onReceive(Object message) {
        int n = (Integer) message;
        if (n > 0) {
          int m = n - 1;
          Actor a = system().register(new B1(), "B1-" + m);
          a.tell(m, self());
        } else {
          self().tell(-1, self());
        }
        become(new B2(sender()));
      }
    }
    Actor actor = system.register(new B1(), "test9 actor");
    actor.tell(n, mailbox);
    assertEquals(mailbox.take(10.0), n);
    exec.shutdown();
    assertTrue(exec.awaitTermination(5, SECONDS));
    assertNull(mailbox.take(0.0));
  }

  @DataProvider
  Object[][] test10data() {
    return new Object[][]{
        {1, 1_000_000}, {2, 1_000_000}, {32, 100_000}
    };
  }

  @Test(dataProvider = "test10data",
      description = "trying to trigger under scheduling [2pts]")
  void test10(int n, int m) throws Exception {
    ExecutorService exec2 = Executors.newCachedThreadPool();
    CountDownLatch done = new CountDownLatch(n);
    ActorSystem system = new ActorSystem(exec);

    class B extends ActorSystem.Behavior {
      protected void onReceive(Object message) {
        sender().tell(message, self());
      }
    }

    class T implements Runnable {
      final int id;

      public T(int i) {
        id = i;
      }

      public void run() {
        try {
          Actor actor = system.register(new B(), "T" + id);
          Mailbox mailbox = new Mailbox("mailbox" + id);
          Object message = new Object();
          for (int i = 0; i < m; i++) {
            actor.tell(message, mailbox);
            mailbox.take();
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } finally {
          done.countDown();
        }
      }
    }

    for (int i = 0; i < n; i++)
      exec2.execute(new T(i));
    exec2.shutdown();
    if (!done.await(60, SECONDS)) {
      exec2.shutdownNow();
      fail("timeout, presumably from deadlock");
    }
    exec.shutdown();
    assertTrue(exec.awaitTermination(5, SECONDS));
  }

  // Note: no way to test hasMessage=true without additional synchronization
  @Test(description = "hasMessage false when inbox empty [3pts]")
  void test11() throws Exception {
    ActorSystem system = new ActorSystem(exec);
    Mailbox mailbox = new Mailbox("mailbox");
    class B extends ActorSystem.Behavior {
      protected void onReceive(Object message) {
        sender().tell(hasMessage(), self());
      }
    }
    Actor actor = system.register(new B(), "test11 actor");
    actor.tell(new Object(), mailbox);
    assertFalse((Boolean) mailbox.take(1.0));
    exec.shutdown();
    assertTrue(exec.awaitTermination(5, SECONDS));
  }

  @DataProvider
  static Object[][] testWheelData() {
    return new Object[][]{
        {1, 10, 10}, {1, 1000, 1000},
        {10, 100, 100}, {10, 1000, 1000}, {10, 10000, 10000},
        {1000, 100, 100}, {1000, 1000, 1000}, {1000, 10000, 10000},
        {10000, 100, 100}, {10000, 1000, 1000}, {10000, 10000, 10000}
    };
  }

  @Test(dataProvider = "testWheelData",
      description = "testing the wheel application")
  void testWheel(int size, int count, int max) throws Exception {
    Random rand = new Random(2017);
    ActorSystem system = new ActorSystem(exec, 64);
    Mailbox mailbox = new Mailbox("mailbox");
    Actor master = system.register(
        new CountDownWheelCenter(size, () -> rand.nextInt(max)), "master"
    );
    master.tell(count, mailbox);
    Object reply = mailbox.take(30.0);
    assertNotNull(reply, "timeout");
    assertTrue(reply instanceof Double);
    exec.shutdown();
    assertTrue(exec.awaitTermination(5, SECONDS));
    assertNull(mailbox.take(0.0));
  }

  @Test(description = "simple test of the tree-map of actors [3pts]")
  void testTree1() throws Exception {
    ActorSystem system = new ActorSystem(exec);
    Mailbox mailbox = new Mailbox("mailbox");
    Actor map = system.register(new TreeNode(mailbox, "", null), "root");
    map.tell(new Put("X", 42), null);
    map.tell(new Get("Y"), null);
    Entry entry = (Entry) mailbox.take(1.0);
    assertNotNull(entry, "timeout");
    assertEquals(entry.key, "Y");
    assertNull(entry.value);
    Object object = new Object();
    map.tell(new Put("Y", object), null);
    map.tell(new Get("X"), null);
    entry = (Entry) mailbox.take(1.0);
    assertNotNull(entry, "timeout");
    assertEquals(entry.key, "X");
    assertEquals(entry.value, 42);
    map.tell(new Get("Y"), null);
    entry = (Entry) mailbox.take(1.0);
    assertNotNull(entry, "timeout");
    assertEquals(entry.key, "Y");
    assertSame(entry.value, object);
    map.tell(new Put("Y", "new"), null);
    map.tell(new Get("Y"), null);
    entry = (Entry) mailbox.take(1.0);
    assertNotNull(entry, "timeout");
    assertEquals(entry.key, "Y");
    assertEquals(entry.value, "new");
    exec.shutdown();
    assertTrue(exec.awaitTermination(5, SECONDS));
    assertNull(mailbox.take(0.0));
  }

  @DataProvider
  Object[][] testTree2data() {
    return new Object[][]{
        {10}, {100}, {1000}, {10000}, {100000}
    };
  }

  @Test(dataProvider = "testTree2data",
      description = "multiple tests of the tree-map of actors")
  void testTree2(int n) throws Exception {
    Random rand = new Random(2017);
    ActorSystem system = new ActorSystem(exec, 64);
    Mailbox mailbox = new Mailbox("mailbox");
    Actor map = system.register(new TreeNode(mailbox, "", null), "root");
    Set<String> keys = new java.util.HashSet<>(n);
    for (int i = 0; i < n; i++) {
      int r;
      String key;
      do {
        r = rand.nextInt();
        key = String.valueOf(r);
      } while (!keys.add(key));
      map.tell(new Put(key, r), null);
      map.tell(new Get(key), null);
      map.tell(new Put(key, -r), null);
      map.tell(new Get(key), null);
    }
    for (int i = 0, l = 3 * n; i < l; i++) {
      Entry entry = (Entry) mailbox.take(10.0);
      assertNotNull(entry, "timeout");
      String key = entry.key;
      Object value = entry.value;
      int v = Integer.parseInt(key);
      if (keys.remove(key)) { // first reply
        assertEquals(value, v);
        map.tell(new Put(key, key), null);
      } else if (value instanceof Integer) { // second reply
        assertEquals(entry.value, -v);
        map.tell(new Get(key), null);
      } else { // third reply
        assertSame(key, value);
      }
    }
    assertTrue(keys.isEmpty());
    exec.shutdown();
    assertTrue(exec.awaitTermination(5, SECONDS));
    assertNull(mailbox.take(0.0));
  }
}
