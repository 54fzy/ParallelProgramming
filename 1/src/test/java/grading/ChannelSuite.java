// $Id: ChannelSuite.java 56 2017-02-13 17:47:07Z cs735a $

package grading;

import cs735_835.channels.Channel;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.*;

public class ChannelSuite {

  final ChannelFactory channelFactory;

  public ChannelSuite(ChannelFactory factory) {
    channelFactory = factory;
  }

  @DataProvider
  Object[][] testSetData() {
    return new Object[][]{
        {10, 10000},
        {10, 100000},
        {100, 10000},
        {500, 10000}
    };
  }

  @Test(description = "n writers send m random numbers each [1pt]",
      dataProvider = "testSetData")
  void testSet1(int n, int m) throws InterruptedException {
    Channel<String> chan = channelFactory.makeChannel(n);

    class Writer extends Thread {
      public final Set<String> strings = new HashSet<>(m);
      private final int id;

      public Writer(int i) {
        super("writer-" + 1);
        id = i;
      }

      public void run() {
        Random rand = new Random(id);
        for (int i = 0; i < m; i++) {
          String s = String.valueOf(rand.nextInt());
          chan.put(s, id);
          strings.add(s);
        }
        chan.put("", id);
      }
    }

    class Reader extends Thread {
      public final Set<String> strings = new HashSet<>(n * m);

      public Reader() {
        super("reader");
      }

      public void run() {
        int count = n;
        while (true) {
          String s = chan.get();
          if (s == null) {
            if (count == 0)
              break;
            continue;
          }
          if (s.isEmpty())
            count -= 1;
          else
            strings.add(s);
        }
      }
    }

    Writer[] writers = new Writer[n];
    Reader reader = new Reader();
    reader.start();
    for (int i = 0; i < n; i++)
      (writers[i] = new Writer(i)).start();
    Set<String> strings = new HashSet<>(n * m);
    for (Writer w : writers) {
      w.join();
      strings.addAll(w.strings);
    }
    reader.join();
    assertEquals(chan.totalCount(), n * (m + 1L));
    assertEquals(strings.size(), reader.strings.size());
    assertEquals(strings, reader.strings);
  }

  @Test(description = "n writers send m random numbers each in a 2n channel [1pt]",
      dataProvider = "testSetData")
  void testSet2(int n, int m) throws InterruptedException {
    Channel<Integer> chan = channelFactory.makeChannel(2 * n);

    class Writer extends Thread {
      public int xor;
      private final int id;

      public Writer(int i) {
        super("writer-" + 1);
        id = i;
      }

      public void run() {
        Random rand = new Random(id);
        int q = 2 * id;
        for (int i = 0; i < m; i++) {
          int s;
          do {
            s = rand.nextInt();
          } while (s == 0);
          chan.put(s, q);
          xor ^= s;
        }
        chan.put(0, q);
      }
    }

    class Reader extends Thread {
      public int xor;

      public Reader() {
        super("reader");
      }

      public void run() {
        int count = n;
        while (true) {
          Integer s = chan.get();
          if (s == null) {
            if (count == 0)
              break;
            continue;
          }
          int t = s;
          if (t == 0)
            count -= 1;
          else
            xor ^= t;
        }
      }
    }

    int xor = 0;
    Writer[] writers = new Writer[n];
    Reader reader = new Reader();
    reader.start();
    for (int i = 0; i < n; i++)
      (writers[i] = new Writer(i)).start();
    for (Writer w : writers) {
      w.join();
      xor ^= w.xor;
    }
    reader.join();
    assertEquals(chan.totalCount(), n * (m + 1L));
    assertEquals(reader.xor, xor);
  }

  @Test(description = "a slow writer [3pts]")
  void testSlowWriter() throws Exception {
    int m = 100_000_000;
    long millis = 100;
    Channel<Integer> chan = channelFactory.makeChannel(1);
    AtomicInteger count = new AtomicInteger();

    Thread reader = new Thread("reader") {
      public void run() {
        while (chan.get() == null)
          count.incrementAndGet();
      }
    };
    reader.start();
    while (count.get() < m) {
      assertTrue(reader.isAlive());
      Thread.sleep(millis);
    }
    chan.put(0, 0);
    reader.join();
  }

  @DataProvider
  Object[][] testQueueCountData() {
    return new Object[][]{
        {1}, {10}, {512}, {9876}
    };
  }

  @Test(description = "queueCount [1pt]", dataProvider = "testQueueCountData")
  void testQueueCount(int n) throws Exception {
    Channel<Object> chan = channelFactory.makeChannel(n);
    assertEquals(chan.queueCount(), n);
  }

  @Test(description = "get on new channel [2pts]")
  void testGetNew() throws Exception {
    Channel<Object> chan = channelFactory.makeChannel(128);
    for (int i = 0; i < 1000; i++)
      assertNull(chan.get());
  }

  @Test(description = "totalCount on new channel [2pts]")
  void testTotalCountNew() throws Exception {
    Channel<Object> chan = channelFactory.makeChannel(128);
    for (int i = 0; i < 1000; i++)
      assertEquals(chan.totalCount(), 0);
  }

  @DataProvider
  Object[][] testTotalCountData() {
    return new Object[][]{
        {5, 100},
        {32, 1_000},
        {64, 10_000},
        {100, 100_000}
    };
  }

  @Test(description = "totalCount [1pt]", dataProvider = "testTotalCountData")
  void testTotalCount(int n, int m) throws Exception {
    int t = n * m;
    Channel<Object> chan = channelFactory.makeChannel(2 * n);
    Object msg = new Object();

    class Writer extends Thread {
      private final int id;

      public Writer(int i) {
        super("writer-" + 1);
        id = i;
      }

      public void run() {
        int q = 2 * id;
        for (int i = 0; i < m; i++)
          chan.put(msg, q);
      }
    }

    Writer[] writers = new Writer[n];
    for (int i = 0; i < n; i++)
      (writers[i] = new Writer(i)).start();

    while (chan.totalCount() < t) {
      Object x = chan.get();
      assert (x == null || x == msg);
    }
    for (Writer w : writers)
      w.join();
  }

  @Test(description = "put in non-existing queue [2pts]", expectedExceptions = IllegalArgumentException.class)
  void testPutNonExisting() throws Exception {
    Channel<Object> chan = channelFactory.makeChannel(42);
    chan.put(new Object(), 42);
  }

  @Test(description = "put of null [3pts]", expectedExceptions = IllegalArgumentException.class)
  void testPutNull() throws Exception {
    Channel<Object> chan = channelFactory.makeChannel(42);
    chan.put(null, 10);
  }
}
