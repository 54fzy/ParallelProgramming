// $Id: SeqSimulatorSuite.java 60 2017-03-26 21:08:15Z abcdef $

package grading;

import cs735_835.noc.Network;
import cs735_835.noc.Simulator;

public class SeqSimulatorSuite extends GenericSimulatorSuite {

  Simulator makeSimulator(Network network) {
    return new cs735_835.noc.SeqSimulator(network);
  }
}
