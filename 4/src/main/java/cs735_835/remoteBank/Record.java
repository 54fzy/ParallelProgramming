// $Id: Record.java 59 2017-03-23 19:03:40Z abcdef $

package cs735_835.remoteBank;

/**
 * Records of bank operations.
 *
 * @author Michel Charpentier
 */
public class Record implements java.io.Serializable {

    private static final long serialVersionUID = 8642569678725441074L;

    static final String DEPOSIT_RECORD_TYPE = "deposit to account #%s";
    static final String WITHDRAWAL_RECORD_TYPE = "withdrawal from account #%s";
    static final String BALANCE_RECORD_TYPE = "balance of account #%s";

    /**
     * Record type (deposit, withdrawal or balance).
     *
     * @see #DEPOSIT_RECORD_TYPE
     * @see #WITHDRAWAL_RECORD_TYPE
     * @see #BALANCE_RECORD_TYPE
     */
    public final String type;

    /**
     * The amount associated with the operation (always nonnegative).
     */
    public final int cents;

    /**
     * String representation.
     *
     * @return a string of the form : {@code "type: $xx.xx"}
     * @see #type
     */
    @Override
    public String toString() {
        return String.format("%s: $%.2f", type, cents / 100.0);
    }

    Record(String type, int cents) {
        this.type = type;
        this.cents = cents;
    }
}
