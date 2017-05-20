// $Id: NaiveExecSimulatorSuite.java 62 2017-03-27 01:17:40Z abcdef $

package grading;

import cs735_835.noc.Network;
import cs735_835.noc.Simulator;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.concurrent.TimeUnit.SECONDS;

public class NaiveExecSimulatorSuite extends ConcurrentSimulatorSuite {

  @DataProvider
  static Object[][] workers() {
    return new Object[][]{
        {1}, {2}, {4}, {8}, {16}
    };
  }

  @Factory(dataProvider = "workers")
  public NaiveExecSimulatorSuite(int nbWorkers) {
    this.workers = nbWorkers;
  }

  Simulator makeSimulator(Network network) {
    return new cs735_835.noc.NaiveExecSimulator(network, exec);
  }

  final int workers;

  ExecutorService exec;

  @BeforeMethod
  void setup() {
    exec = Executors.newFixedThreadPool(workers);
  }

  @AfterMethod
  void teardown() throws InterruptedException {
    exec.shutdown();
    if (!exec.awaitTermination(1, SECONDS)) {
      System.err.println("SHUTTING DOWN EXECUTOR!");
      exec.shutdownNow();
    }
  }

  int nbWorkers() {
    return workers;
  }

  double expectedMaxTime(int steps, int cores) {
    return Math.ceil((double) cores / workers) * steps;
  }
}
