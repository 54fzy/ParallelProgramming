import cs735_835.channels.Channel;
import cs735_835.channels.MultiChannel;
import cs735_835.channels.NoCopyMultiChannel;
import cs735_835.channels.SimpleChannel;
import org.apache.tools.ant.taskdefs.Java;
import org.testng.ITestNGListener;
import org.testng.TestListenerAdapter;
import org.testng.TestNG;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.reporters.TextReporter;
import org.testng.xml.*;

import java.io.*;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Created by lydakis-local on 1/31/17.
 */
public class ChannelPerf {

    public class Stats{
        int numThreads;
        int numMessages;
        double maxGetTime;
        double minGetTime;
        double avgGetTime;
        double stdGetTime;
        int maxNumNullGets;
        int minNumNullGets;
        double avgNumNullGets;
        double stdNumNullGets;
        double maxPutTime;
        double minPutTime;
        double avgPutTime;
        double stdPutTime;
        double throughput;
        double latency;
        double readWriteRatio;
        int numQueues;
        long total;

        public Stats(int nt, int nm, double tp, double lat,
                          double gtmax, double gtmin, double gtavg, double gtstd,
                          int nlgmax, int nlgmin, double nlgavg, double nlgstd,
                          double ptmax, double ptmin, double ptavg, double ptstd){
            this.numThreads = nt;
            this.numMessages = nm;
            this.throughput = tp;
            this.latency = lat;
            this.maxGetTime = gtmax;
            this.minGetTime = gtmin;
            this.avgGetTime = gtavg;
            this.maxGetTime = gtmax;
            this.stdGetTime = gtstd;
            this.maxNumNullGets = nlgmax;
            this.minNumNullGets = nlgmin;
            this.avgNumNullGets = nlgavg;
            this.stdNumNullGets = nlgstd;
            this.maxPutTime = ptmax;
            this.minPutTime = ptmin;
            this.avgPutTime = ptavg;
            this.stdPutTime = ptstd;
        }
        public Stats(
                int nt, int nm, double tp, double lat,
                double gtmax, double gtmin, double gtavg, double gtstd,
                int nlgmax, int nlgmin, double nlgavg, double nlgstd,
                double ptmax, double ptmin, double ptavg, double ptstd,
                double rwr, int q){
            this.numThreads = nt;
            this.numMessages = nm;
            this.throughput = tp;
            this.latency = lat;
            this.maxGetTime = gtmax;
            this.minGetTime = gtmin;
            this.avgGetTime = gtavg;
            this.maxGetTime = gtmax;
            this.stdGetTime = gtstd;
            this.maxNumNullGets = nlgmax;
            this.minNumNullGets = nlgmin;
            this.avgNumNullGets = nlgavg;
            this.stdNumNullGets = nlgstd;
            this.maxPutTime = ptmax;
            this.minPutTime = ptmin;
            this.avgPutTime = ptavg;
            this.stdPutTime = ptstd;
            this.readWriteRatio = rwr;
            this.numQueues = q;
        }

        public Stats(
                int nt, int nm, double tp, double lat,
                double gtmax, double gtmin, double gtavg, double gtstd,
                int nlgmax, int nlgmin, double nlgavg, double nlgstd,
                double ptmax, double ptmin, double ptavg, double ptstd,
                double rwr, int q, long total){
            this.numThreads = nt;
            this.numMessages = nm;
            this.throughput = tp;
            this.latency = lat;
            this.maxGetTime = gtmax;
            this.minGetTime = gtmin;
            this.avgGetTime = gtavg;
            this.maxGetTime = gtmax;
            this.stdGetTime = gtstd;
            this.maxNumNullGets = nlgmax;
            this.minNumNullGets = nlgmin;
            this.avgNumNullGets = nlgavg;
            this.stdNumNullGets = nlgstd;
            this.maxPutTime = ptmax;
            this.minPutTime = ptmin;
            this.avgPutTime = ptavg;
            this.stdPutTime = ptstd;
            this.readWriteRatio = rwr;
            this.numQueues = q;
            this.total = total;
        }
    }

    private ArrayList<Stats> Test1Results = new ArrayList<>();
    private ArrayList<Stats> Test2Results = new ArrayList<>();
    private ArrayList<Stats> Test3Results = new ArrayList<>();

    public static void main(String[] args) throws InterruptedException {
        Thread.sleep(5000);
        List<String> testFileList = new ArrayList<>();
//        testFileList.add("./testSimple.xml");
//        testFileList.add("./testMulti.xml");
//        testFileList.add("./testNoCopy.xml");
        testFileList.add("./testExtra.xml");
        TestNG tng = new TestNG();
        tng.setTestSuites(testFileList);
        tng.setUseDefaultListeners(false);
        tng.addListener((ITestNGListener) new TextReporter("sample tests", 10));
        tng.run();
    }

    @DataProvider(name = "test1Iterator")
    public Iterator<Object []> provider( ) throws InterruptedException
    {
        List<Object []> testCases = new ArrayList<>();
        int threads_power = 10;
        int messageBatches = 11;
        int messageIncrement = 1000;

//        int threads_power = 2;
//        int messageBatches = 2;
//        int messageIncrement = 2;

        double[] rwr_ = new double[]{0.3, 0.5, 0.8};
//        Object[][] ret = new Object[total][2];
        for(int j = 1 ; j < messageBatches;j++){
            for(double rwr : rwr_){
//            for(double rwr = 0.3; rwr < 1.0; rwr+=0.3){
                for(int i = 0 ; i < threads_power ; i++) {
                    testCases.add(new Object[]{(int) java.lang.Math.pow(2, i), j * messageIncrement, rwr});
                }
            }
        }
        return testCases.iterator();
    }

    @DataProvider(name = "test2Iterator")
    public Iterator<Object []> provider2( ) throws InterruptedException
    {
        List<Object []> testCases = new ArrayList<>();
        int threads_power = 10;
        int messageBatches = 11;
        int messageIncrement = 1000;
        int numQues = 1001;
        int queueIncrement = 100;

//        int threads_power = 2;
//        int messageBatches = 2;
//        int messageIncrement = 2;
//        int numQues = 1001;
//        int queueIncrement = 500;

        double[] rwr_ = new double[]{0.3, 0.5, 0.8};
        for(int j = 1 ; j < messageBatches;j++){
            for(double rwr : rwr_){
//            for(double rwr = 0.31; rwr < 1.0; rwr+=0.3){
//                for(int k = 1; k < numQues;k+=queueIncrement) {
                    for(int i = 1 ; i < threads_power ; i++) {
                        int pow = (int) java.lang.Math.pow(2, i);
                        testCases.add(new Object[]{pow, j * messageIncrement, 1, rwr});
                        testCases.add(new Object[]{pow, j * messageIncrement, pow/2, rwr});
                        testCases.add(new Object[]{pow, j * messageIncrement, pow, rwr});
                        testCases.add(new Object[]{pow, j * messageIncrement, pow*2, rwr});
                    }
//                }
            }
        }
        return testCases.iterator();
    }

