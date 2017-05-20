// $Id: Channel.java 51 2017-01-28 22:18:42Z abcdef $

package cs735_835.channels;

/**
 * A channel used by threads to exchange values.
 * It is designed for scenarios with multiple producers and a single consumer, which is why a
 * number of (presumably independent) queues can be used to put values.  Values added to incoming
 * queues are mixed into the outgoing queue (usually in unspecified order).
 * <p>
 * Implementing classes are expected to have a public constructor that takes a single
 * {@code int} (the number of queues).</p>
 * <p>
 * Note that {@code get} is non-blocking, which makes such a channel unsuitable to many
 * scenarios.</p>
 * <p>
 * Instances of this type ought to be thread-safe.</p>
 *
 * @author Michel Charpentier
 */
public interface Channel<T> {

  /**
   * The number of (conceptual) queues.
   * An implementation is free to use fewer actual queues.
   *
   * @return the number of queues in the channel
   */
  int queueCount();

  /**
   * Next channel value.
   *
   * @return the next value or {@code null} if all queues are empty.
   */
  T get();

  /**
   * Puts a value into the specified queue.
   *
   * @param value the value added to the channel
   * @param queue must be between 0 and {@code queueCount()-1}
   * @throws IllegalArgumentException if {@code queue} parameter is invalid.
   * @throws IllegalArgumentException if {@code value} parameter is {@code null}.
   */
  void put(T value, int queue);

  /**
   * The total number of values that have been put into the channel.
   * (This is not the number of values currently present in the queues.)
   *
   * @return the number of values added to the channel
   */
  long totalCount();
}
