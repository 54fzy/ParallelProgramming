// $Id: SampleTests.java 53 2017-02-06 13:59:38Z abcdef $

import cs735_835.computation.Computation;
import grading.Callback;
import grading.TestFunction;
import grading.TestTask;
import jdk.nashorn.internal.codegen.CompilerConstants;
import org.testng.ITestNGListener;
import org.testng.TestNG;
import org.testng.annotations.Test;
import org.testng.reporters.TextReporter;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static cs735_835.computation.Computation.newComputation;
import static org.testng.Assert.*;

public class SampleTests {

  /* so tests can be run from sbt: test:runMain SampleTests */
  public static void main(String[] args) {
    TestNG testng = new TestNG();
    testng.setTestClasses(new Class<?>[]{SampleTests.class});
    testng.setUseDefaultListeners(false);
    testng.addListener((ITestNGListener) new TextReporter("sample tests", 2));
    testng.run();
  }

  @Test
  void test1() throws Exception {
    Thread main = Thread.currentThread();
    TestTask<String> task = new TestTask<>("T"); // the underlying task
    Computation<String> comp = newComputation(task);
    assertTrue(task.waitForStart(1000)); // the task starts to run
    assertFalse(comp.isFinished()); // the computation is not finished
    task.finish();
    assertEquals(comp.get(), "T"); // get blocks the thread, then produces the result "T"
    assertTrue(comp.isFinished()); // once get returns, the computation is finished
    assertNotSame(task.getCaller(), main); // the task ran in a separate thread
    task.getCaller().join(1000); // this thread now terminates
    assertFalse(task.getCaller().isAlive());
  }

  @Test
  void test2() throws Exception {
    Thread main = Thread.currentThread();
    Callback callback1 = new Callback();
    Callback callback2 = new Callback();
    Callback callback3 = new Callback();
    Callback callback4 = new Callback();
    TestTask<String> task = new TestTask<>("T");
    // first callback specified at construction time
    Computation<String> comp = newComputation(task, callback1);
    comp.onComplete(callback2); // callback added while the task is running
    assertFalse(comp.isFinished());
    task.finish();
    comp.onComplete(callback3); // racy callback
    comp.get();
    // get has returned; all the callbacks have run exactly once
    assertEquals(callback1.getCallCount(), 1);
    assertEquals(callback2.getCallCount(), 1);
    assertEquals(callback3.getCallCount(), 1);
    // callbacks 1 and 2 run by the computation thread
    assertSame(callback1.getCaller(), task.getCaller());
    assertSame(callback2.getCaller(), task.getCaller());
    // callback3 run by either thread
    assertTrue(callback3.getCaller() == task.getCaller() || callback3.getCaller() == main);
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
    // computation thread is terminated
    comp.onComplete(callback4); // run in calling thread
    assertSame(callback4.getCaller(), main);
  }

    @Test
    void test3() throws Exception {
      Thread main = Thread.currentThread();
        TestTask<String> task = new TestTask<>("T");
        TestFunction<String, Integer> f1 = new TestFunction<>(1);
        TestFunction<String, Integer> f2 = new TestFunction<>(2);
        TestFunction<String, Integer> f3 = new TestFunction<>(3, 1.0); // f3 terminates automatically after 1 second
    TestFunction<String, Integer> f4 = new TestFunction<>(4, 1.0); // f4 terminates automatically after 1 second
    Computation<String> comp = newComputation(task);
    // first continuation specified while the task is running
    Computation<Integer> comp1 = comp.map(f1);
    assertFalse(f1.isRunning()); // continuation not started yet
    task.finish();
    assertTrue(f1.waitForStart(1000)); // first continuation starts...
    assertSame(f1.getCaller(), task.getCaller()); // in the computation thread...
    assertEquals(f1.getInput(), "T"); // with the output of the computation as its input
    // second continuation added before the first one finished
    assertFalse(comp1.isFinished());

      Computation<Integer> comp2 = comp.map(f2);
      assertFalse(f2.isRunning()); // continuation not started yet
      f1.finish(); // first continuation finishes...
      System.err.println("**** 1");
      System.err.println(comp1.get());
      assertEquals(comp1.get().intValue(), 1); // with its output: 1
      assertTrue(f2.waitForStart(1000)); // second continuation starts...
    assertSame(f2.getCaller(), task.getCaller()); // in the computation thread...
    assertEquals(f2.getInput(), "T"); // with the output of the computation as its input
    f2.finish(); // second continuation finishes...
    // at the same time a third computation is specified
    Computation<Integer> comp3 = comp.map(f3);
    assertTrue(f3.waitForStart(1000)); // third continuation starts...
    assertTrue(f3.getCaller() == task.getCaller() || f3.getCaller() == main); // in an existing thread...
    assertEquals(f3.getInput(), "T"); // with the output of the computation as its input
    f3.finish();
    assertEquals(comp2.get().intValue(), 2); // output of second continuation
    assertEquals(comp3.get().intValue(), 3); // output of third continuation
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
    // computation thread is terminated
    Computation<Integer> comp4 = comp.map(f4);
    assertTrue(comp4.isFinished()); // continuation is terminated
    assertSame(f4.getCaller(), main); // it ran in the calling thread...
    assertEquals(f4.getInput(), "T"); // with the output of the computation as its input
    assertEquals(comp4.get().intValue(), 4); // output of fourth continuation
  }

