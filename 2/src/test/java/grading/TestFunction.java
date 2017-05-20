// $Id: TestFunction.java 53 2017-02-06 13:59:38Z abcdef $

package grading;

import java.util.function.Function;

public class TestFunction<A, B> implements Function<A, B> {
  private final TestTask<B> task;
  private final B result;
  private volatile A input;

  public TestFunction(B value, double duration) {
    result = value;
    task = new TestTask<>(value, duration);
  }

  public TestFunction(B value) {
    this(value, -1);
  }

  public TestFunction(RuntimeException e, double duration) {
    result = null;
    task = new TestTask<>(e, duration);
  }

  public TestFunction(RuntimeException e) {
    this(e, -1);
  }

  public B apply(A x) {
    input = x;
    try {
      task.call();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      assert e instanceof RuntimeException : "functions cannot throw checked exceptions";
      throw (RuntimeException) e;
    }
    return result;
  }

  public A getInput() {
    return input;
  }

  public Thread getCaller() {
    return task.getCaller();
  }

  public boolean isRunning() {
    return task.isRunning();
  }

  public boolean waitForStart(long timeout) throws InterruptedException {
    return task.waitForStart(timeout);
  }

  public void finish() {
    task.finish();
  }
}
