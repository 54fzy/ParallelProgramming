// $Id: TestTask.java 53 2017-02-06 13:59:38Z abcdef $

package grading;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class TestTask<B> implements Callable<B> {
  private final B result;
  private final Exception exception;
  private final long nanos;
  private final CountDownLatch started, finished;
  private volatile Thread caller;
  private volatile boolean running;

  private TestTask(B value, Exception e, double duration) {
    result = value;
    exception = e;
    started = new CountDownLatch(1);
    finished = new CountDownLatch(1);
    nanos = Math.round(duration * 1e9);
  }

  public TestTask(B value, double duration) {
    this(value, null, duration);
  }

  public TestTask(B value) {
    this(value, -1);
  }

  public TestTask(Exception e, double duration) {
    this(null, e, duration);
  }

  public TestTask(Exception e) {
    this(e, -1);
  }

  public B call() throws Exception {
    long time = System.nanoTime();
    running = true;
    caller = Thread.currentThread();
    started.countDown();
    try {
      if (nanos < 0)
        finished.await();
      else
        finished.await(time - System.nanoTime() + nanos, NANOSECONDS);
    } finally {
      running = false;
    }
    if (exception != null)
      throw exception;
    return result;
  }

  public Thread getCaller() {
    return caller;
  }

  public boolean isRunning() {
    return running;
  }

  public boolean waitForStart(long timeout) throws InterruptedException {
    return started.await(timeout, MILLISECONDS);
  }

  public void finish() {
    finished.countDown();
  }
}