  @Test
  void test4() throws Exception {
    Thread main = Thread.currentThread();
    Set<Thread> threads = new java.util.HashSet<>(4);
    TestTask<String> task = new TestTask<>("T");
    TestFunction<String, Integer> f1 = new TestFunction<>(1);
    TestFunction<String, Integer> f2 = new TestFunction<>(2);
    TestFunction<String, Integer> f3 = new TestFunction<>(3);
    Computation<String> comp = newComputation(task);
    // first continuation specified while the task is running
    Computation<Integer> comp1 = comp.mapParallel(f1);
    assertFalse(f1.isRunning()); // continuation not started yet
    task.finish();
    assertTrue(f1.waitForStart(1000)); // first continuation starts...
    assertNotSame(f1.getCaller(), task.getCaller()); // in a new thread...
    assertEquals(f1.getInput(), "T"); // with the output of the computation as its input
    // threads used so far
    threads.add(task.getCaller());
    threads.add(f1.getCaller());
    // computation thread is done
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
    // second continuation added before the first one finished
    assertFalse(comp1.isFinished());
    Computation<Integer> comp2 = comp.mapParallel(f2);
    assertTrue(f2.waitForStart(1000)); // it starts running immediately...
    assertTrue(threads.add(f2.getCaller())); // in a new thread...
    assertEquals(f2.getInput(), "T"); // with the output of the computation as its input
    assertTrue(f1.isRunning() && f2.isRunning()); // both continuations run in parallel
    // the first two continuations finish...
    f1.finish();
    f2.finish();
    // with their outputs: 1 and 2
    assertEquals(comp1.get().intValue(), 1);
    assertEquals(comp2.get().intValue(), 2);
    // all threads now done
    f1.getCaller().join(1000);
    assertFalse(f1.getCaller().isAlive());
    f2.getCaller().join(1000);
    assertFalse(f2.getCaller().isAlive());
    // all threads are finished before a third computation is specified
    Computation<Integer> comp3 = comp.mapParallel(f3);
    assertTrue(f3.waitForStart(1000)); // third continuation starts...
    assertTrue(threads.add(f3.getCaller())); // in a new thread...
    assertEquals(f3.getInput(), "T"); // with the output of the computation as its input
    f3.finish();
    assertEquals(comp1.get().intValue(), 1); // output of first continuation
    assertEquals(comp2.get().intValue(), 2); // output of second continuation
    assertEquals(comp3.get().intValue(), 3); // output of third continuation
    // last thread terminates
    f3.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
    assertEquals(threads.size(), 4); // four threads were used...
    assertFalse(threads.contains(main)); // none of them the calling thread
  }

  @Test
  void test5() throws Exception {
    Exception ex = new Exception(); // exception thrown by the task
    TestTask<String> task = new TestTask<>(ex); // a failing task
    Computation<String> comp = newComputation(task);
    TestFunction<String, Integer> f1 = new TestFunction<>(1); // first continuation
    TestFunction<String, Integer> f2 = new TestFunction<>(2); // second continuation
    Runnable callback1 = () -> { // a failing callback
        throw new RuntimeException();
    };
    Callback callback2 = new Callback(); // a second callback
    // registering callbacks
    comp.onComplete(callback1);
    comp.onComplete(callback2);
    // registering continuations
    Computation<Integer> comp1 = comp.map(f1);
    Computation<Integer> comp2 = comp.mapParallel(f2);
    task.finish(); // the task fails
    try {
        comp.get(); // should throw ExecutionException
        fail("exception expected");
    } catch (InterruptedException e) {
        throw e;
    } catch (Exception e) {
        assertTrue(e instanceof ExecutionException);
        assertSame(e.getCause(), ex); // the exception thrown by the task
    }
    System.err.println("1");
    try {
        comp1.get(); // should throw CancellationException
        fail("exception expected");
    } catch (InterruptedException e) {
        throw e;
    } catch (Exception e) {
        assertTrue(e instanceof CancellationException);
    }
    System.err.println("2");
    try {
        comp2.get(); // should throw CancellationException
        // fail("exception expected");
    } catch (InterruptedException e) {
        throw e;
    } catch (Exception e) {
        assertTrue(e instanceof CancellationException);
    }
      System.err.println("**** 4 *****");
    assertEquals(callback2.getCallCount(), 1); // second callback was executed
    // all computations are finished
    assertTrue(comp.isFinished());
    assertTrue(comp1.isFinished());
    assertTrue(comp2.isFinished());
    // the continuations were never called
    assertNull(f1.getCaller());
    assertNull(f2.getCaller());
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
  }

