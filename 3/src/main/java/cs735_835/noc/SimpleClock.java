// $Id: SimpleClock.java 58 2017-03-07 20:17:15Z abcdef $

package cs735_835.noc;

/** A simple clock.  This implementation is not thread-safe.
 *
 * @author  Michel Charpentier
 */
public class SimpleClock implements Clock {

  private int time;

    public int getTime () {
        synchronized (this) {
          return time;
        }
    }

  /** Step the clock forward. */
    public void step() {
        synchronized (this) {
            time += 1;
        }
    }
}
