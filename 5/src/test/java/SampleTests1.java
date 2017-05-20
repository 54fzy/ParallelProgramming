// $Id: SampleTests1.java 65 2017-04-09 15:34:25Z abcdef $

import cs735_835.actors.Actor;
import cs735_835.actors.ActorSystem;
import cs735_835.actors.Mailbox;
import org.testng.annotations.*;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static grading.TestBehaviors.*;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.testng.Assert.*;

public class SampleTests1 {

    @DataProvider
    static Object[][] parameters() {
        return new Object[][]{
                {4, 100, 1000, 10}, // these parameters can vary
//                {8, 10000, 10000, 100}
        };
    }

    @Factory(dataProvider = "parameters")
    public SampleTests1(int workers, int actors, int messages, int throughput) {
        this.workers = workers;
        this.actors = actors;
        this.messages = messages;
        this.throughput = throughput;
    }

    final int workers, actors, messages, throughput;
    ExecutorService exec;

    @BeforeMethod
    void startExecutor() {
        exec = Executors.newFixedThreadPool(workers);
    }

    @AfterMethod
    void stopExecutor() {
        if (!exec.isTerminated()) {
            System.out.println("SHUTTING DOWN EXECUTOR");
            exec.shutdownNow();
        }
    }

    // mailbox tests

    @Test(description = "few messages, single threaded")
    void testMailbox1() throws Exception {
        exec.shutdown(); // don't need it
        Mailbox mailbox = new Mailbox("mailbox");
        mailbox.tell("foo", null);
        mailbox.tell("bar", null);
        assertEquals(mailbox.take(), "foo");
        assertEquals(mailbox.take(), "bar");
        assertNull(mailbox.take(0.0));
    }

