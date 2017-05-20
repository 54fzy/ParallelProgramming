// $Id: ExecSimulatorSuite.java 60 2017-03-26 21:08:15Z abcdef $

package grading;

import cs735_835.noc.Network;
import cs735_835.noc.Simulator;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;

public class ExecSimulatorSuite extends NaiveExecSimulatorSuite {

  @DataProvider
  static Object[][] workersTasks() {
    return new Object[][]{
        {1, 1},
        {2, 2}, {2, 4}, {2, 10},
        {4, 4}, {4, 8}, {4, 20}, {3, 10}
    };
  }

  @Factory(dataProvider = "workersTasks")
  public ExecSimulatorSuite(int nbWorkers, int nbTasks) {
    super(nbWorkers);
    this.tasks = nbTasks;
  }

  Simulator makeSimulator(Network network) {
    return new cs735_835.noc.ExecSimulator(network, exec, tasks);
  }

  final int tasks;

  @Override
  double expectedMaxTime(int steps, int cores) {
    return (cores < tasks)
        ? super.expectedMaxTime(steps, cores)
        : Math.ceil((double) cores / tasks) * Math.ceil((double) tasks / workers) * steps;
  }
}
