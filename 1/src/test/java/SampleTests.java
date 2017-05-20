// $Id: SampleTests.java 52 2017-01-30 18:54:41Z abcdef $

import cs735_835.channels.Channel;
import cs735_835.channels.MultiChannel;
import cs735_835.channels.NoCopyMultiChannel;
import cs735_835.channels.SimpleChannel;
import org.testng.ITestNGListener;
import org.testng.TestNG;
import org.testng.annotations.Test;
import org.testng.reporters.TextReporter;

import static org.testng.Assert.*;

/**
 * Sample channel tests to illustrate unit testing with testNG.
 */
public class SampleTests {

    public static void main(String[] args) {
    TestNG testng = new TestNG();
    testng.setTestClasses(new Class<?>[]{SampleTests.class});
    testng.setUseDefaultListeners(false);
    testng.addListener((ITestNGListener) new TextReporter("sample tests", 2));
    testng.run();
    }

    @Test
    void test1a() throws Exception {
        System.err.println("Test 1a");
        Channel<Integer> c = new SimpleChannel<>(2); // could be MultiChannel
    //    Channel<Integer> c = new MultiChannel<>(2); // could be MultiChannel
    //    Channel<Integer> c = new NoCopyMultiChannel<>(2); // could be MultiChannel
        assertNull(c.get());
        c.put(10, 0);
        c.put(20, 0);
        c.put(30, 1);
        int n = 0;
        n += c.get();
        n += c.get();
        n += c.get();
        assertEquals(n, 60);
        assertEquals(c.totalCount(), 3);
        assertNull(c.get());
    }

    @Test
    void test1b() throws Exception {
        System.err.println("Test 1b");
        //    Channel<Integer> c = new SimpleChannel<>(2); // could be MultiChannel
        Channel<Integer> c = new MultiChannel<>(2); // could be MultiChannel
        //    Channel<Integer> c = new NoCopyMultiChannel<>(2); // could be MultiChannel
        assertNull(c.get());
        c.put(10, 0);
        c.put(20, 0);
        c.put(30, 1);
        int n = 0;
        n += c.get();
        n += c.get();
        n += c.get();
        assertEquals(n, 60);
        assertEquals(c.totalCount(), 3);
        assertNull(c.get());
    }

    @Test
    void test1c() throws Exception {
        System.err.println("Test 1c");
//        Channel<Integer> c = new SimpleChannel<>(2); // could be MultiChannel
//        Channel<Integer> c = new MultiChannel<>(2); // could be MultiChannel
        Channel<Integer> c = new NoCopyMultiChannel<>(2); // could be MultiChannel
        assertNull(c.get());
        c.put(10, 0);
        c.put(20, 0);
        c.put(30, 1);
        int n = 0;
        n += c.get();
        n += c.get();
        n += c.get();
        assertEquals(n, 60);
        assertEquals(c.totalCount(), 3);
        assertNull(c.get());
    }

    @Test
    void test2a() throws Exception {
        System.err.println("Test 2a");
        int N = 1000000; // number of messages (per thread)
        Channel<Integer> c = new SimpleChannel<>(2); // could be another channel
    //    Channel<Integer> c = new MultiChannel<>(2); // could be another channel
    //    Channel<Integer> c = new NoCopyMultiChannel<>(2); // could be another channel

        class Sender implements Runnable {

          final int msg;
          final int id;

          Sender(int i, int x) {
            id = i;
            msg = x;
          }

          public void run() {
            for (int i = 0; i < N; i++)
              c.put(msg, id);
          }
        }

        Thread one = new Thread(new Sender(0, 1), "One");
        Thread zero = new Thread(new Sender(1, 0), "Zero");
        one.start();
        zero.start();
        int sum = 0, count = 0;
        int N2 = 2 * N;
        while (count < N2) {
          Integer x = c.get();
          if (x != null) {
            int v = x;
            assertTrue(v == 0 || v == 1);
            sum += v;
            count += 1;
          }
        }
        assertEquals(sum, N);
        assertEquals(c.totalCount(), N2);
        assertNull(c.get());
        one.join();
        zero.join();
    }

    @Test
    void test2b() throws Exception {
        System.err.println("Test 2b");
        int N = 1000000; // number of messages (per thread)
//        Channel<Integer> c = new SimpleChannel<>(2); // could be another channel
        Channel<Integer> c = new MultiChannel<>(2); // could be another channel
//        Channel<Integer> c = new NoCopyMultiChannel<>(2); // could be another channel

        class Sender implements Runnable {

            final int msg;
            final int id;

            Sender(int i, int x) {
                id = i;
                msg = x;
            }

            public void run() {
                for (int i = 0; i < N; i++)
                    c.put(msg, id);
            }
        }

        Thread one = new Thread(new Sender(0, 1), "One");
        Thread zero = new Thread(new Sender(1, 0), "Zero");
        one.start();
        zero.start();
        int sum = 0, count = 0;
        int N2 = 2 * N;
        while (count < N2) {
            Integer x = c.get();
            if (x != null) {
                int v = x;
                assertTrue(v == 0 || v == 1);
                sum += v;
                count += 1;
            }
        }
        assertEquals(sum, N);
        assertEquals(c.totalCount(), N2);
        assertNull(c.get());
        one.join();
        zero.join();
    }

