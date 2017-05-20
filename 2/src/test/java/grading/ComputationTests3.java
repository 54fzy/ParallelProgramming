// $Id: ComputationTests3.java 57 2017-03-06 14:04:36Z abcdef $

package grading;

import cs735_835.computation.Computation;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;

import static org.testng.Assert.*;

public class ComputationTests3 { // continuations

  @Test(description = "one continuation, given before completion")
  void test1() throws Exception {
    TestTask<Integer> task = new TestTask<>(42);
    TestFunction<Integer, String> f = new TestFunction<>("O");
    Computation<Integer> comp1 = Computation.newComputation(task);
    Computation<String> comp2 = comp1.map(f);
    assertTrue(task.waitForStart(1000));
    Thread.sleep(100);
    assertTrue(task.isRunning());
    assertFalse(f.isRunning());
    task.finish();
    assertTrue(f.waitForStart(1000));
    f.finish();
    assertSame(task.getCaller(), f.getCaller());
    assertEquals(comp1.get().intValue(), 42);
    assertEquals(comp2.get(), "O");
    assertFalse(f.isRunning());
    assertEquals(f.getInput().intValue(), 42);
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
  }

  @Test(description = "one continuation, given after completion")
  void test2() throws Exception {
    Thread main = Thread.currentThread();
    TestTask<Integer> task = new TestTask<>(42);
    TestFunction<Integer, String> f = new TestFunction<>("O", 0);
    Computation<Integer> comp1 = Computation.newComputation(task);
    task.finish();
    assertEquals(comp1.get().intValue(), 42);
    assertTrue(comp1.isFinished());
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
    Computation<String> comp2 = comp1.map(f);
    assertTrue(comp2.isFinished());
    assertFalse(f.isRunning());
    assertSame(f.getCaller(), main);
    assertEquals(f.getInput().intValue(), 42);
    assertEquals(comp2.get(), "O");
  }

  @DataProvider
  Object[][] test3data() {
    return new Object[][]{
        {10}, {100}, {1000}, {100_000}
    };
  }

  @Test(dataProvider = "test3data", description = "multiple continuations")
  void test3(int n) throws Exception {
    Thread main = Thread.currentThread();
    TestTask<String> task = new TestTask<>("T");
    Computation<String> comp = Computation.newComputation(task);
    List<TestFunction<String, Integer>> funs = new java.util.ArrayList<>(2 * n);
    List<Computation<Integer>> comps = new java.util.ArrayList<>(2 * n);
    for (int i = 0; i < n; i++) {
      TestFunction<String, Integer> f = new TestFunction<>(i);
      funs.add(f);
      comps.add(comp.map(f));
    }
    Thread.sleep(100);
    for (TestFunction<String, Integer> f : funs)
      assertFalse(f.isRunning());
    task.finish();
    assertTrue(funs.get(0).waitForStart(1000));
    for (int i = n, l = 2 * n; i < l; i++) {
      TestFunction<String, Integer> f = new TestFunction<>(i);
      funs.add(f);
      Computation<Integer> c = comp.map(f);
      assertFalse(f.isRunning());
      comps.add(c);
    }
    for (int i = 0, l = 2 * n; i < l; i++) {
      TestFunction<String, Integer> f = funs.get(i);
      Computation<Integer> c = comps.get(i);
      assertTrue(f.waitForStart(1000));
      f.finish();
      assertEquals(f.getInput(), "T");
      assertSame(f.getCaller(), task.getCaller());
      assertEquals(c.get().intValue(), i);
    }
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
    for (int i = 0; i < n; i++) {
      TestFunction<String, Integer> f = new TestFunction<>(i, 0);
      Computation<Integer> c = comp.map(f);
      assertTrue(c.isFinished());
      assertFalse(f.isRunning());
      assertEquals(f.getInput(), "T");
      assertSame(f.getCaller(), main);
      assertEquals(c.get().intValue(), i);
    }
  }

  @DataProvider
  Object[][] test4data() {
    return new Object[][]{
        {5}, {10}, {50}, {100}
    };
  }