    @Test(
            dataProvider = "test1Iterator"
            , groups = {"a"}
    )
    void TestSimpleChannel (int threads, int messages, double rwr) throws Exception{
        System.err.println("TestSimpleChannel "+ threads +" threads "+messages+" messages.");
        System.err.println("SimpleChannel test :");
        System.err.println("Threads: " + threads);
        System.err.println("Messages: " + messages);
        System.err.println("Queues: 1");
        System.err.println("RWR: " + rwr);
        Channel<Integer> c = new SimpleChannel(1);
        Thread[] threadList = new Thread[threads];
        assertNull(c.get());
        assertEquals(1, c.queueCount());
        Random random = new Random();

        int totalMessages = threads*messages;

        double throughput = 0.0;
        double latency = 0.0;

        double getThroughput = 0.0;
        double getLatency = 0.0;

        double puttThroughput = 0.0;
        double puttLatency = 0.0;

        double totalGetTime = 0.0;
        double totalPutTime = 0.0;

        int expectedVal = 0;
        for (int i = 0; i<threads;i++){
            for(int j = 0; j<messages;j++){
                expectedVal += i;
            }
        }

        double maxPutTime = 0.0;
        double minPutTime = 999999999999999.0;
        double avgPutTime = 0.0;
        double stdPutTime = 0.0;

        double maxGetTime = 0.0;
        double minGetTime = 999999999999999.0;
        double avgGetTime = 0.0;
        double stdGetTime = 0.0;

        int nullGets = 0;
        int maxNullGets = 0;
        int minNullGets = 999999999;
        int avgNullGets = 0;
        int stdNullGets = 0;

        int maxThroughput = 0;
        int minThroughput = 999999999;
        int avgThroughput = 0;
        int stdThroughput = 0;

        int maxLatency = 0;
        int minLatency = 999999999;
        int avgLatency = 0;
        int stdLatency = 0;

        long totalTime = 0;

        int[] counts_ar = new int[threads];

        class Sender implements Runnable {

            double getThroughput = 0.0;
            double getLatency = 0.0;

            double putThroughput = 0.0;
            double putLatency = 0.0;

            double totalGetTime = 0.0;
            double totalPutTime = 0.0;

            double maxPutTime = 0.0;
            double minPutTime = 999999999999999999999999.0;
            double avgPutTime = 0.0;
            double stdPutTime = 0.0;

            double maxGetTime = 0.0;
            double minGetTime = 999999999999999999999999.0;
            double avgGetTime = 0.0;
            double stdGetTime = 0.0;

            final int id;

            int count = 0;

            long numPuts = 0;

            long numGets = 0;

            long nullGets = 0;

            long starTime, endTime, dur, totalTime = 0;

            int totalVal = 0;

            Sender(int id_) {
                id = id_;
            }

            public void run() {
                String msg = String.valueOf(id);
//                for (int i = 0; i < messages; i++) {
                while( numPuts < messages && count < messages){
                    if(random.nextDouble() < rwr) { //PUT
                        starTime = System.nanoTime();
//                        c.put(msg, 0);
                        c.put(id, 0);
                        endTime = System.nanoTime();
                        dur = endTime - starTime;
                        maxPutTime = (dur > maxPutTime) ? dur : maxPutTime;
                        minPutTime = (dur < minPutTime) ? dur : minPutTime;
                        avgPutTime += dur;
                        numPuts++;
                    }else{ //TRY TO READ
                        starTime = System.nanoTime();
                        Integer ret  = c.get();
                        endTime = System.nanoTime();
                        dur = endTime - starTime;
//                    System.err.println("Sender " + id +" Got " + ret+" after "+count+" successful gets");
                        maxGetTime = (dur>maxGetTime)?dur:maxGetTime;
                        minGetTime = (dur<minGetTime)?dur:minGetTime;
                        avgGetTime += dur;
                        if (ret != null) {
                            totalVal += ret;
                            numGets++;
                            this.count++;
                        }else{
                            nullGets++;
                        }
                    }
                }
//                System.err.println("Sender " + id +" has "+numPuts+" "+count);
                if(count<messages){
                    while(this.count<messages){
                        starTime = System.nanoTime();
//                        String ret =c.get();
                        Integer ret =c.get();
                        endTime = System.nanoTime();
                        dur = endTime - starTime;
//                    System.err.println("Sender " + id +" Got " + ret+" after "+count+" successful gets");
                        maxGetTime = (dur>maxGetTime)?dur:maxGetTime;
                        minGetTime = (dur<minGetTime)?dur:minGetTime;
                        avgGetTime += dur;
                        if (ret != null) {
//                            System.err.println("Sender " + id +" read "+ret);
                            numGets++;
                            this.count++;
                            totalVal += ret;
                        }else{
                            nullGets++;
                        }
                    }
                }else{
                    while(numPuts<messages){
                        starTime = System.nanoTime();
                        c.put(id, 0);
//                        c.put(msg, 0);
                        endTime = System.nanoTime();
                        dur = endTime - starTime;
                        maxPutTime = (dur > maxPutTime) ? dur : maxPutTime;
                        minPutTime = (dur < minPutTime) ? dur : minPutTime;
                        avgPutTime += dur;
                        numPuts++;
                    }
                }
//                System.err.println("Sender " + id +" PutsSoFar " + numPuts + " Counts " + count+ " "+this.count);

//                System.err.println("Sender " + id +" finished "+numPuts+" "+count);
                totalTime += (avgGetTime+avgPutTime);
                avgGetTime = avgGetTime/(numGets+nullGets);
                avgPutTime = avgPutTime/numPuts;
            }
        }

        int counts = 0;
        int totalCount = 0;

//        Random rn = new Random();
//        int answer = rn.nextInt(10) + 1;

        Sender[] senders = new Sender[threads];
        for(int i = 0 ; i < threads; i++){
            senders[i] = new Sender(i);
            threadList[i] = new Thread(senders[i], "Sender-"+i);
        }
        for(int i = 0 ; i < threads; i++){
            threadList[i].start();
        }
        for(int i = 0 ; i < threads; i++){
            threadList[i].join();
        }

        for(int i = 0 ; i < threads; i++){
            expectedVal -= senders[i].totalVal;
            totalCount += senders[i].count;
            maxPutTime = (senders[i].maxPutTime>maxPutTime)?senders[i].maxPutTime:maxPutTime;
            minPutTime = (senders[i].minPutTime<minPutTime)?senders[i].minPutTime:minPutTime;
            maxGetTime = (senders[i].maxGetTime>maxGetTime)?senders[i].maxPutTime:maxGetTime;
            minGetTime = (senders[i].minGetTime<minGetTime)?senders[i].minPutTime:minGetTime;
            avgGetTime += senders[i].avgGetTime;
            avgPutTime += senders[i].avgPutTime;
            totalTime += senders[i].totalTime;
            nullGets += senders[i].nullGets;
//            System.err.println("ID: "+senders[i].id+", Count: "+senders[i].count+", numPuts "
//                    +senders[i].numPuts+", avgPutTime "+senders[i].avgPutTime+", numGets: "+senders[i].numGets
//                    +", avgGetTime " +senders[i].avgGetTime+", nullGets: "+senders[i].nullGets);
        }

        avgGetTime = avgGetTime/threads;
        avgPutTime = avgPutTime/threads;

        for(int i = 0 ; i < threads; i++){
            stdGetTime += (senders[i].avgGetTime - avgGetTime)*(senders[i].avgGetTime - avgGetTime);
            stdPutTime += (senders[i].avgPutTime - avgPutTime)*(senders[i].avgPutTime - avgPutTime);
            assertEquals(senders[i].numPuts + senders[i].numGets, 2*messages);
        }

        stdGetTime = java.lang.Math.sqrt(stdGetTime/threads);
        stdPutTime = java.lang.Math.sqrt(stdPutTime/threads);

        throughput = (double)(2*messages*threads)/(double)(totalTime/1000);
//        latency = (double)(2*messages*threads)/(double)(totalTime/1000);
        latency = (double)(totalTime/1000)/(double)(2*messages*threads);

        Test1Results.add(new Stats(threads, messages, throughput, latency,
                maxGetTime/1000, minGetTime/1000, avgGetTime/1000, stdGetTime/1000,
                maxNullGets, minNullGets, avgNullGets, stdNullGets,
                maxPutTime/1000, minPutTime/1000, avgPutTime/1000, stdPutTime/1000,
                rwr, 1
        ));
        System.err.println("Threads: "+threads+", Messages: "+messages+", Count: "+totalCount+
                ", avgPutTime "+avgPutTime/1000+
                ", avgGetTime " +avgGetTime/1000+
                ", nullGets: "+nullGets+
                ", throughput: "+throughput+
                ", latency: "+latency);
//        System.err.println(totalCount+" "+totalMessages);
        assertEquals(totalCount, totalMessages);
        assertEquals(expectedVal, 0);
        assertNull(c.get());
    }

