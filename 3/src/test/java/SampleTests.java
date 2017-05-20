// $Id: SampleTests.java 58 2017-03-07 20:17:15Z abcdef $

import cs735_835.noc.*;
import grading.SlowNetwork;
import org.testng.ITestNGListener;
import org.testng.TestNG;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.reporters.TextReporter;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.testng.Assert.*;

public class SampleTests {

    private ArrayList<double[]> seqStats = new ArrayList<>();
    private ArrayList<double[]> naiveStats = new ArrayList<>();
    private ArrayList<double[]> execStats = new ArrayList<>();

    /* so tests can be run from sbt: test:runMain SampleTests */
    public static void main(String[] args) {
//        TestNG testng = new TestNG();
//        testng.setTestClasses(new Class<?>[]{SampleTests.class});
//        testng.setVerbose(10);
//        testng.run();
        List<String> testFileList = new ArrayList<>();
//        testFileList.add("./testSeq.xml");
//        testFileList.add("./testExec.xml");
        testFileList.add("./testNaive.xml");
//        testFileList.add("./testSeqExec.xml");
//        testFileList.add("./testOriginal.xml");
        TestNG tng = new TestNG();
        tng.setTestSuites(testFileList);
        tng.setUseDefaultListeners(false);
        tng.addListener((ITestNGListener) new TextReporter("sample tests", 10));
        tng.run();
    }

    static class Clock implements cs735_835.noc.Clock {
        public int time;
        public int getTime() {
            return time;
        }
    }

    ExecutorService exec;

    @AfterMethod
    void shutdown() {
        if (exec != null) {
            exec.shutdownNow();
            exec = null;
        }
    }

    // Message tests

    @Test(description = "message properly initialized")
    void test1() throws Exception {
        Message m1 = new Message(111,1, 2, 3, 4, 5);
        Message m2 = new Message(222,1, 2, 3, 4, 5);
        assertNotEquals(m1, m2);
        assertEquals(m1.getId(), 111);
        assertEquals(m2.getId(), 222);
        assertEquals(m1.getId(), 111);
        assertEquals(m2.getId(), 222);
        assertEquals(m1.sourceRow(), 2);
        assertEquals(m1.sourceCol(), 3);
        assertEquals(m1.destRow(), 4);
        assertEquals(m1.destCol(), 5);
    }

    @Test(description = "tracking set and get")
    void test2() throws Exception {
        Message m1 = new Message(1,5, 4, 3, 2, 1);
        Message m2 = new Message(2,1, 2, 3, 4, 5);
        assertFalse(m1.isTracked());
        assertFalse(m2.isTracked());
        m1.setTracked(true);
        assertTrue(m1.isTracked());
        assertFalse(m2.isTracked());
        m1.setTracked(false);
        assertFalse(m1.isTracked());
        assertFalse(m2.isTracked());
    }