  @Test(dataProvider = "test4data", description = "multiple parallel continuations")
  void test4(int n) throws Exception {
    TestTask<String> task = new TestTask<>("T");
    Computation<String> comp = Computation.newComputation(task);
    List<TestFunction<String, Integer>> funs = new java.util.ArrayList<>(2 * n);
    List<Computation<Integer>> comps = new java.util.ArrayList<>(2 * n);
    Set<Thread> threads = new java.util.LinkedHashSet<>(2 * n);
    for (int i = 0; i < n; i++) {
      TestFunction<String, Integer> f = new TestFunction<>(i);
      funs.add(f);
      comps.add(comp.mapParallel(f));
    }
    Thread.sleep(100);
    for (TestFunction<String, Integer> t : funs)
      assertFalse(t.isRunning());
    task.finish();
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
    for (int i = n, l = 2 * n; i < l; i++) {
      TestFunction<String, Integer> f = new TestFunction<>(i);
      funs.add(f);
      comps.add(comp.mapParallel(f));
    }
    for (TestFunction<String, Integer> f : funs) {
      assertTrue(f.waitForStart(1000));
      assertTrue(threads.add(f.getCaller()));
      assertEquals(f.getInput(), "T");
    }
    funs.forEach(TestFunction::finish);
    for (int i = 0, l = 2 * n; i < l; i++)
      assertEquals(comps.get(i).get().intValue(), i);
    for (TestFunction<String, Integer> f : funs)
      assertFalse(f.isRunning());
    assertEquals(threads.size(), 2 * n);
    for (Thread thread : threads) {
      thread.join(1000);
      assertFalse(thread.isAlive());
    }
  }

  @Test(description = "one parallel continuation, given before completion")
  void test5() throws Exception {
    TestTask<Integer> task = new TestTask<>(42);
    TestFunction<Integer, String> f = new TestFunction<>("O");
    Computation<Integer> comp1 = Computation.newComputation(task);
    Computation<String> comp2 = comp1.mapParallel(f);
    assertTrue(task.waitForStart(1000));
    Thread.sleep(100);
    assertFalse(f.isRunning());
    task.finish();
    assertTrue(f.waitForStart(1000));
    assertEquals(comp1.get().intValue(), 42);
    assertNotSame(task.getCaller(), f.getCaller());
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
    f.finish();
    assertEquals(comp2.get(), "O");
    assertEquals(f.getInput().intValue(), 42);
    assertFalse(f.isRunning());
    f.getCaller().join(1000);
    assertFalse(f.getCaller().isAlive());
  }

  @Test(description = "one parallel continuation, given after completion")
  void test6() throws Exception {
    TestTask<Integer> task = new TestTask<>(42);
    TestFunction<Integer, String> f = new TestFunction<>("O");
    Computation<Integer> comp1 = Computation.newComputation(task);
    task.finish();
    assertEquals(comp1.get().intValue(), 42);
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
    Computation<String> comp2 = comp1.mapParallel(f);
    assertTrue(f.waitForStart(1000));
    Thread.sleep(100);
    assertTrue(f.isRunning());
    assertNotSame(task.getCaller(), f.getCaller());
    f.finish();
    assertEquals(comp2.get(), "O");
    assertEquals(f.getInput().intValue(), 42);
    assertFalse(f.isRunning());
    f.getCaller().join(1000);
    assertFalse(f.getCaller().isAlive());
  }

  @Test(description = "two continuations, one regular, one parallel, given before completion")
  void test7() throws Exception {
    TestTask<Integer> task = new TestTask<>(42);
    TestFunction<Integer, String> f = new TestFunction<>("f");
    TestFunction<Integer, String> g = new TestFunction<>("g");
    Computation<Integer> comp1 = Computation.newComputation(task);
    Computation<String> comp2 = comp1.map(f);
    Computation<String> comp3 = comp1.mapParallel(g);
    assertTrue(task.waitForStart(1000));
    Thread.sleep(100);
    assertTrue(task.isRunning());
    assertFalse(f.isRunning());
    assertFalse(g.isRunning());
    task.finish();
    assertTrue(f.waitForStart(1000));
    assertTrue(g.waitForStart(1000));
    assertEquals(f.getInput().intValue(), 42);
    assertEquals(g.getInput().intValue(), 42);
    assertEquals(comp1.get().intValue(), 42);
    assertSame(f.getCaller(), task.getCaller());
    assertNotSame(g.getCaller(), task.getCaller());
    f.finish();
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
    assertEquals(comp2.get(), "f");
    assertFalse(f.isRunning());
    Thread.sleep(100);
    assertTrue(g.isRunning());
    g.finish();
    g.getCaller().join(1000);
    assertFalse(g.getCaller().isAlive());
    assertEquals(comp3.get(), "g");
    assertFalse(g.isRunning());
  }