    @Test(
            dataProvider = "test2Iterator"
//            , dependsOnGroups="a"
            , groups = {"b"}
    )
    void TestMultiChannel(int threads, int messages, int queueCount, double rwr) throws InterruptedException {
        Channel c = new MultiChannel<Integer>(queueCount);
        assertNull(c.get());
        assertEquals(queueCount, c.queueCount());
        int totalMessages = threads*messages;
        int totalCount = 0;
        Random rand = new Random();
        System.err.println("Multichannel test :");
        System.err.println("Threads: " + threads);
        System.err.println("Messages: " + messages);
        System.err.println("Queues: " + queueCount);
        System.err.println("RWR: " + rwr);

        double throughput = 0.0;
        double latency = 0.0;

        double maxPutTime = 0.0;
        double minPutTime = 999999999999999.0;
        double avgPutTime = 0.0;
        double stdPutTime = 0.0;

        double maxGetTime = 0.0;
        double minGetTime = 999999999999999.0;
        double avgGetTime = 0.0;
        double stdGetTime = 0.0;

        int nullGets = 0;
        int maxNullGets = 0;
        int minNullGets = 999999999;
        int avgNullGets = 0;
        int stdNullGets = 0;

        int maxThroughput = 0;
        int minThroughput = 999999999;
        int avgThroughput = 0;
        int stdThroughput = 0;

        int maxLatency = 0;
        int minLatency = 999999999;
        int avgLatency = 0;
        int stdLatency = 0;

        long totalTime = 0;

        Thread[] threadList = new Thread[threads];
        int expectedVal = 0;
        for (int i = 0; i<threads;i++){
            for(int j = 0; j<messages;j++){
                expectedVal += i;
            }
        }
        class Sender implements Runnable {

            double maxPutTime = 0.0;
            double minPutTime = 999999999999999999999999.0;
            double avgPutTime = 0.0;
            double stdPutTime = 0.0;

            double maxGetTime = 0.0;
            double minGetTime = 999999999999999999999999.0;
            double avgGetTime = 0.0;
            double stdGetTime = 0.0;

            final int id;

            int count = 0;

            long numPuts = 0;

            long numGets = 0;

            long nullGets = 0;

            long starTime, endTime, dur, totalTime = 0;

            int totalVal = 0;

            Sender(int id_) {
                id = id_;
            }
            public void run() {
                while( numPuts < messages && count < messages){
                    if(rand.nextDouble() < rwr) { //PUT
                        starTime = System.nanoTime();
                        int q = rand.nextInt(queueCount);
                        c.put(id, q);
                        endTime = System.nanoTime();
                        dur = endTime - starTime;
                        maxPutTime = (dur > maxPutTime) ? dur : maxPutTime;
                        minPutTime = (dur < minPutTime) ? dur : minPutTime;
                        avgPutTime += dur;
                        numPuts++;
                    }else{ //TRY TO READ
                        starTime = System.nanoTime();
                        Integer ret  = (Integer) c.get();
//                        String ret  = c.get();
                        endTime = System.nanoTime();
                        dur = endTime - starTime;
                        maxGetTime = (dur>maxGetTime)?dur:maxGetTime;
                        minGetTime = (dur<minGetTime)?dur:minGetTime;
                        avgGetTime += dur;
                        if (ret != null) {
                            totalVal += ret;
                            numGets++;
                            this.count++;
                        }else{
                            nullGets++;
                        }
                    }
                }
                if(count<messages){
                    while(this.count<messages){
                        starTime = System.nanoTime();
//                        String ret =c.get();
                        Integer ret = (Integer) c.get();
                        endTime = System.nanoTime();
                        dur = endTime - starTime;
//                    System.err.println("Sender " + id +" Got " + ret+" after "+count+" successful gets");
                        maxGetTime = (dur>maxGetTime)?dur:maxGetTime;
                        minGetTime = (dur<minGetTime)?dur:minGetTime;
                        avgGetTime += dur;
                        if (ret != null) {
//                            System.err.println("Sender " + id +" read "+ret);
                            numGets++;
                            this.count++;
                            totalVal += ret;
                        }else{
                            nullGets++;
                        }
                    }
                }else{
                    while(numPuts<messages){
                        starTime = System.nanoTime();
                        int q = rand.nextInt(queueCount);
                        c.put(id, q);
//                        c.put(msg, 0);
                        endTime = System.nanoTime();
                        dur = endTime - starTime;
                        maxPutTime = (dur > maxPutTime) ? dur : maxPutTime;
                        minPutTime = (dur < minPutTime) ? dur : minPutTime;
                        avgPutTime += dur;
                        numPuts++;
                    }
                }
                totalTime += (avgGetTime+avgPutTime);
                avgGetTime = avgGetTime/(numGets+nullGets);
                avgPutTime = avgPutTime/numPuts;
            }
        }

        Sender[] senders = new Sender[threads];
        for(int i = 0 ; i < threads; i++){
            senders[i] = new Sender(i);
            threadList[i] = new Thread(senders[i], "Sender-"+i);
        }
        for(int i = 0 ; i < threads; i++){
            threadList[i].start();
        }
        for(int i = 0 ; i < threads; i++){
            threadList[i].join();
        }

        for(int i = 0 ; i < threads; i++){
            expectedVal -= senders[i].totalVal;
            totalCount += senders[i].count;
            maxPutTime = (senders[i].maxPutTime>maxPutTime)?senders[i].maxPutTime:maxPutTime;
            minPutTime = (senders[i].minPutTime<minPutTime)?senders[i].minPutTime:minPutTime;
            maxGetTime = (senders[i].maxGetTime>maxGetTime)?senders[i].maxPutTime:maxGetTime;
            minGetTime = (senders[i].minGetTime<minGetTime)?senders[i].minPutTime:minGetTime;
            avgGetTime += senders[i].avgGetTime;
            avgPutTime += senders[i].avgPutTime;
            totalTime += senders[i].totalTime;
            nullGets += senders[i].nullGets;
//            System.err.println("ID: "+senders[i].id+", Count: "+senders[i].count+", numPuts "
//                    +senders[i].numPuts+", avgPutTime "+senders[i].avgPutTime+", numGets: "+senders[i].numGets
//                    +", avgGetTime " +senders[i].avgGetTime+", nullGets: "+senders[i].nullGets);
        }

        avgGetTime = avgGetTime/threads;
        avgPutTime = avgPutTime/threads;

        for(int i = 0 ; i < threads; i++){
            stdGetTime += (senders[i].avgGetTime - avgGetTime)*(senders[i].avgGetTime - avgGetTime);
            stdPutTime += (senders[i].avgPutTime - avgPutTime)*(senders[i].avgPutTime - avgPutTime);
        }

        stdGetTime = java.lang.Math.sqrt(stdGetTime/threads);
        stdPutTime = java.lang.Math.sqrt(stdPutTime/threads);

        throughput = (double)(2*messages*threads)/(double)(totalTime/1000);
//        latency = (double)(2*messages*threads)/(double)(totalTime/1000);
        latency = (double)(totalTime/1000)/(double)(2*messages*threads);

        Test2Results.add(new Stats(threads, messages, throughput, latency,
                maxGetTime/1000, minGetTime/1000, avgGetTime/1000, stdGetTime/1000,
                maxNullGets, minNullGets, avgNullGets, stdNullGets,
                maxPutTime/1000, minPutTime/1000, avgPutTime/1000, stdPutTime/1000,
                rwr, queueCount)
        );
        System.err.println("Threads: "+threads+", Messages: "+messages+", Count: "+totalCount+
                ", avgPutTime "+avgPutTime/1000+
                ", avgGetTime " +avgGetTime/1000+
                ", nullGets: "+nullGets+
                ", throughput: "+throughput+
                ", latency: "+latency);
//        System.err.println(totalCount+" "+totalMessages);
        assertEquals(totalCount, totalMessages);
        assertEquals(expectedVal, 0);
        assertNull(c.get());
    }