  @Test
    void test6() throws ExecutionException, InterruptedException {
      int n = 5000;
      Thread main = Thread.currentThread();
      Callback cb = new Callback();
      Callback cba[] = new Callback[n];
      TestTask<Integer> task = new TestTask(45346);
      Computation<Integer> cpa [] = new Computation[n];
      for(int i = 0 ; i < n; i++){
          cpa[i] = newComputation(task, cb);
      }
      task.finish();
      for(int i = 0 ; i < n; i++){
          cpa[i].get();
          cba[i]= new Callback();
          cpa[i].onComplete(cba[i]);
          assertEquals(cba[i].getCallCount(), 1);
          assertTrue((cba[i].getCaller()==main));//||
//                  (cba[i].getCaller()==task.getCaller()));
      }
      Thread.sleep(1000);
      assertEquals(cb.getCallCount(), n);
  }

    @Test
    void test7() throws ExecutionException, InterruptedException {
        int n = 2048;
        Random r = new Random();
        Exception ex = new Exception();
        TestTask<String> task = new TestTask<>(ex); // a failing task
        Computation<String> comp = newComputation(task);
        Computation<Integer> cpa[] = new Computation[n];
        TestFunction<String, Integer> tfa[] = new TestFunction[n];
        tfa[0] = new TestFunction<String, Integer>(0);
        cpa[0] = comp.map(tfa[0]);
        for (int i = 1; i < n; i++){
            tfa[i] = new TestFunction<String, Integer>(i);
            if (r.nextDouble()>0.5) {
                cpa[i] = cpa[i - 1].map((Function) tfa[i]);
            }else{
                cpa[i] = cpa[i - 1].mapParallel((Function) tfa[i]);
            }
        }
        task.finish();
        try {
            comp.get(); // should throw ExecutionException
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            assertTrue(e instanceof ExecutionException);
            assertSame(e.getCause(), ex); // the exception thrown by the task
        }

        for(int i = 0 ; i < n ; i++){
            tfa[i].finish();
            try {
                cpa[i].get();
            }catch (Exception e){
                assertTrue(e instanceof CancellationException);
            }
        }
    }

    @Test
    void test8() throws ExecutionException, InterruptedException {
        Thread main = Thread.currentThread();
        InterruptedException iex = new InterruptedException();
        TestTask task = new TestTask(iex);
        TestFunction<String, Integer> f1 = new TestFunction<>(1);
        Callback cb1 = new Callback();
        Callback cb2 = new Callback();
        Callback cb3 = new Callback();
        Callback cb4 = new Callback();
        Computation comp = newComputation(task, cb1);
        comp.onComplete(cb1);
        comp.onComplete(cb1);
        Computation comp1 = comp.map(f1);
        comp1.onComplete(cb2);
        comp1.onComplete(cb2);
        comp1.onComplete(cb2);
        task.finish();
        Thread.sleep(1000);
        comp.onComplete(cb3);
        comp.onComplete(cb3);
        f1.finish();
        Thread.sleep(1000);
        comp.onComplete(cb4);
        comp.onComplete(cb4);
        try {
            comp.get();
        }catch (Exception e){
            e.printStackTrace();
            assertTrue(e instanceof ExecutionException);
        }
        try {
            comp1.get();
        }catch (Exception e){
            e.printStackTrace();
            assertTrue(e instanceof CancellationException);
        }
        assertEquals(cb1.getCallCount(), 3);
        assertEquals(cb2.getCallCount(), 0);
        assertEquals(cb3.getCallCount(), 2);
        assertEquals(cb4.getCallCount(), 2);
        assertTrue(cb3.getCaller()==main);
        assertTrue(cb4.getCaller()==main);
    }