    @Test(description = "send and receive, single thread")
    void test3() throws Exception {
        Message m1 = new Message(1,5, 4, 3, 2, 1);
        Message m2 = new Message(2,1, 2, 3, 4, 5);
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
    void test4() throws Exception {
        Message m = new Message(42,5, 4, 3, 2, 1);
        assertEquals(m.toString(), "msg 42 (never sent)");
        m.send(11);
        assertEquals(m.toString(), "msg 42 sent by (4, 3) at 11 (never delivered)");
        m.receive(20);
        assertEquals(m.toString(), "msg 42 sent by (4, 3) at 11, delivered to (2, 1) at 20");
    }

    @Test(description = "exception (constructor)", expectedExceptions = IllegalArgumentException.class)
    void test5() throws Exception {
        new Message(1,-5, 4, 3, 2, 1);
    }

    @Test(description = "exception (receive without send)",
            expectedExceptions = IllegalStateException.class)
    void test6() throws Exception {
        Message m = new Message(1,5, 4, 3, 2, 1);
        m.receive(10);
    }

    @Test(description = "thread-safety of send", timeOut = 60000)
    void test7() throws Exception { // N threads, M messages
        exec = Executors.newCachedThreadPool();

        int N = 100;
        int M = 10_000;

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch ready = new CountDownLatch(N);
        CountDownLatch done = new CountDownLatch(N);
        AtomicInteger failures = new AtomicInteger();
        Message[] messages = new Message[M];

        for (int i = 0; i < M; i++) {
            messages[i] = new Message(i + 1, i + 1, 2, 3, 4, 5);
        }

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

        for (int i = 0; i < N; i++){
            exec.execute(new Task());
        }
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

    // Network tests

    @Test(description = "simple core processing")
    void test8() throws Exception {
        int time1 = 1;
        int time2 = 5;
        Clock clock = new Clock();
        Network net = new Network(10, 10);
        net.setClock(clock);
        Core core = net.getCore(5, 5);
        Message m1 = new Message(1,time1, 5, 5, 5, 6);
        Message m2 = new Message(2,time2, 5, 5, 6, 5);
        Message m3 = new Message(3,time1, 5, 5, 6, 6);
        m1.setTracked(true);
        m2.setTracked(true);
        m3.setTracked(true);
        assertFalse(core.isActive());
        assertFalse(net.isActive());
        net.injectMessage(m1);
        net.injectMessage(m2);
        net.injectMessage(m3);
        assertTrue(core.isActive());
        assertTrue(net.isActive());
        assertFalse(m1.hasBeenSent());
        assertFalse(m1.hasBeenReceived());
        assertFalse(m2.hasBeenSent());
        assertFalse(m2.hasBeenReceived());
        assertFalse(m3.hasBeenSent());
        assertFalse(m3.hasBeenReceived());
        int i = 0;
        while (i < time1) {
            i += 1;
            core.process();
            assertFalse(m1.hasBeenSent());
            assertFalse(m1.hasBeenReceived());
            assertFalse(m2.hasBeenSent());
            assertFalse(m2.hasBeenReceived());
            assertFalse(m3.hasBeenSent());
            assertFalse(m3.hasBeenReceived());
            clock.time++;
        }
        core.process();
        assertTrue(m1.hasBeenSent());
        assertFalse(m1.hasBeenReceived());
        assertFalse(m2.hasBeenSent());
        assertFalse(m2.hasBeenReceived());
        assertTrue(m3.hasBeenSent());
        assertFalse(m3.hasBeenReceived());
        while (i < time2) {
            i += 1;
            core.process();
            assertTrue(m1.hasBeenSent());
            assertFalse(m1.hasBeenReceived());
            assertFalse(m2.hasBeenSent());
            assertFalse(m2.hasBeenReceived());
            assertTrue(m3.hasBeenSent());
            assertFalse(m3.hasBeenReceived());
            clock.time++;
        }
        core.process();
        assertTrue(m1.hasBeenSent());
        assertFalse(m1.hasBeenReceived());
        assertTrue(m2.hasBeenSent());
        assertFalse(m2.hasBeenReceived());
        assertTrue(m3.hasBeenSent());
        assertFalse(m3.hasBeenReceived());
        assertTrue(core.isActive());
        assertTrue(net.isActive());
    }

    @Test(description = "transfer between neighboring cores")
    void test9() throws Exception {
        Clock clock = new Clock();
        Network net = new Network(10, 10);
        net.setClock(clock);
        Message m = new Message(1,1,
                5, 5, 5, 6);
        Core srcCore = net.getCore(5, 5);
        Core dstCore = net.getCore(5, 6);
        Router srcRouter = net.getRouter(5, 5);
        Router dstRouter = net.getRouter(5, 6);
        Wire wire = net.getHWire(5, 5);
        List<Wire> ws = net.allWires();
        assertFalse(net.isActive());
        assertFalse(srcCore.isActive());
        assertFalse(dstCore.isActive());
        assertFalse(srcRouter.isActive());
        assertFalse(dstRouter.isActive());
        srcCore.scheduleMessage(m);
        assertTrue(net.isActive());
        assertTrue(srcCore.isActive());
        assertFalse(dstCore.isActive());
        assertTrue(srcRouter.isActive());
        assertFalse(dstRouter.isActive());
        clock.time++;
        srcCore.process();
        assertTrue(m.hasBeenSent());
        assertFalse(m.hasBeenReceived());
        assertTrue(net.isActive());
        assertTrue(srcCore.isActive());
        assertFalse(dstCore.isActive());
        assertTrue(srcRouter.isActive());
        assertFalse(dstRouter.isActive());
        clock.time++;
        srcRouter.route();
        assertTrue(m.hasBeenSent());
        assertFalse(m.hasBeenReceived());
        assertTrue(net.isActive());
        assertFalse(srcCore.isActive());
        assertFalse(dstCore.isActive());
        assertTrue(srcRouter.isActive());
        assertFalse(dstRouter.isActive());
        wire.transfer();
        assertTrue(m.hasBeenSent());
        assertFalse(m.hasBeenReceived());
        assertTrue(net.isActive());
        assertFalse(srcCore.isActive());
        assertFalse(dstCore.isActive());
        assertFalse(srcRouter.isActive());
        assertTrue(dstRouter.isActive());
        clock.time++;
        dstRouter.route();
        assertTrue(m.hasBeenSent());
        assertTrue(m.hasBeenReceived());
        assertFalse(net.isActive());
        assertFalse(srcCore.isActive());
        assertFalse(dstCore.isActive());
        assertFalse(srcRouter.isActive());
        assertFalse(dstRouter.isActive());
        Message[] received = dstCore.receivedMessages().toArray(new Message[1]);
        assertEquals(received.length, 1);
        assertSame(received[0], m);
    }

    // simulator tests

    Simulator setSimulator(int n, int w, int h, Collection<? extends Message> messages) {
        Network network = new Network(w, h);
        messages.forEach(network::injectMessage);
        if (n == 0) {
            return new SeqSimulator(network);
        } else if (n > 0) {
            exec = Executors.newFixedThreadPool(n);
            return new ExecSimulator(network, exec, n);
        } else {
            exec = Executors.newFixedThreadPool(-n);
            return new NaiveExecSimulator(network, exec);
        }
    }

    @Test(description = "various network sizes with 1 random message")
    void test10a() {
        long start = System.nanoTime();
        test10(0);
        System.err.println(System.nanoTime() - start);
    }

    @Test(description = "various network sizes with 1 random message")
    void test10b() {
        long start = System.nanoTime();
        test10(4);
        System.err.println(System.nanoTime() - start);
    }

    @Test(description = "various network sizes with 1 random message")
    void test10c(){
        test10(-8);
    }

    void test10(int n) {
        int w = 73;
        int h = 34;
        int count = 10;
        Random rand = new Random();
        for (int i = 0; i < count; i++) {
            int time = rand.nextInt(100) + 1;
            int sr = rand.nextInt(h);
            int sc = rand.nextInt(w);
            int dr = rand.nextInt(h);
            int dc = rand.nextInt(w);
            Message m = new Message(1,time, sr, sc, dr, dc);
            //      Message m = new Message(1,10, 31, 31, 3, 31);
            Simulator sim = setSimulator(n, w, h, Collections.singletonList(m));
            List<Message> r = sim.simulate();
            if (exec != null) {
                exec.shutdown();
                exec = null;
            }
            assertEquals(r.size(), 1);
            assertSame(r.get(0), m);
            assertTrue(m.hasBeenSent());
            assertTrue(m.hasBeenReceived());
            assertEquals(m.getSendTime(), time);
            int x = (sc > dc) ? w + dc - sc : dc - sc;
            int y = (sr > dr) ? h + dr - sr : dr - sr;
            assertEquals(m.getReceiveTime(), time + x + y + 1);
        }
    }

    @Test(description = "complete simulation")
    void test11a() throws Exception {
        test11(0, "10-10-1-10");
    }

    @Test(description = "complete simulation")
    void test11b() throws Exception {
        test11(4, "10-10-1-10");
    }

    @Test(description = "complete simulation")
    void test11c() throws Exception {
        test11(-8, "10-10-1-10");
    }

    void test11(int n, String filename) throws Exception {
        String[] parts = filename.split("-");
        int w = Integer.parseInt(parts[0]);
        int h = Integer.parseInt(parts[1]);
        URL data = SampleTests.class.getResource("/" + filename + ".in");
        List<Message> in = Message.readMessagesFromURL(data);
        int firstId = in.get(0).getId();
        int[] out = new int[in.size()];
        readReceiveTimes(filename, out);
        Simulator sim = setSimulator(n, w, h, in);
        List<Message> r = sim.simulate();
        for (Message m : r) {
            assertEquals(m.getReceiveTime(), out[m.getId() - firstId], String.valueOf(m));
        }
    }

    static final Pattern OUT = Pattern.compile("msg\\s+(\\p{Digit}+).*at\\s+(\\p{Digit}+)");

    static void readReceiveTimes(String filename, int[] times) throws java.io.IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(
                SampleTests.class.getResourceAsStream("/" + filename + ".out")
        ));
        String line;
        while ((line = in.readLine()) != null) {
            Matcher match = OUT.matcher(line);
            assertTrue(match.matches(), "Cannot run test: unable to read times: " + line);
            int id = Integer.parseInt(match.group(1));
            int time = Integer.parseInt(match.group(2));
            times[id - 1] = time;
        }
    }