    @Test
    void test2c() throws Exception {
        System.err.println("Test 2c");
        int N = 2000000; // number of messages (per thread)
//        Channel<Integer> c = new SimpleChannel<>(2); // could be another channel
//        Channel<Integer> c = new MultiChannel<>(2); // could be another channel
        Channel<Integer> c = new NoCopyMultiChannel<>(2); // could be another channel

        class Sender implements Runnable {

            final int msg;
            final int id;

            Sender(int i, int x) {
                id = i;
                msg = x;
            }

            public void run() {
                for (int i = 0; i < N; i++)
                    c.put(msg, id);
            }
        }

        Thread one = new Thread(new Sender(0, 1), "One");
        Thread zero = new Thread(new Sender(1, 0), "Zero");
        one.start();
        zero.start();
        int sum = 0, count = 0;
        int N2 = 2 * N;
        while (count < N2) {
            Integer x = c.get();
            if (x != null) {
                int v = x;
                assertTrue(v == 0 || v == 1);
                sum += v;
                count += 1;
//                System.err.println(count+" "+N2);
            }
        }
        assertEquals(sum, N);
        assertEquals(c.totalCount(), N2);
        assertNull(c.get());
        one.join();
        zero.join();
    }

    @Test
    void test3a() throws Exception {
        System.err.println("Test 3a");
        int N = 256; // number of threads
        int M = 1000; // number of messages (per thread)
        Channel<String> c = new SimpleChannel<>(N); // could be another channel
        //    Channel<String> c = new MultiChannel<>(N); // could be another channel
        //    Channel<String> c = new NoCopyMultiChannel<>(N); // could be another channel

        class Sender implements Runnable {

          final int id;

          Sender(int i) {
            id = i;
          }

          public void run() {
            String msg = String.valueOf(id);
            for (int i = 0; i < M; i++)
              c.put(msg, id);
          }
        }

        Thread[] threads = new Thread[N];
        for (int i = 0; i < N; i++) {
          Thread t = new Thread(new Sender(i), "Sender-" + i);
          t.start();
          threads[i] = t;
        }

        int NM = N * M, count = 0;
        int[] counts = new int[N];
        while (count < NM) {
          String msg = c.get();
          if (msg != null) {
            count++;
            counts[Integer.parseInt(msg)] += 1;
          }
        }
        assertEquals(c.totalCount(), NM);
        assertNull(c.get());
        for (int n : counts)
          assertEquals(n, M);

        for (Thread t : threads)
          t.join();
    }

    @Test
    void test3b() throws Exception {
        System.err.println("Test 3b");
        int N = 256; // number of threads
        int M = 1000; // number of messages (per thread)
    //        Channel<String> c = new SimpleChannel<>(N); // could be another channel
        Channel<String> c = new MultiChannel<>(N); // could be another channel
    //        Channel<String> c = new NoCopyMultiChannel<>(N); // could be another channel

        class Sender implements Runnable {

            final int id;

            Sender(int i) {
                id = i;
            }

            public void run() {
                String msg = String.valueOf(id);
                for (int i = 0; i < M; i++)
                    c.put(msg, id);
            }
        }

        Thread[] threads = new Thread[N];
        for (int i = 0; i < N; i++) {
            Thread t = new Thread(new Sender(i), "Sender-" + i);
            t.start();
            threads[i] = t;
        }

        int NM = N * M, count = 0;
        int[] counts = new int[N];
        while (count < NM) {
            String msg = c.get();
            if (msg != null) {
                count++;
                counts[Integer.parseInt(msg)] += 1;
            }
        }
        assertEquals(c.totalCount(), NM);
        assertNull(c.get());
        for (int n : counts)
            assertEquals(n, M);

        for (Thread t : threads)
            t.join();
    }

    @Test
    void test3c() throws Exception {
        System.err.println("Test 3c");
        int N = 256; // number of threads
        int M = 1000; // number of messages (per thread)
    //        Channel<String> c = new SimpleChannel<>(N); // could be another channel
    //        Channel<String> c = new MultiChannel<>(N); // could be another channel
        Channel<String> c = new NoCopyMultiChannel<>(N); // could be another channel

        class Sender implements Runnable {

            final int id;

            Sender(int i) {
                id = i;
            }

            public void run() {
                String msg = String.valueOf(id);
                for (int i = 0; i < M; i++)
                    c.put(msg, id);
            }
        }

        Thread[] threads = new Thread[N];
        for (int i = 0; i < N; i++) {
            Thread t = new Thread(new Sender(i), "Sender-" + i);
            t.start();
            threads[i] = t;
        }

        int NM = N * M, count = 0;
        int[] counts = new int[N];
        while (count < NM) {
            String msg = c.get();
            if (msg != null) {
                count++;
                counts[Integer.parseInt(msg)] += 1;
            }
        }
        assertEquals(c.totalCount(), NM);
        assertNull(c.get());
        for (int n : counts)
            assertEquals(n, M);

        for (Thread t : threads)
            t.join();
    }

