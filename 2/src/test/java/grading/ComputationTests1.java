// $Id: ComputationTests1.java 57 2017-03-06 14:04:36Z abcdef $

package grading;

import cs735_835.computation.Computation;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class ComputationTests1 { // starting computations (no callbacks or continuations)

  @Test(description = "one asynchronous computation")
  void test1() throws Exception {
    Thread main = Thread.currentThread();
    TestTask<String> task = new TestTask<>("T");
    Computation<String> comp = Computation.newComputation(task);
    assertTrue(task.waitForStart(1000));
    assertFalse(comp.isFinished());
    task.finish();
    String result = comp.get();
    assertTrue(comp.isFinished());
    assertEquals(result, "T");
    assertNotSame(task.getCaller(), main);
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
  }

  @Test(description = "two asynchronous computations")
  void test2() throws Exception {
    Thread main = Thread.currentThread();
    TestTask<String> task1 = new TestTask<>("T1");
    TestTask<String> task2 = new TestTask<>("T2");
    Computation<String> comp1 = Computation.newComputation(task1);
    Computation<String> comp2 = Computation.newComputation(task2);
    assertTrue(task1.waitForStart(1000));
    assertFalse(comp1.isFinished());
    assertTrue(task2.waitForStart(1000));
    assertFalse(comp2.isFinished());
    task1.finish();
    assertEquals(comp1.get(), "T1");
    assertFalse(task1.isRunning());
    assertTrue(task2.isRunning());
    assertTrue(comp1.isFinished());
    assertNotSame(task1.getCaller(), main);
    task2.finish();
    assertEquals(comp2.get(), "T2");
    assertFalse(task2.isRunning());
    assertTrue(comp2.isFinished());
    assertNotSame(task2.getCaller(), main);
    assertNotSame(task1.getCaller(), task2.getCaller());
    task1.getCaller().join(1000);
    assertFalse(task1.getCaller().isAlive());
    task2.getCaller().join(1000);
    assertFalse(task2.getCaller().isAlive());
  }

  @Test(description = "get blocks")
  void test3() throws Exception {
    TestTask<String> task = new TestTask<>("T");
    Computation<String> comp = Computation.newComputation(task);
    Computation<String> getter = Computation.newComputation(comp::get);
    Thread.sleep(100);
    assertFalse(getter.isFinished());
    task.finish();
    assertEquals(getter.get(), "T");
    assertTrue(comp.isFinished());
    assertTrue(getter.isFinished());
  }


}