    @Test
    void testConcurrent1() {
        Network network = new SlowNetwork(4, 4, 1.0);
        Message m = new Message(1,1, 1, 1, 2, 3);
        network.injectMessage(m);
        exec = Executors.newFixedThreadPool(8);
        Simulator sim = new NaiveExecSimulator(network, exec);
        long time = System.nanoTime();
        sim.simulate();
        double seconds = (System.nanoTime() - time) / 1e9;
        double expected = 10;
        assertTrue(seconds <= expected + 1, // add 1 second margin
                String.format("simulation took too long: %.1f (expected less than %.1f)", seconds, expected));
    }

    @Test
    void testConcurrent2() {
        Network network = new SlowNetwork(4, 4, 1.0);
        Message m = new Message(1,1, 1, 1, 2, 3);
        network.injectMessage(m);
        exec = Executors.newFixedThreadPool(4);
        Simulator sim = new ExecSimulator(network, exec, 8);
        long time = System.nanoTime();
        sim.simulate();
        double seconds = (System.nanoTime() - time) / 1e9;
        double expected = 20;
        assertTrue(seconds <= expected + 1, // add 1 second margin
                String.format("simulation took too long: %.1f (expected less than %.1f)", seconds, expected));
    }

    @Test
    void question2Test(){
        Network network = new Network(5, 5);
        Message m1 = new Message(1, 1, 1, 1, 2, 2);
        Message m2 = new Message(2, 1, 0, 2, 2, 2);
        m1.setTracked(true);
        m2.setTracked(true);
        network.injectMessage(m1);
        network.injectMessage(m2);
        exec = Executors.newFixedThreadPool(5);
        Simulator sim = new NaiveExecSimulator(network, exec);
        sim.simulate();
    }



