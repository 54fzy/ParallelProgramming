package grading;

/**
 * Created by lydakis-local on 4/22/17.
 */

import cs735_835.CountDownWheelCenter;
import cs735_835.actors.Actor;
import cs735_835.actors.ActorSystem;
import cs735_835.actors.Mailbox;
import org.testng.ITestNGListener;
import org.testng.TestNG;
import org.testng.annotations.*;
import org.testng.reporters.TextReporter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.concurrent.TimeUnit.MINUTES;

public class CountDownTests {
    public ArrayList<Stats> fixedThreadStats = new ArrayList<>();
    public ArrayList<Stats> workStealingStats = new ArrayList<>();

    public class Stats {
        private final double time;
        private final double actors;
        private final double messages;
        private final double maxMesgLen;
        private final double poolSize;

        public Stats(double t, double ps, double a, double m, double mml) {
            this.time = t;
            this.poolSize = ps;
            this.actors = a;
            this.messages = m;
            this.maxMesgLen = mml;
        }
    }

    public static void main(String[] args) {
        List<String> testFileList = new ArrayList<>();
        testFileList.add("./testFixed.xml");
        TestNG tng = new TestNG();
        tng.setTestSuites(testFileList);
        tng.setUseDefaultListeners(false);
        tng.addListener((ITestNGListener) new TextReporter("sample tests", 10));
        tng.run();
    }


    @DataProvider(name = "dataprovider")
    public Iterator<Object[]> parameters() throws InterruptedException {
        List<Object[]> testCases = new ArrayList<>();
        int threads[] = {1, 4, 16, -1, -4, -16};
        int actors[] = {10, 100, 1000};
        int messages[] = {100, 1000, 10000};
        int range[] = {100, 1000, 10000, 100000};
        for (int t : threads) {
            for (int a : actors) {
                for (int m : messages) {
                    for (int r : range) {
                        testCases.add(new Object[]{t, a, m, r});
                    }
                }
            }
        }
        return testCases.iterator();
    }

    @DataProvider(name = "dataprovider1")
    public Iterator<Object[]> parameters1() throws InterruptedException {
        List<Object[]> testCases = new ArrayList<>();
//        int threads[] = {1};
        int threads[] = {8};
//        int actors[] = {10,};
        int actors[] = {10, 100, 1000};
//        int messages[] = {100};
        int messages[] = {100, 1000, 10000};
//        int range[] = {100};
        int range[] = {100, 1000, 10000, 100000};
        for (int t : threads) {
            for (int a : actors) {
                for (int m : messages) {
                    for (int r : range) {
                        testCases.add(new Object[]{t, a, m, r});
                    }
                }
            }
        }
        return testCases.iterator();
    }

    @DataProvider(name = "dataprovider2")
    public Iterator<Object[]> parameters2() throws InterruptedException {
        List<Object[]> testCases = new ArrayList<>();
        int threads[] = {-8};
        int actors[] = {10, 100, 1000};
        int messages[] = {100, 1000, 10000};
        int range[] = {100, 1000, 10000, 100000};
        for (int t : threads) {
            for (int a : actors) {
                for (int m : messages) {
                    for (int r : range) {
                        testCases.add(new Object[]{t, a, m, r});
                    }
                }
            }
        }
        return testCases.iterator();
    }


    @Test(dataProvider = "dataprovider", groups = "a")
    void countWheelTest(int t, int a, int m, int r) throws InterruptedException {
        double avgLat = 0;
        int runs = 10;
        double sum = 0;
        for (int i = 0; i < runs; i++) {
            Random rand = new Random(2017);
            ExecutorService exec = (t > 0)
                    ? Executors.newFixedThreadPool(t)
                    : Executors.newWorkStealingPool(-t);
            ActorSystem system = new ActorSystem(exec, 64);
            Mailbox mailbox = new Mailbox("mailbox");
            Actor master = system.register(
                    new CountDownWheelCenter(a, () -> rand.nextInt(r)), "master"
            );
            System.gc();
            master.tell(m, mailbox);
            double duration = (Double) mailbox.take();
            exec.shutdown();
            if (!exec.awaitTermination(1, MINUTES)) {
                System.err.println("EXECUTOR DOES NOT TERMINATE!");
                System.exit(1);
            }
            System.out.printf("%.3f%n", duration);
            sum += duration;
        }
        avgLat = sum / runs;
        System.out.printf("average: %.3f%n", avgLat);

        if (t > 0) {
            fixedThreadStats.add(new Stats(t, a, m, r, avgLat));
        } else {
            workStealingStats.add(new Stats(t, a, m, r, avgLat));
        }
    }

