// $Id: Actor.java 65 2017-04-09 15:34:25Z abcdef $

package cs735_835.actors;

/**
 * Actors.  An actor reacts to incoming messages and sends messages to other actors.  The only way
 * to communicate with an actor is to send a message to it, hence the minimal nature of this interface.
 *
 * @author Michel Charpentier
 */
public interface Actor {

    /**
     * Sends a message to this actor.  Sending is asynchronous and this method returns immediately.
     * It is valid for a thread outside the actor system to call this method, as it offers a way to
     * send data to the actor system from the outside.  (Sending data <em>from</em> the actor system
     * <em>to</em> the outside world requires using <em>mailboxes</em> (see {@link Mailbox}).)
     *
     * @param message the message to be sent; implementations may or may not allow the sending of
     *                {@code null} as a message.
     * @param from    the identity of the sender; it can be different from the actual sender (so replies
     *                will go to a different actor) or {@code null} (so no reply can be sent).
     */
    void tell(Object message, Actor from);
}