    @DataProvider(name = "naiveIterator")
    public Iterator<Object []> naiveProvider( ) throws InterruptedException{
        List<Object []> testCases = new ArrayList<>();
        int sizes[] = new int[]{500, 400, 300, 200, 100};
        int messages[] = new int[]{5000, 10000, 50000, 100000, 200000};
        int numThreads[] = {1, 2, 4, 8, 16, 32};
        for (int s : sizes) {
            for (int m : messages) {
                for(int threads :numThreads) {
                    testCases.add(new Object[]{s, m, threads});
                }
            }
        }
        return testCases.iterator();
    }

    @DataProvider(name = "seqIterator")
    public Iterator<Object []> seqProvider( ) throws InterruptedException{
        List<Object []> testCases = new ArrayList<>();
        int sizes[] = new int[]{100, 200, 300, 400, 500};
        int messages[] = new int[]{5000, 10000, 50000, 100000, 200000};
        for(int s : sizes){
            for(int m : messages){
                testCases.add(new Object[]{s, m});
            }
        }
        return testCases.iterator();
    }

    @DataProvider(name = "execIterator")
    public Iterator<Object []> provider( ) throws InterruptedException{
        List<Object []> testCases = new ArrayList<>();
        int sizes[] = new int[]{100, 200, 300, 400, 500};
        int messages[] = new int[]{5000, 10000, 50000, 100000, 200000};
        int threads[] = new int[]{1, 2, 4, 8, 16, 32};
        int tasks[] = new int[]{1, 2, 4, 8 , 16, 32};
        for(int s : sizes){
            for(int m :messages){
                for(int ts: tasks){
                    for(int th:threads){
                        testCases.add(new Object[]{s, m, ts, th});
                    }
                }
            }
        }
        return testCases.iterator();
    }