    @Test(dataProvider = "dataprovider1", groups = "b")
    void countWheelTest1(int t, int a, int m, int r) throws InterruptedException {
        double avgLat = 0;
        int runs = 5;
        double sum = 0;
        for (int i = 0; i < runs; i++) {
            Random rand = new Random(2017);
            ExecutorService exec = (t > 0)
                    ? Executors.newFixedThreadPool(t)
                    : Executors.newWorkStealingPool(-t);
            ActorSystem system = new ActorSystem(exec, 64);
            Mailbox mailbox = new Mailbox("mailbox");
            Actor master = system.register(
                    new CountDownWheelCenter(a, () -> rand.nextInt(r)), "master"
            );
            System.gc();
            master.tell(m, mailbox);
            double duration = (Double) mailbox.take();
            exec.shutdown();
            if (!exec.awaitTermination(1, MINUTES)) {
                System.err.println("EXECUTOR DOES NOT TERMINATE!");
                System.exit(1);
            }
            System.out.printf("%.3f%n", duration);
            sum += duration;
        }
        avgLat = sum / runs;
        System.out.printf("average: %.3f%n", avgLat);

        if (t > 0) {
            fixedThreadStats.add(new Stats(avgLat, t, a, m, r));
        } else {
            workStealingStats.add(new Stats(avgLat, t, a, m, r));
        }
    }

    @Test(dataProvider = "dataprovider2", groups = "c")
    void countWheelTest2(int t, int a, int m, int r) throws InterruptedException {
        double avgLat = 0;
        int runs = 5;
        double sum = 0;
        for (int i = 0; i < runs; i++) {
            Random rand = new Random(2017);
            ExecutorService exec = (t > 0)
                    ? Executors.newFixedThreadPool(t)
                    : Executors.newWorkStealingPool(-t);
            ActorSystem system = new ActorSystem(exec, 64);
            Mailbox mailbox = new Mailbox("mailbox");
            Actor master = system.register(
                    new CountDownWheelCenter(a, () -> rand.nextInt(r)), "master"
            );
            System.gc();
            master.tell(m, mailbox);
            double duration = (Double) mailbox.take();
            exec.shutdown();
            if (!exec.awaitTermination(1, MINUTES)) {
                System.err.println("EXECUTOR DOES NOT TERMINATE!");
                System.exit(1);
            }
            System.out.printf("%.3f%n", duration);
            sum += duration;
        }
        avgLat = sum / runs;
        System.out.printf("average: %.3f%n", avgLat);

        if (t > 0) {
            fixedThreadStats.add(new Stats(avgLat, t, a, m, r));
        } else {
            workStealingStats.add(new Stats(avgLat, t, a, m, r));
        }
    }

    @Test(dependsOnGroups = "a", groups = "A")
    void printStats() throws IOException {
        BufferedWriter outputWriter;
        outputWriter = new BufferedWriter(new FileWriter("~/fixedPool.csv"));
        outputWriter.write("Latency Threads Actors Messages MaxLength\n");
        for (Stats s : fixedThreadStats) {
            outputWriter.write(s.time + " " + s.poolSize +
                    " " + s.actors + " " + s.messages + " " + s.maxMesgLen + "\n");
        }
        outputWriter.close();
        outputWriter = new BufferedWriter(new FileWriter("stealingPool.csv"));
        outputWriter.write("Latency Threads Actors Messages MaxLength\n");
        for (Stats s : workStealingStats) {
            outputWriter.write(s.time + " " + s.poolSize +
                    " " + s.actors + " " + s.messages + " " + s.maxMesgLen + "\n");
        }

        outputWriter.close();
    }

    @Test(dependsOnGroups = "b", groups = "B")
    void printStats1() throws IOException {
        BufferedWriter outputWriter;
        outputWriter = new BufferedWriter(new FileWriter("/home/lydakis-local/fixedPool.csv"));
        outputWriter.write("Latency Threads Actors Messages MaxLength\n");
        for (Stats s : fixedThreadStats) {
            outputWriter.write(s.time + " " + s.poolSize +
                    " " + s.actors + " " + s.messages + " " + s.maxMesgLen + "\n");
        }
        outputWriter.close();
        System.err.println("Done");
    }

    @Test(dependsOnGroups = "c", groups = "C")
    void printStats2() throws IOException {
        BufferedWriter outputWriter;
        outputWriter = new BufferedWriter(new FileWriter("/home/lydakis-local/stealingPool.csv"));
        outputWriter.write("Latency Threads Actors Messages MaxLength\n");
        for (Stats s : workStealingStats) {
            outputWriter.write(s.time + " " + s.poolSize +
                    " " + s.actors + " " + s.messages + " " + s.maxMesgLen + "\n");
        }
        outputWriter.close();
    }
}

