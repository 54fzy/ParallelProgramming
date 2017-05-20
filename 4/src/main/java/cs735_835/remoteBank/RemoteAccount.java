// $Id: RemoteAccount.java 59 2017-03-23 19:03:40Z abcdef $

package cs735_835.remoteBank;

import java.rmi.RemoteException;

/**
 * Remote bank account: number, balance, withdraw and deposit.
 *
 * @author Michel Charpentier
 */
public interface RemoteAccount extends java.rmi.Remote {

    /**
     * Account number.  Can be cached as it never changes.
     */
    long accountNumber() throws RemoteException;

    /**
     * Deposit.
     *
     * @param cents the amount deposited (must be nonnegative)
     * @return a record of the form {@code "deposit to account #...: $xx.xx"}
     * @throws IllegalArgumentException if {@code cents} is negative
     * @throws BankException            if the account has been closed
     */
    Record deposit(int cents) throws RemoteException;

    /**
     * Withdrawal.
     *
     * @param cents the amount withdrawn (must be nonnegative)
     * @return a record of the form {@code "withdrawal from account #...: $xx.xx"}
     * @throws IllegalArgumentException if {@code cents} is negative
     * @throws BankException            if the account has been closed or funds are insufficient for
     *                                  the withdrawal
     */
    Record withdraw(int cents) throws RemoteException;

    /**
     * Balance.  The balance of closed accounts is zero.
     *
     * @return a record of the form {@code "balance of account #...: $xx.xx"}
     */
    Record getBalance() throws RemoteException;
}