    @Test(dataProvider = "seqIterator", groups = "a")
    void seqTest(int size, int numMessages){
        System.err.println("Size :"+size+", #Mesages :"+numMessages);
        long seqThroughput = 0;
        long seqLatency = 0;
        int turns = 10;
        Random rand = new Random();
        try {
            for (int turn = 0; turn < turns; turn++) {
                ArrayList<Message> simMessages = new ArrayList<>();
                int id = 1;
                int time_ = 1;
                int rate = 100;
                double d = 2 / rate;
                for (int i = 0; i < numMessages; i++) {
                    time_ += rand.nextDouble() * d;
                    int srcRow = rand.nextInt(size);
                    int srcCol = rand.nextInt(size);
                    int destRow = rand.nextInt(size);
                    int destCol = rand.nextInt(size);
                    simMessages.add(new Message(id, time_, srcRow, srcCol, destRow, destCol));
                    id++;
                }

                long start;
                int numMes;
                long throughput_;
                long latency_;
                Iterator it;

                Network seQNetwork = new Network(size, size);
                simMessages.forEach(seQNetwork::injectMessage);
                Simulator seqSim = new SeqSimulator(seQNetwork);
                start = System.nanoTime();
                seqSim.simulate();
                latency_ = (System.nanoTime() - start);
                throughput_ = new Long(0);
                numMes = 0;

                for (Core c : seQNetwork.allCores()) {
                    numMes += c.receivedMessages().size();
                    for (Message m : c.receivedMessages()) {
                        throughput_ += (m.getReceiveTime() - m.getSendTime());
                    }
                }
                seqLatency += latency_;
                try {
                    seqThroughput += (throughput_ / numMes);
                } catch (Exception e) {
                    e.printStackTrace();
                    fail();
                }
            }

            double stat1 = (double) seqThroughput / (double) turns;
            double stat2 = (double) seqLatency / (double) turns /1e9;
//        System.err.println("Seq Throughput :"+stat1);
            System.err.println("Seq Latency :" + stat2);
            seqStats.add(new double[]{size, numMessages, stat1, stat2});
        }catch (Exception e){
            e.printStackTrace();
            fail();
        }
    }

    @Test(dataProvider = "execIterator", groups = "b")
    void execTest(int size, int numMessages, int numTasks, int numThreads){
        System.err.println("Size :"+size+", #Mes :"+numMessages+
                ", #Tasks: "+numTasks+", numThreads: "+numThreads);
        long ExecThroughput = 0;
        long ExecLatency = 0;
        int turns = 10;
        Random rand = new Random();
        try {
            for (int turn = 0; turn < turns; turn++) {
                long start;
                int numMes;
                long throughput_ = 0;
                long latency_;
                Iterator it;
                ArrayList<Message> execSimMessages = new ArrayList<>();
                int id = 1;
                int time_ = 1;
                int rate = 100;
                double d = 2 / rate;
                for (int i = 0; i < numMessages; i++) {
                    time_ += rand.nextDouble() * d;
                    int srcRow = rand.nextInt(size);
                    int srcCol = rand.nextInt(size);
                    int destRow = rand.nextInt(size);
                    int destCol = rand.nextInt(size);
                    execSimMessages.add(new Message(id, time_, srcRow, srcCol, destRow, destCol));
                    id++;
                }

                Network execNetwork = new Network(size, size);
                execSimMessages.forEach(execNetwork::injectMessage);
                ExecutorService execServ = Executors.newFixedThreadPool(numThreads);
                Simulator execSim = new ExecSimulator(execNetwork, execServ, numTasks);
                start = System.nanoTime();
                execSim.simulate();
                latency_ = (System.nanoTime() - start);
                numMes = 0;

                for (Core c : execNetwork.allCores()) {
                    numMes += c.receivedMessages().size();
                    for (Message m : c.receivedMessages()) {
                        throughput_ += (m.getReceiveTime() - m.getSendTime());
                    }
                }

                ExecLatency += latency_;
                try {
                    ExecThroughput += (throughput_ / numMes);
                } catch (Exception e) {
                    e.printStackTrace();
                    fail();
                }
                ExecThroughput += (throughput_ / numMes);
                ExecLatency += latency_;
                execServ.shutdown();
            }
            double stat1 = (double) ExecThroughput / (double) turns;
            double stat2 = (double) ExecLatency / (double) turns /1e9;
//        System.err.println("Exec Throughput :"+stat1);
            System.err.println("Exec Latency :" + stat2);
            execStats.add(new double[]{size, numMessages, numThreads, numTasks, stat1, stat2});
        }catch (Exception e){
            e.printStackTrace();
            fail();
        }
    }

