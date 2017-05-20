package grading;

import cs735_835.noc.Network;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class SlowNetwork extends Network {

  private final long nanos;

  public SlowNetwork(int w, int h, double delay) {
    super(w, h);
    this.nanos = Math.round(delay * 1e9);
  }

  @Override
  protected void beforeRouter(int row, int col) {
    try {
      NANOSECONDS.sleep(nanos);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