    @Test
    void test9() throws ExecutionException, InterruptedException {
        int n = 4000;
        Random r = new Random();
        InterruptedException iex = new InterruptedException();
        RuntimeException rex = new RuntimeException();
        TestTask<String> task = new TestTask("startingTask");
        ArrayList<Computation> computations = new ArrayList<>();
        ArrayList<Computation> continuations = new ArrayList<>();
        ArrayList<TestTask> tasks = new ArrayList<>();
        ArrayList<Callback> callbacks = new ArrayList<>();
        ArrayList<Callback> completedCallbacks= new ArrayList<>();
        ArrayList<ArrayList<Callback>> continuationCallbacks= new ArrayList<>();
        ArrayList<TestFunction> functions = new ArrayList<>();
        Computation mainComp = newComputation(task);
        tasks.add(task);
        computations.add(mainComp);
        for(int i = 0; i<n ;i++){
            TestFunction tf;
//            Add regular function or function that throws exception
            if(r.nextDouble()>.5){
                tf = new TestFunction("testFunction", 0.5);
            }else{
                tf = new TestFunction(rex, 0.5);
            }
            Computation c;
//            Add continuation or new computation
            if((r.nextDouble()>.5)&&(computations.size()>0)) {
                int nextComp = r.nextInt(computations.size());
//                we roll to see if we are going to add the
//                continuations right after the task has finished
                if((r.nextDouble()<.05)&&(computations.size()>0)){
                    Computation cc = computations.get(nextComp);
                    TestTask tt = tasks.get(nextComp);
                    tt.finish();
                    //map or mapParallel
                    if (r.nextDouble() > .5) {
//                        c = computations.get(nextComp).map(tf);
                        c = cc.map(tf);
                    } else {
//                        c = computations.get(nextComp).mapParallel(tf);
                        c = cc.mapParallel(tf);
                    }

                }else {
//                    map or mapParallel
                    if (r.nextDouble() > .5) {
                        c = computations.get(nextComp).map(tf);
                    } else {
                        c = computations.get(nextComp).mapParallel(tf);
                    }
                }
                continuations.add(c);
                functions.add(tf);
            }else{
                //Create a new Computation
                System.err.println("Creating new Computation");
                TestTask ts;
                //Task may throw an exception
                if(r.nextDouble()>.1){
                    ts = new TestTask(i);
                }else{
                    ts = new TestTask(iex);
                }
                c = newComputation(ts);
                tasks.add(ts);
                computations.add(c);
            }
//            Add a random number of callbacks half of which will fail
            int l = r.nextInt(100);
            ArrayList<Callback> tempcb = new ArrayList<>();
            for(int j = 0; j < l; j++){
                Callback cb ;
                if(r.nextInt()>.5){
                    cb = new Callback();
                    callbacks.add(cb);
                    c.onComplete(cb);
                    if(computations.contains(c)){
//                      if computation is a not continuation
//                      if it fails the callbacls should
//                      be executed
                        completedCallbacks.add(cb);
                    }else{
                        tempcb.add(cb);
                    }
                }else{
                    Runnable cb_ = () -> { // a failing callback
                        throw new RuntimeException();
                    };
                    c.onComplete(cb_);
                }
            }
            if(continuations.contains(c)){
                //if computation is a continuation
                //if it fails the callbacls should
                //not be executed
                continuationCallbacks.add(tempcb);
            }

        }
        assertEquals(tasks.size(), computations.size());
        System.err.println("-----------------");
        for(int i = 0; i<tasks.size();i++){
            tasks.get(i).finish();
            try {
                computations.get(i).get();
            } catch (Exception e) {
//                if a computation failed
//                make sure it threw the correct exception
                assertTrue((e instanceof ExecutionException));
            }
        }
        for(int i = 0 ; i <completedCallbacks.size(); i++){
            //Check that the callbacks of the computations have
            //all been run even if the computations have been
            //canceled
            assertEquals(completedCallbacks.get(i).getCallCount(), 1);
        }
        for(int i = 0; i<functions.size();i++) {
            functions.get(i).finish();
        }
        assertEquals(functions.size(), continuations.size());
        for(int i = 0; i<continuations.size();i++){
            try {
                continuations.get(i).get();
            } catch (Exception e) {
                // for every continuation that failed make sure
                // that they threw the correct exception
                assertTrue((e instanceof CancellationException)||
                        (e instanceof ExecutionException));
                for(int j = 0; j<continuationCallbacks.get(i).size();j++){
                    if(e instanceof CancellationException){
                        // if they did not have any parents
                        // their callbacks should have run exactly once
                        assertEquals(continuationCallbacks.get(i).get(j).getCallCount(), 0);
                    }else{
                        //if they were continuations their
                        //callbacks should have not run
                        assertEquals(continuationCallbacks.get(i).get(j).getCallCount(), 1);
                    }
                }
            }
        }
    }
}