    @Test(dataProvider = "test2Iterator"
//            , dependsOnGroups = "b"
            , groups={"c"}
    )
    void TestNoCopyMultiChannel(int threads, int messages, int queueCount, double rwr) throws InterruptedException {
        Channel c = new NoCopyMultiChannel<Integer>(queueCount);
        assertNull(c.get());
        assertEquals(queueCount, c.queueCount());
        int totalMessages = threads*messages;
        int totalCount = 0;
        Random rand = new Random();
        System.err.println("NoCopyMultichannel test :");
        System.err.println("Threads: " + threads);
        System.err.println("Messages: " + messages);
        System.err.println("Queues: " + queueCount);
        System.err.println("RWR: " + rwr);
        System.err.println("Total Messages: " + totalMessages);

        double throughput = 0.0;
        double latency = 0.0;

        double maxPutTime = 0.0;
        double minPutTime = 999999999999999.0;
        double avgPutTime = 0.0;
        double stdPutTime = 0.0;

        double maxGetTime = 0.0;
        double minGetTime = 999999999999999.0;
        double avgGetTime = 0.0;
        double stdGetTime = 0.0;

        int nullGets = 0;
        int maxNullGets = 0;
        int minNullGets = 999999999;
        int avgNullGets = 0;
        int stdNullGets = 0;

        int maxThroughput = 0;
        int minThroughput = 999999999;
        int avgThroughput = 0;
        int stdThroughput = 0;

        int maxLatency = 0;
        int minLatency = 999999999;
        int avgLatency = 0;
        int stdLatency = 0;

        long totalTime = 0;

        Thread[] threadList = new Thread[threads];
        int expectedVal = 0;
        for (int i = 0; i<threads;i++){
            for(int j = 0; j<messages;j++){
                expectedVal += i;
            }
        }
        class Sender implements Runnable {

            double maxPutTime = 0.0;
            double minPutTime = 999999999999999999999999.0;
            double avgPutTime = 0.0;
            double stdPutTime = 0.0;

            double maxGetTime = 0.0;
            double minGetTime = 999999999999999999999999.0;
            double avgGetTime = 0.0;
            double stdGetTime = 0.0;

            final int id;

            long count = 0;

            long numPuts = 0;

            long numGets = 0;

            long nullGets = 0;

            long starTime, endTime, dur, totalTime = 0;

            int totalVal = 0;

            Sender(int id_) {
                id = id_;
            }
            public void run() {
                while( numPuts < messages && numGets < messages){
                    if(rand.nextDouble() < rwr) { //PUT
//                        System.err.println("Sender "+id+" trying to put "+ numPuts +" "+ numGets);
                        starTime = System.nanoTime();
                        int q = rand.nextInt(queueCount);
                        c.put(id, q);
                        endTime = System.nanoTime();
                        dur = endTime - starTime;
                        maxPutTime = (dur > maxPutTime) ? dur : maxPutTime;
                        minPutTime = (dur < minPutTime) ? dur : minPutTime;
                        avgPutTime += dur;
                        numPuts++;
                    }else{ //TRY TO READ
//                        System.err.println("Sender "+id+" trying to get "+ numPuts +" "+ numGets);
                        starTime = System.nanoTime();
                        Integer ret  = (Integer) c.get();
//                        String ret  = c.get();
                        endTime = System.nanoTime();
                        dur = endTime - starTime;
                        maxGetTime = (dur>maxGetTime)?dur:maxGetTime;
                        minGetTime = (dur<minGetTime)?dur:minGetTime;
                        avgGetTime += dur;
                        if (ret != null) {
                            totalVal += ret;
                            numGets++;
                        }else{
                            nullGets++;
                        }
                    }
                }
                if(numGets<messages){
                    while(numGets<messages){
                        starTime = System.nanoTime();
//                        String ret =c.get();
                        Integer ret = (Integer) c.get();
                        endTime = System.nanoTime();
                        dur = endTime - starTime;

                        maxGetTime = (dur>maxGetTime)?dur:maxGetTime;
                        minGetTime = (dur<minGetTime)?dur:minGetTime;
                        avgGetTime += dur;
                        if (ret != null) {
//                            System.err.println("Sender " + id +" Got " + ret+" after "+numPuts+" numPuts, "+numGets+" numGets");
//                            System.err.println("Sender " + id +" read "+ret);
                            numGets++;
                            totalVal += ret;
                        }else{
                            nullGets++;
                        }
                    }
                }else{
                    while(numPuts<messages){
                        starTime = System.nanoTime();
                        int q = rand.nextInt(queueCount);
                        c.put(id, q);
//                        c.put(msg, 0);
                        endTime = System.nanoTime();
                        dur = endTime - starTime;
                        maxPutTime = (dur > maxPutTime) ? dur : maxPutTime;
                        minPutTime = (dur < minPutTime) ? dur : minPutTime;
                        avgPutTime += dur;
                        numPuts++;
                    }
                }
                count = numGets;
//                System.err.println("Sender " + id +" Got "+count+" numGets, "+numPuts+" puts");
//                System.err.println(c.totalCount());
                totalTime += (avgGetTime+avgPutTime);
                avgGetTime = avgGetTime/(numGets+nullGets);
                avgPutTime = avgPutTime/numPuts;
            }
        }

        Sender[] senders = new Sender[threads];
        for(int i = 0 ; i < threads; i++){
            senders[i] = new Sender(i);
            threadList[i] = new Thread(senders[i], "Sender-"+i);
        }
        for(int i = 0 ; i < threads; i++){
            threadList[i].start();
        }
        for(int i = 0 ; i < threads; i++){
            threadList[i].join();
        }

        for(int i = 0 ; i < threads; i++){
            expectedVal -= senders[i].totalVal;
            totalCount += senders[i].count;
            maxPutTime = (senders[i].maxPutTime>maxPutTime)?senders[i].maxPutTime:maxPutTime;
            minPutTime = (senders[i].minPutTime<minPutTime)?senders[i].minPutTime:minPutTime;
            maxGetTime = (senders[i].maxGetTime>maxGetTime)?senders[i].maxGetTime:maxGetTime;
            minGetTime = (senders[i].minGetTime<minGetTime)?senders[i].minGetTime:minGetTime;
            avgGetTime += senders[i].avgGetTime;
            avgPutTime += senders[i].avgPutTime;
            totalTime += senders[i].totalTime;
            nullGets += senders[i].nullGets;
//            System.err.println("ID: "+senders[i].id+", Count: "+senders[i].count+", numPuts "
//                    +senders[i].numPuts+", avgPutTime "+senders[i].avgPutTime+", numGets: "+senders[i].numGets
//                    +", avgGetTime " +senders[i].avgGetTime+", nullGets: "+senders[i].nullGets);
        }

        avgGetTime = avgGetTime/threads;
        avgPutTime = avgPutTime/threads;

        for(int i = 0 ; i < threads; i++){
            stdGetTime += (senders[i].avgGetTime - avgGetTime)*(senders[i].avgGetTime - avgGetTime);
            stdPutTime += (senders[i].avgPutTime - avgPutTime)*(senders[i].avgPutTime - avgPutTime);
        }

        stdGetTime = java.lang.Math.sqrt(stdGetTime/threads);
        stdPutTime = java.lang.Math.sqrt(stdPutTime/threads);

        throughput = (double)(2*messages*threads)/(double)(totalTime/1000);
//        latency = (double)(2*messages*threads)/(double)(totalTime/1000);
        latency = (double)(totalTime/1000)/(double)(2*messages*threads);

        Test3Results.add(new Stats(threads, messages, throughput, latency,
                maxGetTime/1000, minGetTime/1000, avgGetTime/1000, stdGetTime/1000,
                maxNullGets, minNullGets, avgNullGets, stdNullGets,
                maxPutTime/1000, minPutTime/1000, avgPutTime/1000, stdPutTime/1000,
                rwr, queueCount)
        );
        System.err.println("Threads: "+threads+", Messages: "+messages+", Count: "+totalCount+
                ", avgPutTime "+avgPutTime/1000+
                ", avgGetTime " +avgGetTime/1000+
                ", nullGets: "+nullGets+
                ", throughput: "+throughput+
                ", latency: "+latency);
//        System.err.println(totalCount+" "+totalMessages);
        assertEquals(totalCount, totalMessages);
        assertEquals(expectedVal, 0);
        assertNull(c.get());
        for(int i = 0;i< queueCount;i++){
            assertNull(c.get());
        }
    }


