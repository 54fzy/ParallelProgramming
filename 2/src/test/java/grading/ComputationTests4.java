// $Id: ComputationTests4.java 57 2017-03-06 14:04:36Z abcdef $

package grading;

import cs735_835.computation.Computation;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.testng.Assert.*;

public class ComputationTests4 { // continuations (and callbacks) of continuations

  @Test(description = "one callback of one continuation")
  void test1() throws Exception {
    TestTask<Integer> task = new TestTask<>(42);
    Computation<Integer> comp1 = Computation.newComputation(task);
    TestFunction<Integer, String> f = new TestFunction<>("T");
    Computation<String> comp2 = comp1.map(f);
    Callback callback = new Callback();
    comp2.onComplete(callback);
    assertTrue(task.waitForStart(1000));
    Thread.sleep(100);
    assertEquals(callback.getCallCount(), 0);
    task.finish();
    Thread.sleep(100);
    assertEquals(callback.getCallCount(), 0);
    f.finish();
    comp2.get();
    assertEquals(callback.getCallCount(), 1);
    assertSame(task.getCaller(), f.getCaller());
    assertSame(callback.getCaller(), task.getCaller());
    callback.getCaller().join(1000);
    assertFalse(callback.getCaller().isAlive());
  }

  @Test(description = "one callback of one parallel continuation")
  void test2() throws Exception {
    TestTask<Integer> task = new TestTask<>(42);
    Computation<Integer> comp1 = Computation.newComputation(task);
    TestFunction<Integer, String> f = new TestFunction<>("T");
    Computation<String> comp2 = comp1.mapParallel(f);
    Callback callback = new Callback();
    comp2.onComplete(callback);
    assertTrue(task.waitForStart(1000));
    Thread.sleep(100);
    assertEquals(callback.getCallCount(), 0);
    task.finish();
    Thread.sleep(100);
    assertEquals(callback.getCallCount(), 0);
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
    f.finish();
    comp2.get();
    assertEquals(callback.getCallCount(), 1);
    assertNotSame(task.getCaller(), f.getCaller());
    assertSame(callback.getCaller(), f.getCaller());
    callback.getCaller().join(1000);
    assertFalse(callback.getCaller().isAlive());
  }

  @DataProvider
  Object[][] test3data() {
    return new Object[][]{
        {5}, {10}, {50}, {100}, {1000}
    };
  }

  @Test(dataProvider = "test3data", description = "multiple callbacks on multiple continuations")
  void test3(int n) throws Exception {
    TestTask<String> task = new TestTask<>("T");
    Computation<String> comp = Computation.newComputation(task);
    List<TestFunction<String, Integer>> funs = new java.util.ArrayList<>(n);
    List<Computation<Integer>> comps = new java.util.ArrayList<>(n);
    List<Callback> callbacks = new java.util.ArrayList<>(n * n);
    for (int i = 0; i < n; i++) {
      TestFunction<String, Integer> f = new TestFunction<>(i);
      Computation<Integer> c = comp.map(f);
      for (int j = 0; j < n; j++) {
        Callback callback = new Callback();
        c.onComplete(callback);
        callbacks.add(callback);
      }
      funs.add(f);
      comps.add(c);
    }
    task.finish();
    comp.get();
    for (Callback callback : callbacks)
      assertEquals(callback.getCallCount(), 0);
    for (TestFunction<String, Integer> f : funs)
      f.finish();
    for (Computation<Integer> c : comps)
      c.get();
    for (Callback callback : callbacks) {
      assertEquals(callback.getCallCount(), 1);
      assertSame(callback.getCaller(), task.getCaller());
    }
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
  }

  @DataProvider
  Object[][] test4data() {
    return new Object[][]{
        {5}, {10}, {50}, {100}, {200}
    };
  }

