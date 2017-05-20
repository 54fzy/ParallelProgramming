// $Id: ConcurrentSimulatorSuite.java 62 2017-03-27 01:17:40Z abcdef $

package grading;

import cs735_835.noc.Message;
import cs735_835.noc.Network;
import cs735_835.noc.Simulator;
import org.testng.SkipException;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

public abstract class ConcurrentSimulatorSuite extends GenericSimulatorSuite {

  abstract int nbWorkers();

  abstract double expectedMaxTime(int steps, int cores);

  @Test(timeOut = 90000,
      description = "checks that routing steps are done in parallel when possible [3pts]")
  void testConcurrent() {
    if (nbWorkers() == 1)
      throw new SkipException("simulator is sequential");
    Network network = new SlowNetwork(4, 4, 1.0);
    Message m = new Message(1,1, 1, 1, 2, 3);
    network.injectMessage(m);
    Simulator sim = makeSimulator(network);
    long time = System.nanoTime();
    sim.simulate();
    double seconds = (System.nanoTime() - time) / 1e9;
    double expected = expectedMaxTime(5, 16);
    assertTrue(seconds <= expected + 1, // add 1 second margin
        String.format("simulation took too long: %.1f (expected less than %.1f)", seconds, expected));
  }
}
