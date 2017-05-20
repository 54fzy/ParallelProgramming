// $Id: ComputationTests2.java 57 2017-03-06 14:04:36Z abcdef $

package grading;

import cs735_835.computation.Computation;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.*;

public class ComputationTests2 { // callbacks

  @Test(description = "one callback, given initially")
  void test1() throws Exception {
    Callback callback = new Callback();
    TestTask<String> task = new TestTask<>("T");
    Computation<String> comp = Computation.newComputation(task, callback);
    assertTrue(task.waitForStart(1000));
    assertEquals(callback.getCallCount(), 0);
    Thread.sleep(100);
    assertEquals(callback.getCallCount(), 0);
    task.finish();
    comp.get();
    assertSame(callback.getCaller(), task.getCaller());
    assertEquals(callback.getCallCount(), 1);
    Thread.sleep(100);
    assertEquals(callback.getCallCount(), 1);
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
  }

  @Test(description = "one callback, given after start")
  void test2() throws Exception {
    Callback callback = new Callback();
    TestTask<String> task = new TestTask<>("T");
    Computation<String> comp = Computation.newComputation(task);
    assertTrue(task.waitForStart(1000));
    comp.onComplete(callback);
    Thread.sleep(100);
    assertEquals(callback.getCallCount(), 0);
    task.finish();
    comp.get();
    assertSame(callback.getCaller(), task.getCaller());
    assertEquals(callback.getCallCount(), 1);
    Thread.sleep(100);
    assertEquals(callback.getCallCount(), 1);
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
  }

  @Test(description = "one callback, given after completion")
  void test3() throws Exception {
    Thread main = Thread.currentThread();
    Callback callback = new Callback();
    TestTask<String> task = new TestTask<>("T");
    Computation<String> comp = Computation.newComputation(task);
    task.finish();
    comp.get();
    comp.onComplete(callback);
    assertEquals(callback.getCallCount(), 1);
    assertSame(callback.getCaller(), main);
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
  }

  @DataProvider
  Object[][] test4data() {
    return new Object[][]{
        {10}, {100}, {1000}, {1000_000}
    };
  }

  @Test(dataProvider = "test4data", description = "multiple callbacks")
  void test4(int n) throws Exception {
    Thread main = Thread.currentThread();
    Callback[] callbacks1 = new Callback[n];
    Callback[] callbacks2 = new Callback[n];
    Callback[] callbacks3 = new Callback[n];
    java.util.Arrays.setAll(callbacks1, i -> new Callback());
    java.util.Arrays.setAll(callbacks2, i -> new Callback());
    java.util.Arrays.setAll(callbacks3, i -> new Callback());
    TestTask<String> task = new TestTask<>("T");
    Computation<String> comp = Computation.newComputation(task, callbacks1);
    assertTrue(task.waitForStart(1000));
    for (Callback callback : callbacks2)
      comp.onComplete(callback);
    Thread.sleep(100);
    for (Callback callback : callbacks1)
      assertEquals(callback.getCallCount(), 0);
    for (Callback callback : callbacks2)
      assertEquals(callback.getCallCount(), 0);
    task.finish();
    comp.get();
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
    for (Callback callback : callbacks3)
      comp.onComplete(callback);
    for (Callback callback : callbacks1) {
      assertSame(callback.getCaller(), task.getCaller());
      assertEquals(callback.getCallCount(), 1);
    }
    for (Callback callback : callbacks2) {
      assertSame(callback.getCaller(), task.getCaller());
      assertEquals(callback.getCallCount(), 1);
    }
    for (Callback callback : callbacks3) {
      assertSame(callback.getCaller(), main);
      assertEquals(callback.getCallCount(), 1);
    }
  }

  @Test(description = "lots of callbacks")
  void test5() throws Exception {
    List<Callback> callbacks = new java.util.ArrayList<>(10_000_000);
    TestTask<String> task = new TestTask<>("T", 1); // 1-second task
    Computation<String> comp = Computation.newComputation(task);
    do {
      for (int i = 0; i < 100_000; i++) {
        Callback callback = new Callback();
        comp.onComplete(callback);
        callbacks.add(callback);
      }
    } while (task.isRunning());
    assertTrue(!comp.isFinished() || callbacks.get(callbacks.size() - 1).getCallCount() == 1);
    comp.get();
    for (Callback callback : callbacks)
      assertEquals(callback.getCallCount(), 1);
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
  }

  @Test(description = "callback between end of task and end of computation")
  void test6() throws Exception {
    TestTask<String> task = new TestTask<>("T");
    Computation<String> comp = Computation.newComputation(task);
    class Callback implements Runnable {
      public volatile boolean run, taskRunning, compRunning;

      public void run() {
        run = true;
        taskRunning = task.isRunning();
        compRunning = !comp.isFinished();
      }
    }
    Callback callback = new Callback();
    comp.onComplete(callback);
    task.finish();
    comp.get();
    assertTrue(callback.run);
    assertFalse(callback.taskRunning);
    assertTrue(callback.compRunning);
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
  }

  @Test(description = "callback completes without calling get")
  void test7() throws Exception {
    Callback callback = new Callback();
    TestTask<String> task = new TestTask<>("T");
    Computation<String> comp = Computation.newComputation(task);
    comp.onComplete(callback);
    task.finish();
    assertTrue(task.waitForStart(1000));
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
    assertEquals(callback.getCallCount(), 1);
    assertSame(callback.getCaller(), task.getCaller());
  }


}