    @Test
    void test4a() throws Exception {
        System.err.println("Test 4a");
        Channel<Object> chan = new SimpleChannel<>(2017); // could be another channel
        //    Channel<Object> chan = new MultiChannel<>(2017); // could be another channel
        //    Channel<Object> chan = new NoCopyMultiChannel<>(2017); // could be another channel
        assertEquals(chan.queueCount(), 2017);
    }

    @Test
    void test4b() throws Exception {
        System.err.println("Test 4b");
//        Channel<Object> chan = new SimpleChannel<>(2017); // could be another channel
        Channel<Object> chan = new MultiChannel<>(2017); // could be another channel
//        Channel<Object> chan = new NoCopyMultiChannel<>(2017); // could be another channel
        assertEquals(chan.queueCount(), 2017);
    }

    @Test
    void test4c() throws Exception {
        System.err.println("Test 4c");
//        Channel<Object> chan = new SimpleChannel<>(2017); // could be another channel
//        Channel<Object> chan = new MultiChannel<>(2017); // could be another channel
        Channel<Object> chan = new NoCopyMultiChannel<>(2017); // could be another channel
        assertEquals(chan.queueCount(), 2017);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    void test5a() throws Exception {
        System.err.println("Test 5a");
        Channel<Object> chan = new SimpleChannel<>(10); // could be another channel
//        Channel<Object> chan = new MultiChannel<>(10); // could be another channel
//        Channel<Object> chan = new NoCopyMultiChannel<>(10); // could be another channel
        chan.put(null, 1);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    void test5b() throws Exception {
        System.err.println("Test 5b");
//        Channel<Object> chan = new SimpleChannel<>(10); // could be another channel
        Channel<Object> chan = new MultiChannel<>(10); // could be another channel
//        Channel<Object> chan = new NoCopyMultiChannel<>(10); // could be another channel
        chan.put(null, 1);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    void test5c() throws Exception {
        System.err.println("Test 5c");
//        Channel<Object> chan = new SimpleChannel<>(10); // could be another channel
//        Channel<Object> chan = new MultiChannel<>(10); // could be another channel
        Channel<Object> chan = new NoCopyMultiChannel<>(10); // could be another channel
        chan.put(null, 1);
    }

    @Test
    void test6a() throws Exception {
        System.err.println("Test 6a");
        Channel<String> chan = new SimpleChannel<>(3); // could be MultiChannel
//        Channel<String> chan = new MultiChannel<>(3); // could be MultiChannel
//        Channel<String> chan = new NoCopyMultiChannel<>(3); // could be MultiChannel
        chan.put("foo", 0);
        chan.put("bar", 2);
        assertNotNull(chan.get());
        assertNotNull(chan.get());
    }

    @Test
    void test6b() throws Exception {
        System.err.println("Test 6b");
//        Channel<String> chan = new SimpleChannel<>(3); // could be MultiChannel
        Channel<String> chan = new MultiChannel<>(3); // could be MultiChannel
//        Channel<String> chan = new NoCopyMultiChannel<>(3); // could be MultiChannel
        chan.put("foo", 0);
        chan.put("bar", 2);
        assertNotNull(chan.get());
        assertNotNull(chan.get());
    }

//    @Test
//    void test6c() throws Exception {
//        Channel<String> chan = new SimpleChannel<>(3); // could be MultiChannel
//        Channel<String> chan = new MultiChannel<>(3); // could be MultiChannel
//        Channel<String> chan = new NoCopyMultiChannel<>(3); // could be MultiChannel
//        chan.put("foo", 0);
//        chan.put("bar", 2);
//        assertNotNull(chan.get());
//        assertNotNull(chan.get());
//    }

    @Test
    void test7() throws Exception {
        System.err.println("Test 7");
        Channel<String> chan = new NoCopyMultiChannel<>(42);
        chan.put("foo", 11);
        assertEquals(chan.totalCount(), 0);
        chan.put("bar", 37);
        assertEquals(chan.totalCount(), 0);
        while (chan.get() == null)
        assertEquals(chan.totalCount(), 0);
        assertEquals(chan.totalCount(), 1);
        while (chan.get() == null)
        assertEquals(chan.totalCount(), 1);
        assertEquals(chan.totalCount(), 2);
    }
}
