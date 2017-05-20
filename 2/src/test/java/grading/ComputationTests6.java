// $Id: ComputationTests6.java 57 2017-03-06 14:04:36Z abcdef $

package grading;

public class ComputationTests6 {

  /*
  @Test(description = "task-list")
  void test4() throws Exception {
    TestTask<String> task1 = new TestTask<>("T1");
    TestTask<String> task2 = new TestTask<>("T2");
    Computation<List<String>> comp = Computation.newComputation(Arrays.asList(task1, task2));
    assertTrue(task1.waitForStart(1000));
    assertTrue(task2.waitForStart(1000));
    assertTrue(task1.isRunning() && task2.isRunning());
    assertNotSame(task1.getCaller(), task2.getCaller());
    task2.finish();
    assertTrue(task1.isRunning());
    assertFalse(comp.isFinished());
    task1.finish();
    List<String> results = comp.get();
    assertEquals(results.get(0), "T1");
    assertEquals(results.get(1), "T2");
    assertTrue(comp.isFinished());
    task1.getCaller().join(1000);
    assertFalse(task1.getCaller().isAlive());
    task2.getCaller().join(1000);
    assertFalse(task2.getCaller().isAlive());
  }

  @Test(description = "callback on a task-list")
  void test8() throws Exception {
    TestTask<String> task1 = new TestTask<>("T1");
    TestTask<String> task2 = new TestTask<>("T2");
    Callback callback = new Callback();
    Computation<List<String>> comp = Computation.newComputation(Arrays.asList(task1, task2));
    assertTrue(task1.waitForStart(1000));
    assertTrue(task2.waitForStart(1000));
    assertTrue(task1.isRunning() && task2.isRunning());
    assertNotSame(task1.getCaller(), task2.getCaller());
    comp.onComplete(callback);
    task2.finish();
    assertTrue(task1.isRunning());
    assertFalse(comp.isFinished());
    assertEquals(callback.getCallCount(), 0);
    task1.finish();
    List<String> results = comp.get();
    assertEquals(results.get(0), "T1");
    assertEquals(results.get(1), "T2");
    assertTrue(comp.isFinished());
    assertEquals(callback.getCallCount(), 1);
    task1.getCaller().join(1000);
    assertFalse(task1.getCaller().isAlive());
    task2.getCaller().join(1000);
    assertFalse(task2.getCaller().isAlive());
  }
*/
}

