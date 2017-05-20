// $Id: Simulator.java 58 2017-03-07 20:17:15Z abcdef $

package cs735_835.noc;

import java.util.List;

/**
 * Network-on-chip simulators.
 *
 * @author Michel Charpentier
 */
public interface Simulator {

  /**
   * Runs the simulation.
   *
   * @return a list of all received messages, in order of their ids.
   */
  List<Message> simulate();
}
