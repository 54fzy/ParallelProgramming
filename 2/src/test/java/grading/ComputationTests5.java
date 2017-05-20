// $Id: ComputationTests5.java 57 2017-03-06 14:04:36Z abcdef $

package grading;

import cs735_835.computation.Computation;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import static org.testng.Assert.*;

public class ComputationTests5 { // failures

  @Test(description = "one computation fails")
  void test1() throws Exception {
    Exception ex = new Exception();
    TestTask<Void> task = new TestTask<>(ex);
    Computation<Void> comp = Computation.newComputation(task);
    task.finish();
    try {
      comp.get();
      fail("expected exception");
    } catch (ExecutionException e) {
      assertSame(e.getCause(), ex);
    } catch (Exception e) {
      fail("unexpected exception", e);
    }
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
  }

  @Test(description = "computation fails, callback runs")
  void test2() throws Exception {
    Exception ex = new Exception();
    TestTask<Void> task = new TestTask<>(ex);
    Computation<Void> comp = Computation.newComputation(task);
    Callback callback = new Callback();
    comp.onComplete(callback);
    task.finish();
    try {
      comp.get();
      fail("expected exception");
    } catch (ExecutionException e) {
      assertSame(e.getCause(), ex);
    } catch (Exception e) {
      fail("unexpected exception", e);
    }
    assertEquals(callback.getCallCount(), 1);
    assertSame(callback.getCaller(), task.getCaller());
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
  }

  @Test(description = "one callback fails, others are attempted")
  void test3() throws Exception {
    TestTask<String> task = new TestTask<>("T");
    Computation<String> comp = Computation.newComputation(task);
    Callback callback1 = new Callback();
    Runnable callback2 = () -> {
      throw new RuntimeException();
    };
    Callback callback3 = new Callback();
    comp.onComplete(callback1);
    comp.onComplete(callback2);
    comp.onComplete(callback3);
    task.finish();
    comp.get();
    assertEquals(callback1.getCallCount(), 1);
    assertEquals(callback3.getCallCount(), 1);
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
  }

  @Test(description = "computation fails, continuation does not run")
  void test4() throws Exception {
    TestTask<Void> task = new TestTask<>(new Exception());
    Computation<Void> comp1 = Computation.newComputation(task);
    TestFunction<Void, Integer> f = new TestFunction<>(42);
    Computation<Integer> comp2 = comp1.map(f);
    task.finish();
    try {
      comp2.get();
      fail("expected exception");
    } catch (CancellationException e) {
      // OK
    } catch (Exception e) {
      fail("unexpected exception", e);
    }
    assertFalse(f.waitForStart(100));
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
  }

  @Test(description = "computation fails, parallel continuation does not run")
  void test5() throws Exception {
    TestTask<Void> task = new TestTask<>(new Exception());
    Computation<Void> comp1 = Computation.newComputation(task);
    TestFunction<Void, Integer> f = new TestFunction<>(42);
    Computation<Integer> comp2 = comp1.mapParallel(f);
    task.finish();
    try {
      comp2.get();
      fail("expected exception");
    } catch (CancellationException e) {
      // OK
    } catch (Exception e) {
      fail("unexpected exception", e);
    }
    assertFalse(f.waitForStart(100));
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
  }

  @Test(description = "computation fails, callback of continuation does not run")
  void test6() throws Exception {
    TestTask<Void> task = new TestTask<>(new Exception());
    Computation<Void> comp1 = Computation.newComputation(task);
    TestFunction<Void, Integer> f = new TestFunction<>(42);
    Computation<Integer> comp2 = comp1.map(f);
    Callback callback = new Callback();
    comp2.onComplete(callback);
    task.finish();
    assertTrue(task.waitForStart(1000));
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
    assertEquals(callback.getCallCount(), 0);
  }

  @DataProvider
  Object[][] test7data() {
    return new Object[][]{
        {5}, {10}, {100}, {1000}
    };
  }

  @Test(dataProvider = "test7data",
      description = "computation fails, continuations of continuations are all cancelled")
  void test7(int n) throws Exception {
    TestTask<Integer> task = new TestTask<>(new Exception());
    Computation<Integer> comp = Computation.newComputation(task);
    List<TestFunction<Integer, Integer>> funs = new java.util.ArrayList<>(n);
    List<Computation<Integer>> conts = new java.util.ArrayList<>(n);
    Computation<Integer> cont = comp;
    for (int i = 0; i < n; i++) {
      TestFunction<Integer, Integer> f = new TestFunction<>(i);
      cont = cont.map(f);
      funs.add(f);
      conts.add(cont);
    }
    task.finish();
    for (Computation<Integer> c : conts) {
      try {
        c.get();
        fail("expected exception");
      } catch (CancellationException e) {
        // OK
      } catch (Exception e) {
        fail("unexpected exception", e);
      }
    }
    assertTrue(task.waitForStart(1000));
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
    long time = 1000 / n; // about 1 sec total time
    for (TestFunction<Integer, Integer> f : funs)
      assertFalse(f.waitForStart(time));
  }

  @DataProvider
  Object[][] test8data() {
    return new Object[][]{
        {5}, {10}, {100}, {256}
    };
  }