    @Test(
            dataProvider = "test2Iterator"
            ,groups={"extra"}
    )
    void TestSingle(int threads, int messages, int queueCount, double rwr) throws InterruptedException{
        Channel c1 = new SimpleChannel<Integer>(queueCount);
        Channel c2 = new MultiChannel<Integer>(queueCount);
        Channel c3 = new NoCopyMultiChannel<Integer>(queueCount);
        assertNull(c1.get());
        assertNull(c2.get());
        assertNull(c3.get());
        assertEquals(queueCount, c1.queueCount());
        assertEquals(queueCount, c2.queueCount());
        assertEquals(queueCount, c3.queueCount());

        int totalMessages = threads*messages;
        int totalCount = 0;

        Random rand = new Random();
        System.err.println("Single: "+"T: " + threads+" M: " + messages+" Q: " + queueCount+" RWR: " + rwr);

        Thread[] threadList = new Thread[threads];
        int expectedVal = 0;
        for (int i = 0; i<threads;i++){
            for(int j = 0; j<messages;j++){
                expectedVal += i;
            }
        }

        class Reader implements Runnable {

            private Channel c;

            double maxGetTime = 0.0;
            double minGetTime = 999999999999999999999999.0;
            double avgGetTime = 0.0;

            final int id;

            long numGets = 0;

            int nullGets = 0;

            long starTime, endTime, dur, totalTime = 0;

            int totalVal = 0;

            public Reader(Channel chan, int id_) {
                this.id = id_;
                this.c = chan;
            }
            public void run() {
                while(numGets < threads*messages){
                    if(rand.nextDouble() < rwr) {
                        starTime = System.nanoTime();
                        Integer ret = (Integer) c.get();
//                        String ret  = c.get();
                        endTime = System.nanoTime();
                        dur = endTime - starTime;
                        maxGetTime = (dur > maxGetTime) ? dur : maxGetTime;
                        minGetTime = (dur < minGetTime) ? dur : minGetTime;
                        avgGetTime += (dur);
                        if (ret != null) {
                            totalVal += ret;
                            numGets++;
                        } else {
                            nullGets++;
                        }
                    }
                }
                totalTime += (avgGetTime);
                avgGetTime = avgGetTime/(numGets+nullGets);
                }
        }

        class Writer implements Runnable {

            private Channel c;

            double maxPutTime = 0.0;
            double minPutTime = 999999999999999999999999.0;
            double avgPutTime = 0.0;

            final int id;

            long numPuts = 0;

            long starTime, endTime, dur, totalTime = 0;

            public Writer(Channel chan, int id_) {
                this.id = id_;
                this.c = chan;
            }
            public void run() {
                while( numPuts < messages){
                    if(rand.nextDouble() > rwr) { //PUT
//                        System.err.println("Sender "+id+" trying to put "+ numPuts +" "+ numGets);
                        starTime = System.nanoTime();
                        int q = rand.nextInt(queueCount);
                        c.put(id, q);
                        endTime = System.nanoTime();
                        dur = endTime - starTime;
                        maxPutTime = (dur > maxPutTime) ? dur : maxPutTime;
                        minPutTime = (dur < minPutTime) ? dur : minPutTime;
                        minPutTime = (dur < minPutTime) ? dur : minPutTime;
                        avgPutTime += (dur);
                        numPuts++;
                    }
                }
//                System.err.println("Sender " + id +" Got "+count+" numGets, "+numPuts+" puts");
//                System.err.println(c.totalCount());
                totalTime += (avgPutTime);
                avgPutTime = avgPutTime/numPuts;
            }
        }


        Writer[] writers = new Writer[threads];
        for(int i = 0 ; i < threads; i++){
            writers[i] = new Writer(c1, i);
            threadList[i] = new Thread(writers[i], "Sender-"+i);
        }
        for(int i = 0 ; i < threads; i++){
            threadList[i].start();
        }
        Reader reader = new Reader(c1, 0);
        Thread readThread = new Thread(reader, "reader");
        readThread.start();
        for(int i = 0 ; i < threads; i++){
            threadList[i].join();
        }
        readThread.join();

        double maxGetTimeSimple = 0.0;
        double maxPutTimeSimple = 0.0;
        double minGetTimeSimple = 0.0;
        double minPutTimeSimple = 0.0;
        double avgGetTimeSimple = 0.0;
        double avgPutTimeSimple = 0.0;
        long totalTimeSimple = 0;
        int nullGetsSimple = 0;
        double throughputSimple = 0.0;
        double latencySimple = 0.0;
        double stdGetTimeSimple = 0.0;
        double stdPutTimeSimple = 0.0;
        int maxNullGetsSimple = 0;
        int minNullGetsSimple = 0;
        double avgNullGetsSimple = 0.0;
        double stdNullGetsSimple = 0.0;
        int nputs = 0;
        for(int i = 0 ; i < threads; i++){
            maxPutTimeSimple = (writers[i].maxPutTime>maxPutTimeSimple)?writers[i].maxPutTime:maxPutTimeSimple;
            minPutTimeSimple = (writers[i].minPutTime<minPutTimeSimple)?writers[i].minPutTime:minPutTimeSimple;
            avgPutTimeSimple += writers[i].avgPutTime;
            totalTimeSimple += writers[i].totalTime;
            nputs += writers[i].numPuts;
        }
        avgPutTimeSimple = avgPutTimeSimple/threads;
        totalTimeSimple = totalTimeSimple+reader.totalTime;
        nullGetsSimple = reader.nullGets;
        avgGetTimeSimple = reader.avgGetTime;
        maxGetTimeSimple = reader.maxGetTime;
        minGetTimeSimple = reader.minGetTime;
        for(int i = 0 ; i < threads; i++){
            stdPutTimeSimple += (writers[i].avgPutTime - avgPutTimeSimple)*(writers[i].avgPutTime - avgPutTimeSimple);
        }
        stdPutTimeSimple = java.lang.Math.sqrt(stdPutTimeSimple/threads);
        stdGetTimeSimple = java.lang.Math.sqrt(stdGetTimeSimple);

        throughputSimple = (double)(2*messages*threads)/(double)(totalTimeSimple);
//        latency = (double)(2*messages*threads)/(double)(totalTime);
        latencySimple = (double)(totalTimeSimple)/(double)(2*messages*threads);

        Test1Results.add(new Stats(threads, messages, throughputSimple, latencySimple,
                maxGetTimeSimple, minGetTimeSimple, avgGetTimeSimple, stdGetTimeSimple,
                maxNullGetsSimple, minNullGetsSimple, avgNullGetsSimple, stdNullGetsSimple,
                maxPutTimeSimple, minPutTimeSimple, avgPutTimeSimple, stdPutTimeSimple,
                rwr, queueCount, totalTimeSimple
        ));
        System.err.println("Threads: "+threads+", Messages: "+messages+", Queus:  "+queueCount+
                ", avgPutTimeSimple "+avgPutTimeSimple+
                ", avgGetTimeSimple " +avgGetTimeSimple+
                ", nullGetsSimple: "+nullGetsSimple+
                ", throughputSimple: "+throughputSimple+
                ", latencySimple: "+latencySimple+
                ", totalTimeSimple: "+totalTimeSimple
        );
//        System.err.println(totalCount+" "+totalMessages);
        assertEquals(reader.numGets, totalMessages);
        assertEquals(nputs, totalMessages);
        assertEquals(expectedVal-reader.totalVal, 0);
        assertEquals(totalMessages, c1.totalCount());
        assertNull(c1.get());
        for(int i = 0;i< queueCount;i++){
            assertNull(c1.get());
        }
//        expectedVal = placeHolder;

        writers = new Writer[threads];
        for(int i = 0 ; i < threads; i++){
            writers[i] = new Writer(c2, i);
            threadList[i] = new Thread(writers[i], "Sender-"+i);
        }
        for(int i = 0 ; i < threads; i++){
            threadList[i].start();
        }
        reader = new Reader(c2, 0);
        readThread = new Thread(reader, "reader");
        readThread.start();
        for(int i = 0 ; i < threads; i++){
            threadList[i].join();
        }
        readThread.join();

        double maxGetTimeMulti = 0.0;
        double maxPutTimeMulti = 0.0;
        double minGetTimeMulti = 0.0;
        double minPutTimeMulti = 0.0;
        double avgGetTimeMulti = 0.0;
        double avgPutTimeMulti = 0.0;
        long totalTimeMulti = 0;
        int nullGetsMulti = 0;
        double throughputMulti = 0.0;
        double latencyMulti = 0.0;
        double stdGetTimeMulti = 0.0;
        double stdPutTimeMulti = 0.0;
        int maxNullGetsMulti = 0;
        int minNullGetsMulti = 0;
        double avgNullGetsMulti = 0.0;
        double stdNullGetsMulti = 0.0;
        nputs = 0;
        for(int i = 0 ; i < threads; i++){
            maxPutTimeMulti = (writers[i].maxPutTime>maxPutTimeMulti)?writers[i].maxPutTime:maxPutTimeMulti;
            minPutTimeMulti = (writers[i].minPutTime<minPutTimeMulti)?writers[i].minPutTime:minPutTimeMulti;
            avgPutTimeMulti += writers[i].avgPutTime;
            totalTimeMulti += writers[i].totalTime;
            nputs += writers[i].numPuts;
        }
        avgPutTimeMulti = avgPutTimeMulti/threads;
        totalTimeMulti = totalTimeMulti+reader.totalTime;
        nullGetsMulti = reader.nullGets;
        avgGetTimeMulti = reader.avgGetTime;
        maxGetTimeMulti = reader.maxGetTime;
        minGetTimeMulti = reader.minGetTime;
        for(int i = 0 ; i < threads; i++){
            stdPutTimeMulti += (writers[i].avgPutTime - avgPutTimeMulti)*(writers[i].avgPutTime - avgPutTimeMulti);
        }
        stdPutTimeMulti = java.lang.Math.sqrt(stdPutTimeMulti/threads);
        stdGetTimeMulti = java.lang.Math.sqrt(stdGetTimeMulti);

        throughputMulti = (double)(2*messages*threads)/(double)(totalTimeMulti);
//        latency = (double)(2*messages*threads)/(double)(totalTime);
        latencyMulti = (double)(totalTimeMulti)/(double)(2*messages*threads);

        Test2Results.add(new Stats(threads, messages, throughputMulti, latencyMulti,
                maxGetTimeMulti, minGetTimeMulti, avgGetTimeMulti, stdGetTimeMulti,
                maxNullGetsMulti, minNullGetsMulti, avgNullGetsMulti, stdNullGetsMulti,
                maxPutTimeMulti, minPutTimeMulti, avgPutTimeMulti, stdPutTimeMulti,
                rwr, queueCount, totalTimeMulti
        ));
        System.err.println("Threads: "+threads+", Messages: "+messages+", Queus:  "+queueCount+
                ", avgPutTimeMulti "+avgPutTimeMulti+
                ", avgGetTimeMulti " +avgGetTimeMulti+
                ", nullGetsMulti: "+nullGetsMulti+
                ", throughputMulti: "+throughputMulti+
                ", latencyMulti: "+latencyMulti+
                ", totalTimeMulti: "+totalTimeMulti
        );
//        System.err.println(totalCount+" "+totalMessages);
        assertEquals(reader.numGets, totalMessages);
        assertEquals(nputs, totalMessages);
        assertEquals(expectedVal-reader.totalVal, 0);
        assertEquals(totalMessages, c2.totalCount());
        assertNull(c2.get());
        for(int i = 0;i< queueCount;i++){
            assertNull(c2.get());
        }

        writers = new Writer[threads];
        for(int i = 0 ; i < threads; i++){
            writers[i] = new Writer(c3, i);
            threadList[i] = new Thread(writers[i], "Sender-"+i);
        }
        for(int i = 0 ; i < threads; i++){
            threadList[i].start();
        }
        reader = new Reader(c3, 0);
        readThread = new Thread(reader, "reader");
        readThread.start();
        for(int i = 0 ; i < threads; i++){
            threadList[i].join();
        }
        readThread.join();

        double maxGetTimeNoCopy = 0.0;
        double maxPutTimeNoCopy = 0.0;
        double minGetTimeNoCopy = 0.0;
        double minPutTimeNoCopy = 0.0;
        double avgGetTimeNoCopy = 0.0;
        double avgPutTimeNoCopy = 0.0;
        long totalTimeNoCopy = 0;
        int nullGetsNoCopy = 0;
        double throughputNoCopy = 0.0;
        double latencyNoCopy = 0.0;
        double stdGetTimeNoCopy = 0.0;
        double stdPutTimeNoCopy = 0.0;
        int maxNullGetsNoCopy = 0;
        int minNullGetsNoCopy = 0;
        double avgNullGetsNoCopy = 0.0;
        double stdNullGetsNoCopy = 0.0;
        nputs = 0;
        for(int i = 0 ; i < threads; i++){
            maxPutTimeNoCopy = (writers[i].maxPutTime>maxPutTimeNoCopy)?writers[i].maxPutTime:maxPutTimeNoCopy;
            minPutTimeNoCopy = (writers[i].minPutTime<minPutTimeNoCopy)?writers[i].minPutTime:minPutTimeNoCopy;
            avgPutTimeNoCopy += writers[i].avgPutTime;
            totalTimeNoCopy += writers[i].totalTime;
            nputs += writers[i].numPuts;
        }
        avgPutTimeNoCopy = avgPutTimeNoCopy/threads;
        nullGetsNoCopy = reader.nullGets;
        totalTimeNoCopy = totalTimeNoCopy+reader.totalTime;
        avgGetTimeNoCopy = reader.avgGetTime;
        maxGetTimeNoCopy = reader.maxGetTime;
        minGetTimeNoCopy = reader.minGetTime;
        for(int i = 0 ; i < threads; i++){
            stdPutTimeNoCopy += (writers[i].avgPutTime - avgPutTimeNoCopy)*(writers[i].avgPutTime - avgPutTimeNoCopy);
        }
        stdPutTimeNoCopy = java.lang.Math.sqrt(stdPutTimeNoCopy/threads);
        stdGetTimeNoCopy = java.lang.Math.sqrt(stdGetTimeNoCopy);

        throughputNoCopy = (double)(2*messages*threads)/(double)(totalTimeNoCopy);
//        latency = (double)(2*messages*threads)/(double)(totalTime);
        latencyNoCopy = (double)(totalTimeNoCopy)/(double)(2*messages*threads);

        Test3Results.add(new Stats(threads, messages, throughputNoCopy, latencyNoCopy,
                maxGetTimeNoCopy, minGetTimeNoCopy, avgGetTimeNoCopy, stdGetTimeNoCopy,
                maxNullGetsNoCopy, minNullGetsNoCopy, avgNullGetsNoCopy, stdNullGetsNoCopy,
                maxPutTimeNoCopy, minPutTimeNoCopy, avgPutTimeNoCopy, stdPutTimeNoCopy,
                rwr, queueCount, totalTimeNoCopy
        ));
        System.err.println("Threads: "+threads+", Messages: "+messages+", Queus:  "+queueCount+
                ", avgPutTimeNoCopy "+avgPutTimeNoCopy+
                ", avgGetTimeNoCopy " +avgGetTimeNoCopy+
                ", nullGetsNoCopy: "+nullGetsNoCopy+
                ", throughputNoCopy: "+throughputNoCopy+
                ", latencyNoCopy: "+latencyNoCopy+
                ", totalTimeNoCopy: "+totalTimeNoCopy
        );
        assertEquals(reader.numGets, totalMessages);
        assertEquals(nputs, totalMessages);
        assertEquals(expectedVal-reader.totalVal, 0);
        assertNull(c3.get());
        assertEquals(totalMessages, c3.totalCount());
        for(int i = 0;i< queueCount;i++){
            assertNull(c3.get());
        }
//        Thread.sleep(200);
    }

