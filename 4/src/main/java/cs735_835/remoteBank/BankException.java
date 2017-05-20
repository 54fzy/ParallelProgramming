// $Id: BankException.java 59 2017-03-23 19:03:40Z abcdef $

package cs735_835.remoteBank;

/**
 * Banking exceptions.  Instances of this class are immutable and serializable.  They have no cause
 * (i.e., {@code getCause} returns {@code null}).
 *
 * @author Michel Charpentier
 */
public class BankException extends RuntimeException {

    private static final long serialVersionUID = -3011912973465419856L;

    BankException(String message) {
        super(message);
    }
}