  @Test(dataProvider = "test8data",
      description = "computation fails, parallel continuations of parallel continuations are all cancelled")
  void test8(int n) throws Exception {
    TestTask<Integer> task = new TestTask<>(new Exception());
    Computation<Integer> comp = Computation.newComputation(task);
    List<TestFunction<Integer, Integer>> funs = new java.util.ArrayList<>(n);
    List<Computation<Integer>> conts = new java.util.ArrayList<>(n);
    Computation<Integer> cont = comp;
    for (int i = 0; i < n; i++) {
      TestFunction<Integer, Integer> f = new TestFunction<>(i);
      cont = cont.mapParallel(f);
      funs.add(f);
      conts.add(cont);
    }
    task.finish();
    for (Computation<Integer> c : conts) {
      try {
        c.get();
        fail("expected exception");
      } catch (CancellationException e) {
        // OK
      } catch (Exception e) {
        fail("unexpected exception", e);
      }
    }
    assertTrue(task.waitForStart(1000));
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
    long time = 1000 / n; // about 1 sec total time
    for (TestFunction<Integer, Integer> f : funs)
      assertFalse(f.waitForStart(time));
  }

  @Test(description = "continuation fails, its continuation does not run")
  void test9() throws Exception {
    RuntimeException ex = new RuntimeException();
    TestTask<Integer> task = new TestTask<>(42);
    Computation<Integer> comp = Computation.newComputation(task);
    TestFunction<Integer, String> f = new TestFunction<>(ex);
    Computation<String> cont1 = comp.map(f);
    TestFunction<String, String> g = new TestFunction<>("g");
    Computation<String> cont2 = cont1.map(g);
    task.finish();
    assertTrue(f.waitForStart(1000));
    f.finish();
    try {
      cont2.get();
      fail("expected exception");
    } catch (CancellationException e) {
      // OK
    } catch (Exception e) {
      fail("unexpected exception", e);
    }
    try {
      cont1.get();
      fail("expected exception");
    } catch (ExecutionException e) {
      assertSame(e.getCause(), ex);
    } catch (Exception e) {
      fail("unexpected exception", e);
    }
    assertFalse(g.waitForStart(100));
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
  }

  @Test(description = "continuation fails, its parallel continuation does not run")
  void test10() throws Exception {
    RuntimeException ex = new RuntimeException();
    TestTask<Integer> task = new TestTask<>(42);
    Computation<Integer> comp = Computation.newComputation(task);
    TestFunction<Integer, String> f = new TestFunction<>(ex);
    Computation<String> cont1 = comp.map(f);
    TestFunction<String, String> g = new TestFunction<>("g");
    Computation<String> cont2 = cont1.mapParallel(g);
    task.finish();
    assertTrue(f.waitForStart(1000));
    f.finish();
    try {
      cont2.get();
      fail("expected exception");
    } catch (CancellationException e) {
      // OK
    } catch (Exception e) {
      fail("unexpected exception", e);
    }
    try {
      cont1.get();
      fail("expected exception");
    } catch (ExecutionException e) {
      assertSame(e.getCause(), ex);
    } catch (Exception e) {
      fail("unexpected exception", e);
    }
    assertFalse(g.waitForStart(100));
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
  }

  @Test(description = "parallel continuation fails, its continuation does not run")
  void test11() throws Exception {
    RuntimeException ex = new RuntimeException();
    TestTask<Integer> task = new TestTask<>(42);
    Computation<Integer> comp = Computation.newComputation(task);
    TestFunction<Integer, String> f = new TestFunction<>(ex);
    Computation<String> cont1 = comp.mapParallel(f);
    TestFunction<String, String> g = new TestFunction<>("g");
    Computation<String> cont2 = cont1.map(g);
    task.finish();
    assertTrue(f.waitForStart(1000));
    f.finish();
    try {
      cont2.get();
      fail("expected exception");
    } catch (CancellationException e) {
      // OK
    } catch (Exception e) {
      fail("unexpected exception", e);
    }
    try {
      cont1.get();
      fail("expected exception");
    } catch (ExecutionException e) {
      assertSame(e.getCause(), ex);
    } catch (Exception e) {
      fail("unexpected exception", e);
    }
    assertFalse(g.waitForStart(100));
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
    f.getCaller().join(1000);
    assertFalse(f.getCaller().isAlive());
  }

  @Test(description = "continuation added after computation fails does not run")
  void test12() throws Exception {
    TestTask<String> task = new TestTask<>(new Exception());
    Computation<String> comp = Computation.newComputation(task);
    assertTrue(task.waitForStart(1000));
    task.finish();
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
    TestFunction<String, Integer> f = new TestFunction<>(42);
    Computation<Integer> cont = comp.map(f);
    assertTrue(cont.isFinished());
    assertFalse(f.waitForStart(100));
    try {
      cont.get();
      fail("expected exception");
    } catch (CancellationException e) {
      // OK
    } catch (Exception e) {
      fail("unexpected exception", e);
    }
  }

  @Test(description = "parallel continuation added after computation fails does not run")
  void test13() throws Exception {
    TestTask<String> task = new TestTask<>(new Exception());
    Computation<String> comp = Computation.newComputation(task);
    assertTrue(task.waitForStart(1000));
    task.finish();
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
    TestFunction<String, Integer> f = new TestFunction<>(42);
    Computation<Integer> cont = comp.mapParallel(f);
    assertTrue(cont.isFinished());
    assertFalse(f.waitForStart(100));
    try {
      cont.get();
      fail("expected exception");
    } catch (CancellationException e) {
      // OK
    } catch (Exception e) {
      fail("unexpected exception", e);
    }
  }
}