    @Test(
            dependsOnGroups = "a"
            ,groups = {"d"}
    )
    void printSimple() throws IOException {
        File file= new File(System.getProperty("user.home")+"/IdeaProjects/1/Test1.txt");
        file.createNewFile();
        FileWriter fw = new FileWriter(file);
        System.out.println(Test1Results.size());
        for (int i = 0; i < Test1Results.size(); i++) {
            Stats x = Test1Results.get(i);
            fw.write(x.numThreads+" "+x.numMessages+" "+x.throughput+" "+x.latency+" "+x.avgGetTime+" "+x.avgPutTime+
                    " "+x.stdGetTime+" "+ x.stdPutTime + " "+x.numQueues+" "+x.readWriteRatio+" "+"\n");
        }
        fw.close();
    }

    @Test(
            dependsOnGroups = "b"
            ,groups={"e"}
    )
    void printMulti() throws IOException {
        File file= new File(System.getProperty("user.home")+"/IdeaProjects/1/Test2.txt");
        file.createNewFile();
        FileWriter fw = new FileWriter(file);
        System.out.println(Test2Results.size());
        for (int i = 0; i < Test2Results.size(); i++) {
            Stats x = Test2Results.get(i);
            fw.write(x.numThreads+" "+x.numMessages+" "+x.throughput+" "+x.latency+" "+x.avgGetTime+" "+x.avgPutTime+
                    " "+x.stdGetTime+" "+ x.stdPutTime +" "+x.numQueues+" "+x.readWriteRatio+" "+"\n");
        }
        fw.close();
    }
    @Test(
            dependsOnGroups = "c"
            ,groups={"f"}
    )
    void printNoCopy() throws IOException {
        File file= new File(System.getProperty("user.home")+"/IdeaProjects/1/Test3.txt");
        file.createNewFile();
        FileWriter fw = new FileWriter(file);
        System.out.println(Test3Results.size());
        for (int i = 0; i < Test3Results.size(); i++) {
            Stats x = Test3Results.get(i);
            fw.write(x.numThreads+" "+x.numMessages+" "+x.throughput+" "+x.latency+" "+x.avgGetTime+" "+x.avgPutTime+
                    " "+x.stdGetTime+" "+ x.stdPutTime +" "+x.numQueues+" "+x.readWriteRatio+" "+"\n");
        }
        fw.close();
    }