    @Test(dataProvider = "naiveIterator", groups = "c")
    void naiveTest(int size, int numMessages, int numThreads){
        System.err.println("Size :"+size+", #Mes :"+numMessages+
                " numThreads: "+numThreads);
        int turns = 10;
        long naiveThroughput = 0;
        long naiveLatency = 0;
        Random rand = new Random();
        try {
            for (int turn = 0; turn < turns; turn++) {
                ArrayList<Message> naiveSimMessages = new ArrayList<>();
                int id = 1;
                int time_ = 1;
                int rate = 100;
                double d = 2 / rate;
                for (int i = 0; i < numMessages; i++) {
                    time_ += rand.nextDouble() * d;
                    int srcRow = rand.nextInt(size);
                    int srcCol = rand.nextInt(size);
                    int destRow = rand.nextInt(size);
                    int destCol = rand.nextInt(size);
                    naiveSimMessages.add(new Message(id, time_, srcRow, srcCol, destRow, destCol));
                    id++;
                }

                Network naiveExecNetwork = new Network(size, size);
                naiveSimMessages.forEach(naiveExecNetwork::injectMessage);
                ExecutorService naiveExecServ = Executors.newFixedThreadPool(numThreads);
                Simulator naiveSim = new NaiveExecSimulator(naiveExecNetwork, naiveExecServ);
                long throughput_ = new Long(0);
                long latency_;
                long start = System.nanoTime();
                naiveSim.simulate();
                latency_ = (System.nanoTime() - start);
                Iterator it = naiveExecNetwork.getRecTimes().entrySet().iterator();
                long numMes = 0;
                for (Core c : naiveExecNetwork.allCores()) {
                    numMes += c.receivedMessages().size();
                    for (Message m : c.receivedMessages()) {
                        throughput_ += (m.getReceiveTime() - m.getSendTime());
                    }
                }

                naiveThroughput += (throughput_ / numMes);
                naiveLatency += latency_;
                naiveExecServ.shutdown();
            }
            double stat1 = (double) naiveThroughput / (double) turns;
            double stat2 = (double) naiveLatency / (double) turns /1e9;
//        System.err.println("Naive Throughput :"+stat1);
            System.err.println("Naive Latency :" + stat2);
            naiveStats.add(new double[]{size, numMessages, numThreads, stat1, stat2});
        }catch(Exception e){
            e.printStackTrace();
            fail();
        }
    }

    @Test(groups = "A", dependsOnGroups = "a")
    void printSeq() throws IOException {
        BufferedWriter outputWriter = null;
        outputWriter = new BufferedWriter(new FileWriter("seqStats.csv"));
        for(double[] ar:seqStats){
            for(double v : ar){
                outputWriter.write(String.valueOf(v));
                outputWriter.write(" ");
            }
            outputWriter.write('\n');
        }
        outputWriter.close();
    }

    @Test(groups = "B", dependsOnGroups = "b")
    void printExec() throws IOException {
        BufferedWriter outputWriter = null;
        outputWriter = new BufferedWriter(new FileWriter("execStats.csv"));
        for(double[] ar:execStats){
            for(double v : ar){
                outputWriter.write(String.valueOf(v));
                outputWriter.write(" ");
            }
            outputWriter.write('\n');
        }
        outputWriter.close();
    }

    @Test(groups = "C", dependsOnGroups = "c")
    void printNaive() throws IOException {
        BufferedWriter outputWriter = null;
        outputWriter = new BufferedWriter(new FileWriter("naiveStats.csv"));
        for(double[] ar:naiveStats){
            for(double v : ar){
                outputWriter.write(String.valueOf(v));
                outputWriter.write(" ");
            }
            outputWriter.write('\n');
        }
        outputWriter.close();
    }

}