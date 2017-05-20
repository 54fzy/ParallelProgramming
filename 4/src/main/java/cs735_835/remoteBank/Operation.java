// $Id: Operation.java 59 2017-03-23 19:03:40Z abcdef $

package cs735_835.remoteBank;

public abstract class Operation implements java.io.Serializable {

    static final long serialVersionUID = 34523456324456L;

    private static final String balanceString = "BALANCE";

    Operation() { // non-public constructor
    }

    public static Operation deposit(int cents) {
        if(cents<0)throw new IllegalArgumentException("Negative deposit");
        return new Operation() {
            @Override
            public String toString() {
                return "DEPOSIT "+Integer.toString(cents);
            }
        };
    }

    public static Operation withdraw(int cents) {
        if(cents<0)throw new IllegalArgumentException("Negative withdrawal");
        return new Operation() {
            @Override
            public String toString() {
                return "WITHDRAW "+Integer.toString(cents);
            }
        };
    }

    public static Operation getBalance() {
        return new Operation() {
            @Override
            public String toString() {
                return balanceString;
            }
        };
    }
}