    @Test(
            dependsOnGroups = "extra"
            ,groups = "extra_print"
    )
    void printExtra() throws IOException{
        File file= new File(System.getProperty("user.home")+"/IdeaProjects/1/TestExtra1.txt");
        file.createNewFile();
        FileWriter fw = new FileWriter(file);
        for (int i = 0; i < Test1Results.size(); i++) {
            Stats x = Test1Results.get(i);
            fw.write(x.numThreads+" "+
                    x.numMessages+" "+
                    x.numQueues+" "+
                    x.readWriteRatio+" "+
                    x.latency+" "+
                    x.throughput+" "+
                    x.avgGetTime+" "+
                    x.avgPutTime+" "+
                    x.stdGetTime+" "+
                    x.stdPutTime +" "+
                    x.total
                    +"\n");
        }
        fw.close();
        file= new File(System.getProperty("user.home")+"/IdeaProjects/1/TestExtra2.txt");
        file.createNewFile();
        fw = new FileWriter(file);
        for (int i = 0; i < Test2Results.size(); i++) {
            Stats x = Test2Results.get(i);
            fw.write(x.numThreads+" "+
                    x.numMessages+" "+
                    x.numQueues+" "+
                    x.readWriteRatio+" "+
                    x.latency+" "+
                    x.throughput+" "+
                    x.avgGetTime+" "+
                    x.avgPutTime+" "+
                    x.stdGetTime+" "+
                    x.stdPutTime +" "+
                    x.total
                    +"\n");
        }
        fw.close();
        file= new File(System.getProperty("user.home")+"/IdeaProjects/1/TestExtra3.txt");
        file.createNewFile();
        fw = new FileWriter(file);
        for (int i = 0; i < Test3Results.size(); i++) {
            Stats x = Test3Results.get(i);
            fw.write(x.numThreads+" "+
                    x.numMessages+" "+
                    x.numQueues+" "+
                    x.readWriteRatio+" "+
                    x.latency+" "+
                    x.throughput+" "+
                    x.avgGetTime+" "+
                    x.avgPutTime+" "+
                    x.stdGetTime+" "+
                    x.stdPutTime +" "+
                    x.total
                    +"\n");
        }
        fw.close();
    }
}
