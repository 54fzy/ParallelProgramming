// $Id: ActorSuite1.java 68 2017-04-27 16:02:21Z abcdef $

package grading;

import cs735_835.actors.Actor;
import cs735_835.actors.ActorSystem;
import org.testng.annotations.*;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static grading.TestBehaviors.*;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.testng.Assert.*;

public class ActorSuite1 { // basic actors

  @DataProvider
  static Object[][] poolSizes() {
    return new Object[][]{
        {1}, {2}, {4}, {8}, {16}, {256}
    };
  }

  @Factory(dataProvider = "poolSizes")
  public ActorSuite1(int n) {
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

  @Test(description = "single message sent [5pts]")
  void test1() throws Exception {
    ActorSystem system = new ActorSystem(exec);
    TestBehavior behavior = new TestBehavior();
    Actor actor = system.register(behavior, "A");
    actor.tell("M", null);
    Queue<Object> q = behavior.waitForMessages(1, 1.0);
    assertNotNull(q, "timeout");
    assertEquals(q.remove(), "M");
    exec.shutdown();
    assertTrue(exec.awaitTermination(5, SECONDS));
    assertNull(behavior.waitForMessages(1, 0.0));
  }

  @DataProvider
  Object[][] test2data() {
    return new Object[][]{
        {10, 1}, {100, 1}, {1000, 1}, {1_000_000, 1},
        {10, 3}, {100, 13}, {1000, 41}, {1_000_000, 101}, {1_000_000, Integer.MAX_VALUE}
    };
  }

  @Test(dataProvider = "test2data",
      description = "multiple messages sent with various throughput values")
  void test2(int m, int t) throws Exception {
    ActorSystem system = new ActorSystem(exec, t);
    TestBehavior behavior = new TestBehavior();
    Actor actor = system.register(behavior, "test2 actor");
    for (int i = 0; i < m; i++)
      actor.tell(i, null);
    Queue<Object> q = behavior.waitForMessages(m, 10.0);
    assertNotNull(q, "timeout");
    for (int i = 0; i < m; i++)
      assertEquals(q.remove(), i);
    exec.shutdown();
    assertTrue(exec.awaitTermination(5, SECONDS));
    assertNull(behavior.waitForMessages(1, 0.0));
  }

  @DataProvider
  Object[][] test3data() {
    return new Object[][]{
        {10, 10, 1}, {10, 100, 1}, {10, 1000, 7},
        {100, 10, 1}, {100, 100, 7}, {100, 1000, 31},
        {1000, 10, 1}, {1000, 100, 19}, {1000, 1000, 101},
        {10000, 10, 7}, {10000, 100, 31}, {10000, 1000, 2017},
        {10000, 1000, 11}
    };
  }

  @Test(dataProvider = "test3data",
      description = "multiple messages sent to multiple actors with various throughput values")
  void test3a(int n, int m, int k) throws Exception {
    ActorSystem system = new ActorSystem(exec, k);
    TestBehavior[] behaviors = new TestBehavior[n];
    Actor[] actors = new Actor[n];
    for (int i = 0; i < n; i++) {
      TestBehavior behavior = new TestBehavior();
      actors[i] = system.register(behavior, "test3 actor " + i);
      behaviors[i] = behavior;
    }
    for (Actor actor : actors)
      for (int j = 0; j < m; j++)
        actor.tell(j, null);
    for (TestBehavior behavior : behaviors) {
      Queue<Object> q = behavior.waitForMessages(m, 10.0);
      assertNotNull(q, "timeout");
      for (int i = 0; i < m; i++)
        assertEquals(q.remove(), i);
    }
    exec.shutdown();
    assertTrue(exec.awaitTermination(5, SECONDS));
    for (TestBehavior behavior : behaviors)
      assertNull(behavior.waitForMessages(1, 0.0));
  }

  @Test(dataProvider = "test3data",
      description = "same as 3a but more concurrent and less bursty")
  void test3b(int n, int m, int k) throws Exception {
    ActorSystem system = new ActorSystem(exec, k);
    TestBehavior[] behaviors = new TestBehavior[n];
    Actor[] actors = new Actor[n];
    for (int i = 0; i < n; i++) {
      TestBehavior behavior = new TestBehavior();
      actors[i] = system.register(behavior, "test3 actor " + i);
      behaviors[i] = behavior;
    }
    for (int j = 0; j < m; j++)
      for (Actor actor : actors)
        actor.tell(j, null);
    for (TestBehavior behavior : behaviors) {
      Queue<Object> q = behavior.waitForMessages(m, 10.0);
      assertNotNull(q, "timeout");
      for (int i = 0; i < m; i++)
        assertEquals(q.remove(), i);
    }
    exec.shutdown();
    assertTrue(exec.awaitTermination(5, SECONDS));
    for (TestBehavior behavior : behaviors)
      assertNull(behavior.waitForMessages(1, 0.0));
  }

  @DataProvider
  Object[][] test4data() {
    return new Object[][]{
        {10, 1}, {100, 1}, {1000, 1}, {1_000_000, 1},
        {10, 7}, {100, 7}, {1000, 7}, {1_000_000, 7}
    };
  }

  @Test(dataProvider = "test4data",
      description = "actor sends decreasing numbers to itself")
  void test4a(int max, int k) throws Exception {
    ActorSystem system = new ActorSystem(exec, k);
    CountDown p = new CountDown();
    Actor actor = system.register(p, "actor for test4a");
    actor.tell(max, actor);
    Queue<Object> q = p.waitForMessages(max + 1, 10.0);
    assertNotNull(q, "timeout");
    for (int i = max; i >= 0; i--)
      assertEquals(q.remove(), i);
    exec.shutdown();
    assertTrue(exec.awaitTermination(5, SECONDS));
    assertNull(p.waitForMessages(1, 0.0));
  }

  @Test(dataProvider = "test4data",
      description = "two actors send decreasing numbers back and forth")
  void test4b(int max, int k) throws Exception {
    ActorSystem system = new ActorSystem(exec, k);
    CountDown p1 = new CountDown();
    CountDown p2 = new CountDown();
    Actor a1 = system.register(p1, "a1");
    Actor a2 = system.register(p2, "a2");
    a2.tell(2 * max - 1, a1);
    Queue<Object> q1 = p1.waitForMessages(max, 60.0);
    assertNotNull(q1, "timeout");
    Queue<Object> q2 = p2.waitForMessages(max, 0.0);
    assertNotNull(q2, "timeout");
    int i = 2 * max - 1;
    while (i > 0) {
      assertEquals(q2.remove(), i--);
      assertEquals(q1.remove(), i--);
    }
    exec.shutdown();
    assertTrue(exec.awaitTermination(1, SECONDS));
    assertNull(p1.waitForMessages(1, 0.0));
    assertNull(p2.waitForMessages(1, 0.0));
  }

  @DataProvider
  Object[][] test5data() {
    return new Object[][]{
        {3, 7}, {3, 12},
        {4, 5}, {4, 9},
        {5, 5}, {5, 8},
        {7, 3}, {7, 7}
    };
  }

  /* In a network of size n, each actor receives max 1 time, max-1 n times, max-2 n^2 times, ...
   * and max-k n^k times, for a grand total of (n^(max+1) - 1)/(n - 1) messages (for n>1)
   */
  @Test(dataProvider = "test5data",
      description = "actors in a fully connected network send decreasing numbers to each other")
  void test5(int n, int max) throws Exception {
    ActorSystem system = new ActorSystem(exec, 100);
    CountDownSet[] counters = new CountDownSet[n];
    Actor[] actors = new Actor[n];
    Arrays.setAll(counters, i -> new CountDownSet());
    for (int i = 0; i < n; i++)
      actors[i] = system.register(counters[i], "counter-" + i);
    List<Actor> all = Collections.unmodifiableList(Arrays.asList(actors));
    for (Actor actor : actors)
      actor.tell(all, null);
    for (Actor actor : actors)
      actor.tell(max, null);

    Deque<Integer> d = new java.util.LinkedList<>();
    for (int m = max, t = 1; m >= 0; m--, t *= n) {
      for (int j = 0; j < t; j++)
        d.addFirst(m);
    }
    int total = d.size();
    Integer[] expectedMessages = d.toArray(new Integer[total]);
    for (CountDownSet counter : counters) {
      Queue<Object> q = counter.waitForMessages(1, 10.0);
      assertNotNull(q, "timeout");
      assertEquals(q.remove(), all);
      q = counter.waitForMessages(total, 5.0);
      assertNotNull(q, "timeout");
      @SuppressWarnings("SuspiciousToArrayCall")
      Integer[] messages = q.toArray(new Integer[total]);
      Arrays.sort(messages);
      assertEquals(messages, expectedMessages);
    }
    exec.shutdown();
    assertTrue(exec.awaitTermination(5, SECONDS));
    for (CountDownSet counter : counters)
      assertNull(counter.waitForMessages(1, 0.0));
  }
}