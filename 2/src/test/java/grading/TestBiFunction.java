// $Id: TestBiFunction.java 57 2017-03-06 14:04:36Z abcdef $

package grading;

import java.util.function.BiFunction;

public class TestBiFunction<A, B, C> implements BiFunction<A, B, C> {
  private final TestFunction<Integer, C> fun;

  public TestBiFunction(C value, double duration) {
    fun = new TestFunction<>(value, duration);
  }

  public TestBiFunction(C value) {
    this(value, -1);
  }

  public C apply(A x, B y) {
    return fun.apply(x.hashCode() + y.hashCode());
  }

  public Integer getInput() {
    return fun.getInput();
  }

  public Thread getCaller() {
    return fun.getCaller();
  }

  public boolean isRunning() {
    return fun.isRunning();
  }

  public boolean waitForStart(long timeout) throws InterruptedException {
    return fun.waitForStart(timeout);
  }

  public void finish() {
    fun.finish();
  }
}