  @Test(dataProvider = "test4data",
      description = "multiple callbacks on multiple continuations, some of them parallel")
  void test4(int n) throws Exception {
    TestTask<String> task = new TestTask<>("T");
    Computation<String> comp = Computation.newComputation(task);
    List<TestFunction<String, Integer>> funs1 = new java.util.ArrayList<>(n);
    List<TestFunction<String, Integer>> funs2 = new java.util.ArrayList<>(n);
    List<Computation<Integer>> comps1 = new java.util.ArrayList<>(n);
    List<Computation<Integer>> comps2 = new java.util.ArrayList<>(n);
    List<Callback> callbacks1 = new java.util.ArrayList<>(n * n);
    List<Callback> callbacks2 = new java.util.ArrayList<>(n * n);
    for (int i = 0; i < n; i++) {
      TestFunction<String, Integer> f = new TestFunction<>(i);
      Computation<Integer> c = comp.map(f);
      for (int j = 0; j < n; j++) {
        Callback callback = new Callback();
        c.onComplete(callback);
        callbacks1.add(callback);
      }
      funs1.add(f);
      comps1.add(c);
      f = new TestFunction<>(i);
      c = comp.mapParallel(f);
      for (int j = 0; j < n; j++) {
        Callback callback = new Callback();
        c.onComplete(callback);
        callbacks2.add(callback);
      }
      funs2.add(f);
      comps2.add(c);
    }
    task.finish();
    comp.get();
    for (Callback callback : callbacks1)
      assertEquals(callback.getCallCount(), 0);
    for (Callback callback : callbacks2)
      assertEquals(callback.getCallCount(), 0);
    for (TestFunction<String, Integer> t : funs1)
      t.finish();
    for (TestFunction<String, Integer> t : funs2)
      t.finish();
    for (Computation<Integer> c : comps1)
      c.get();
    for (Computation<Integer> c : comps2)
      c.get();
    for (Callback callback : callbacks1) {
      assertEquals(callback.getCallCount(), 1);
      assertSame(callback.getCaller(), task.getCaller());
    }
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        Callback callback = callbacks2.get(i * n + j);
        assertEquals(callback.getCallCount(), 1);
        assertSame(callback.getCaller(), funs2.get(i).getCaller());
      }
    }
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
    for (TestFunction<String, Integer> t : funs2) {
      t.getCaller().join(1000);
      assertFalse(t.getCaller().isAlive());
    }
  }

  // fun transforms n+1 into n; therefore comp returns n
  Runnable setup5(TestFunction<Integer, Integer> fun, Computation<Integer> comp, int n) {
    if (n == 0)
      return fun::finish;

    TestFunction<Integer, Integer> f = new TestFunction<>(n - 1);
    TestFunction<Integer, Integer> g = new TestFunction<>(n - 1);
    Computation<Integer> comp1 = comp.map(f);
    Computation<Integer> comp2 = comp.mapParallel(g);
    Runnable r1 = setup5(f, comp1, n - 1);
    Runnable r2 = setup5(g, comp2, n - 1);
    return () -> {
      try {
        fun.finish();
        assertEquals(comp.get().intValue(), n);
        assertEquals(fun.getInput().intValue(), n + 1);
        assertTrue(f.waitForStart(1000));
        assertSame(f.getCaller(), fun.getCaller());
        assertTrue(g.waitForStart(1000));
        assertNotSame(g.getCaller(), fun.getCaller());
        r1.run();
        r2.run();
        g.getCaller().join(1000);
        assertFalse(g.getCaller().isAlive());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (ExecutionException e) {
        fail("execution exception", e.getCause());
      }
    };
  }

  // test uses about 2^n threads
  @DataProvider
  Object[][] test5data() {
    return new Object[][]{
        {1}, {3}, {5}, {7}
    };
  }

  @Test(dataProvider = "test5data", description = "each continuations has two continuations, one of them parallel")
  void test5(int n) throws Exception {
    TestTask<Integer> task = new TestTask<>(n + 1);
    Computation<Integer> comp = Computation.newComputation(task);
    TestFunction<Integer, Integer> f = new TestFunction<>(n);
    Computation<Integer> cont = comp.map(f);
    Runnable r = setup5(f, cont, n);
    task.finish();
    r.run();
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
  }
}