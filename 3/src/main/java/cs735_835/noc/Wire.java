// $Id: Wire.java 58 2017-03-07 20:17:15Z abcdef $

package cs735_835.noc;

/** A network wire.  Wires represent the connections between routers into a regular grid.  They are
 * unidirectional.
 *
 * @author  Michel Charpentier
 * @see Network
 */
public interface Wire {

  /** Transfers a message.  The message from the outgoing port of the sending router is moved to the
   * incoming port of the receiving router. If there is no such message or the receiving port is not
   * free, this method does nothing.
   * @return true iff a message was actually transferred
   */
  boolean transfer ();
}