  @Test(description = "two continuations, one regular, one parallel, given after completion")
  void test8() throws Exception {
    Thread main = Thread.currentThread();
    TestTask<Integer> task = new TestTask<>(42);
    TestFunction<Integer, String> f = new TestFunction<>("f", 0);
    TestFunction<Integer, String> g = new TestFunction<>("g");
    Computation<Integer> comp1 = Computation.newComputation(task);
    assertTrue(task.waitForStart(1000));
    task.finish();
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
    assertEquals(comp1.get().intValue(), 42);
    Computation<String> comp3 = comp1.mapParallel(g);
    Computation<String> comp2 = comp1.map(f);
    assertTrue(comp2.isFinished());
    assertFalse(f.isRunning());
    assertEquals(comp2.get(), "f");
    assertEquals(f.getInput().intValue(), 42);
    assertSame(f.getCaller(), main);
    assertTrue(g.waitForStart(1000));
    assertEquals(g.getInput().intValue(), 42);
    assertNotSame(g.getCaller(), main);
    Thread.sleep(100);
    assertTrue(g.isRunning());
    g.finish();
    g.getCaller().join(1000);
    assertFalse(g.getCaller().isAlive());
    assertEquals(comp3.get(), "g");
    assertFalse(g.isRunning());
    g.getCaller().join(1000);
    assertFalse(g.getCaller().isAlive());
  }

  @DataProvider
  Object[][] test9data() {
    return new Object[][]{
        {5}, {10}, {50}, {100}, {200}
    };
  }

  @Test(dataProvider = "test9data", description = "multiple continuations, some parallel")
  void test9(int n) throws Exception {
    TestTask<String> task = new TestTask<>("T");
    Computation<String> comp = Computation.newComputation(task);
    List<TestFunction<String, Integer>> funs = new java.util.ArrayList<>(2 * n);
    List<Computation<Integer>> comps = new java.util.ArrayList<>(2 * n);
    Set<Thread> threads = new java.util.LinkedHashSet<>(n);
    for (int i = 0; i < n; i++) {
      TestFunction<String, Integer> f = new TestFunction<>(i);
      funs.add(f);
      comps.add(comp.map(f));
    }
    for (int i = n, l = 2 * n; i < l; i++) {
      TestFunction<String, Integer> f = new TestFunction<>(i);
      funs.add(f);
      comps.add(comp.mapParallel(f));
    }
    task.finish();
    assertTrue(funs.get(0).waitForStart(1000));
    for (int i = n, l = 2 * n; i < l; i++) {
      TestFunction<String, Integer> f = funs.get(i);
      assertTrue(f.waitForStart(1000));
      assertTrue(threads.add(f.getCaller()));
    }
    assertEquals(threads.size(), n);
    assertFalse(threads.contains(task.getCaller()));
    for (Thread thread : threads)
      assertTrue(thread.isAlive());
    for (int i = 0, l = 2 * n; i < l; i++) {
      TestFunction<String, Integer> f = funs.get(i);
      Computation<Integer> c = comps.get(i);
      f.finish();
      assertEquals(c.get().intValue(), i);
      assertEquals(f.getInput(), "T");
    }
    for (int i = 0; i < n; i++)
      assertEquals(funs.get(i).getCaller(), task.getCaller());
    task.getCaller().join(1000);
    assertFalse(task.getCaller().isAlive());
    for (Thread thread : threads) {
      thread.join(1000);
      assertFalse(thread.isAlive());
    }
  }
}