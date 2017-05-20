// $Id: Callback.java 53 2017-02-06 13:59:38Z abcdef $

package grading;

public class Callback implements Runnable {
  private volatile int calls;
  private volatile Thread caller;

  public void run() {
    caller = Thread.currentThread();
    calls++;
  }

  public Thread getCaller() {
    return caller;
  }

  public int getCallCount() {
    return calls;
  }
}
