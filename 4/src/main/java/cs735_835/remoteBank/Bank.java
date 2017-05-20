// $Id: Bank.java 59 2017-03-23 19:03:40Z abcdef $

package cs735_835.remoteBank;

import java.rmi.RemoteException;

/**
 * Remote bank: account creation, retrieval and deletion. Operations can be applied to accounts in
 * three different ways:
 * <p>
 * <ul>
 * <li> by using {@code requestOperation} on the bank itself; </li>
 * <li> by first obtaining a {@code RemoteAccount} from the bank, then applying operations to it;
 * or </li>
 * <li> by first obtaining a {@code BufferedAccount} from the bank, applying operations to it,
 * and finally syncing it. </li>
 * </ul>
 *
 * @author Michel Charpentier
 */
public interface Bank extends java.rmi.Remote {

    /**
     * All accounts.  This may only be an approximation if accounts are added/removed while this call
     * is taking place.
     *
     * @return a newly allocated array with all the accounts in the bank,
     * in increasing order of their numbers
     */
    long[] getAllAccounts() throws RemoteException;

    /**
     * A remote account.
     *
     * @return a remote account (stub) backed by the corresponding account in the bank
     * @throws BankException if the given number does not correspond to a (currently open) account
     */
    RemoteAccount getRemoteAccount(long accountNumber) throws RemoteException;

    /**
     * A buffered account.
     *
     * @return a buffered account backed by the corresponding account in the bank
     * @throws BankException if the given number does not correspond to a (currently open) account
     */
    BufferedAccount getBufferedAccount(long accountNumber) throws RemoteException;

    /**
     * Creates a new account.
     *
     * @return the number of the newly created account, which is guaranteed to be positive and
     * different from all other (currently open) accounts
     */
    long openAccount() throws RemoteException;

    /**
     * Closes an account.
     *
     * @return the balance of the account being closed
     * @throws BankException if the given number does not correspond to an open account
     */
    int closeAccount(long accountNumber) throws RemoteException;

    /**
     * Applies an operation to the account.
     *
     * @return a record of the operation, similar to the records produced by {@link RemoteAccount}
     * @throws BankException if the given number does not correspond to an open account or
     *                       the operation cannot proceed (e.g., withdrawal with insufficient funds)
     */
    Record requestOperation(long number, Operation operation) throws RemoteException;
}