    @Test(description = "few messages, timeouts, single threaded")
    void testMailbox2() throws Exception {
        exec.shutdown(); // don't need it
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

    // tests 1-6 don't use mailboxes

    @Test(description = "single message sent")
    void test1() throws Exception {
        ActorSystem system = new ActorSystem(exec);
        TestBehavior behavior = new TestBehavior();
        Actor actor = system.register(behavior, "A");
        actor.tell("M", null);
        Queue<Object> q = behavior.waitForMessages(1, 1.0);
        assertNotNull(q, "timeout");
        assertEquals(q.remove(), "M");
        System.out.println("dsfklgh");
        exec.shutdown();
        assertTrue(exec.awaitTermination(5, SECONDS));
        assertNull(behavior.waitForMessages(1, 0.0));
    }

    @Test(description = "multiple messages sent with various throughput values")
    void test2() throws Exception {
        int m = messages;
        ActorSystem system = new ActorSystem(exec, throughput);
        TestBehavior behavior = new TestBehavior();
        Actor actor = system.register(behavior, "A");
        for (int i = 0; i < m; i++)
            actor.tell(i, null);
        Queue<Object> q = behavior.waitForMessages(m, 30.0);
        assertNotNull(q, "timeout");
        for (int i = 0; i < m; i++)
            assertEquals(q.remove(), i);
        exec.shutdown();
        assertTrue(exec.awaitTermination(5, SECONDS));
        assertNull(behavior.waitForMessages(1, 0.0));
    }

    @Test(description = "multiple messages sent to multiple actors with various throughput values")
    void test3() throws Exception {
        int n = actors;
        int m = messages;
        ActorSystem system = new ActorSystem(exec, throughput);
        TestBehavior[] behaviors = new TestBehavior[n];
        Actor[] actors = new Actor[n];
        for (int i = 0; i < n; i++) {
            TestBehavior behavior = new TestBehavior();
            actors[i] = system.register(behavior, "A-" + i);
            behaviors[i] = behavior;
        }
        System.err.println("1");
        int idx = 0;
        for (Actor actor : actors)
            for (int j = 0; j < m; j++)
                actor.tell(j, null);
        System.err.println(idx++);
        System.err.println("2");
        for (TestBehavior behavior : behaviors) {
            Queue<Object> q = behavior.waitForMessages(m, 3);
            assertNotNull(q, "timeout");
            for (int i = 0; i < m; i++)
                assertEquals(q.remove(), i);
        }
        System.err.println("3");
        exec.shutdown();
        assertTrue(exec.awaitTermination(5, SECONDS));
        for (TestBehavior behavior : behaviors)
            assertNull(behavior.waitForMessages(1, 0.0));
    }

    @Test(description = "actor sends decreasing numbers to itself")
    void test4() throws Exception {
        int max = messages;
        ActorSystem system = new ActorSystem(exec, throughput);
        CountDown p = new CountDown();
        Actor actor = system.register(p, "A");
        actor.tell(max, actor);
        Queue<Object> q = p.waitForMessages(max + 1, 30.0);
        assertNotNull(q, "timeout");
        for (int i = max; i >= 0; i--)
            assertEquals(q.remove(), i);
        exec.shutdown();
        assertTrue(exec.awaitTermination(5, SECONDS));
        assertNull(p.waitForMessages(1, 0.0));
    }

    @Test(description = "two actors send decreasing numbers back and forth")
    void test5() throws Exception {
        int max = messages;
        ActorSystem system = new ActorSystem(exec, throughput);
        CountDown p1 = new CountDown();
        CountDown p2 = new CountDown();
        Actor a1 = system.register(p1, "A1");
        Actor a2 = system.register(p2, "A2");
        a2.tell(2 * max - 1, a1);
        Queue<Object> q1 = p1.waitForMessages(max, 30.0);
        assertNotNull(q1, "timeout");
        Queue<Object> q2 = p2.waitForMessages(max, 30.0);
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

    /* In a network of size n, each actor receives max 1 time, max-1 n times, max-2 n^2 times, ...
     * and max-k n^k times, for a grand total of (n^(max+1) - 1)/(n - 1) messages (for n>1)
     */
    @Test(description = "actors in a fully connected network send decreasing numbers to each other")
    void test6() throws Exception {
        int n = 5;
        int max = 8;
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

        Deque<Integer> d = new LinkedList<>();
        for (int m = max, t = 1; m >= 0; m--, t *= n) {
            for (int j = 0; j < t; j++)
                d.addFirst(m);
        }
        int total = d.size();
        Integer[] expectedMessages = d.toArray(new Integer[total]);
        for (CountDownSet counter : counters) {
            Queue<Object> q = counter.waitForMessages(1, 5.0);
            assertNotNull(q, "timeout");
            assertEquals(q.remove(), all);
            q = counter.waitForMessages(total, 60.0);
            assertNotNull(q, "timeout");
            Integer[] messages = q.toArray(new Integer[total]);
            Arrays.sort(messages);
            assertEquals(messages, expectedMessages);
        }
        exec.shutdown();
        assertTrue(exec.awaitTermination(5, SECONDS));
        for (CountDownSet counter : counters)
            assertNull(counter.waitForMessages(1, 0.0));
    }


    // tests below use mailboxes

    @Test(description = "actor converts strings to uppercase")
    void test7() throws Exception {
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
        System.err.println("Shutdown");
        assertTrue(exec.awaitTermination(5, SECONDS));
        assertNull(mailbox.take(0.0));
    }

    @Test(description = "actor repeats message until reset to a new message")
    void test8() throws Exception {
        Object[] messages = new Object[]{"first", 101, 3002, "second", 3, 1001, 2016};
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
        System.err.println("told");
        Thread.sleep(2000);
        Object target = null;
        for (Object message : messages) {
            if (message instanceof Integer) {
                for (int i = 0; i < (Integer) message; i++)
                    assertEquals(mailbox.take(3.0), target);
            } else {
                target = message;
            }
        }
        exec.shutdown();
        assertTrue(exec.awaitTermination(5, SECONDS));
        assertNull(mailbox.take(0.0));

    }

    @Test(description = "simple stop test")
    void test9() throws Exception {
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
        Actor actor = system.register(new B(), "A");
        actor.tell("BEFORE", mailbox);
        actor.tell("STOP", mailbox);
        actor.tell("AFTER", mailbox);
        assertEquals(mailbox.take(1.0), "BEFORE");
        assertEquals(mailbox.take(1.0), "OK");
        exec.shutdown();
        assertTrue(exec.awaitTermination(5, SECONDS));
        assertNull(mailbox.take(0.0));
    }

    @Test(description = "simple stop test with actions after stop")
    void test10() throws Exception {
        ActorSystem system = new ActorSystem(exec);
        Mailbox mailbox = new Mailbox("mailbox");
        class B extends ActorSystem.Behavior {
            protected void onReceive(Object message) {
                if (message.equals("STOP"))
                    stop();
                sender().tell(message, self());
            }
        }
        Actor actor = system.register(new B(), "A");
        actor.tell("BEFORE", mailbox);
        actor.tell("STOP", mailbox);
        actor.tell("AFTER", mailbox);
        System.err.println("Told");
        assertEquals(mailbox.take(1.0), "BEFORE");
        assertEquals(mailbox.take(1.0), "STOP");
        exec.shutdown();
        assertTrue(exec.awaitTermination(5, SECONDS));
        assertNull(mailbox.take(0.0));
    }

    @Test(description = "simple become test")
    void test11() throws Exception {
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
        Actor actor = system.register(new B1(), "A");
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

    @Test(description = "simple become test with actions after become")
    void test12() throws Exception {
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
        Actor actor = system.register(new B1(), "A");
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


    @Test(description = "trying to trigger under scheduling")
    void test13() throws Exception {
        int n = 10;
        int m = messages;
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

    @Test(description = "simple test of the tree-map of actors")
    void test14() throws Exception {
        ActorSystem system = new ActorSystem(exec);
        Mailbox mailbox = new Mailbox("mailbox");
        Actor map = system.register(new TreeNode(mailbox, "", null), "root");
        map.tell(new TreeNode.Put("X", 42), null);
        map.tell(new TreeNode.Get("Y"), null);
        TreeNode.Entry entry = (TreeNode.Entry) mailbox.take(1.0);
        assertNotNull(entry, "timeout");
        assertEquals(entry.key, "Y");
        assertNull(entry.value);
        Object object = new Object();
        map.tell(new TreeNode.Put("Y", object), null);
        map.tell(new TreeNode.Get("X"), null);
        entry = (TreeNode.Entry) mailbox.take(1.0);
        assertNotNull(entry, "timeout");
        assertEquals(entry.key, "X");
        assertEquals(entry.value, 42);
        map.tell(new TreeNode.Get("Y"), null);
        entry = (TreeNode.Entry) mailbox.take(1.0);
        assertNotNull(entry, "timeout");
        assertEquals(entry.key, "Y");
        assertSame(entry.value, object);
        map.tell(new TreeNode.Put("Y", "new"), null);
        map.tell(new TreeNode.Get("Y"), null);
        entry = (TreeNode.Entry) mailbox.take(1.0);
        assertNotNull(entry, "timeout");
        assertEquals(entry.key, "Y");
        assertEquals(entry.value, "new");
        exec.shutdown();
        assertTrue(exec.awaitTermination(5, SECONDS));
        assertNull(mailbox.take(0.0));
    }

    @Test(description = "sleeping tasks take predictable time based on parallelism")
    void test15() throws Exception {
        int p = workers;
        int n = 3 * p;
        int m = 5;
        assert n < p || n % p == 0;
        double d = 1;
        double expected = d;
        if (n <= p) {
            expected *= m;
            System.err.println("1 expected: " + expected);
        } else {
            expected *= m * n / p;
            System.err.println("2 expected: " + expected);
        }
//        System.err.println("number of messages = " + m + ", number of actors = " + n);
        Object done = new Object();
        ActorSystem system = new ActorSystem(exec);
        Mailbox mailbox = new Mailbox("mailbox");
        long nanos = System.nanoTime();
        for (int i = 0; i < n; i++) {
            Actor a = system.register(new Sleeper(), "sleeper-" + i);
            for (int j = 0; j < m; j++)
                a.tell(d, null);
            a.tell(done, mailbox);
        }
        for (int i = 0; i < n; i++) {
            assertNotNull(mailbox.take(2 * expected), "timeout");
        }
        nanos = System.nanoTime() - nanos;
        double duration = nanos / 1e9;
        assertTrue(duration >= expected && duration < 1.05 * expected, // 5% margin
                String.format("incorrect time %.1f, expected %.1f", duration, 1.05 * expected));
        exec.shutdown();
        assertTrue(exec.awaitTermination(5, SECONDS));
    }
}