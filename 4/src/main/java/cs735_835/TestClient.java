// $Id: TestClient.java 59 2017-03-23 19:03:40Z abcdef $

package cs735_835;

import cs735_835.remoteBank.*;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Scanner;

/**
 * Sample client.
 */
public class TestClient {

    private final Bank bank;

    private TestClient(String service)
            throws RemoteException, NotBoundException, MalformedURLException {
        bank = (Bank) java.rmi.Naming.lookup(service);
    }

    private void createAccount() throws RemoteException {
        System.out.printf("account created: %d%n", bank.openAccount());
    }

    private void closeAccount(long id) throws RemoteException {
        System.out.printf("account closed with balance: %d cents%n", bank.closeAccount(id));
    }

    private void runTestsRemote(long number) throws RemoteException {
        RemoteAccount account = bank.getRemoteAccount(number);
        System.out.println(account.getBalance());
        Scanner in = new Scanner(System.in); // not closed on purpose
        try {
            while (true) {
                String line = in.nextLine().trim();
                if (line.isEmpty()) {
                    System.out.println(account.getBalance());
                    continue;
                }
                double amount = Double.parseDouble(line);
                int cents = (int) Math.round(amount * 100);
                if (amount == 0)
                    break;
                if (amount > 0) {
                    System.out.println(account.deposit(cents));
                    System.out.println(account.getBalance());
                } else {
                    try {
                        System.out.println(account.withdraw(-cents));
                        System.out.println(account.getBalance());
                    } catch (BankException e) {
                        System.out.printf("bank exception: %s%n", e.getMessage());
                    }
                }
            }
        } catch (java.util.NoSuchElementException e) {
      /* EOF */
        }
    }

    private void runTestsOperation(long account) throws RemoteException {
        System.out.println(bank.requestOperation(account, Operation.getBalance()));
        Scanner in = new Scanner(System.in); // not closed on purpose
        try {
            while (true) {
                String line = in.nextLine().trim();
                if (line.isEmpty()) {
                    System.out.println(bank.requestOperation(account, Operation.getBalance()));
                    continue;
                }
                double amount = Double.parseDouble(line);
                int cents = (int) Math.round(amount * 100);
                if (amount == 0)
                    break;
                if (amount > 0) {
                    System.out.println(bank.requestOperation(account, Operation.deposit(cents)));
                    System.out.println(bank.requestOperation(account, Operation.getBalance()));
                } else {
                    try {
                        System.out.println(bank.requestOperation(account, Operation.withdraw(-cents)));
                        System.out.println(bank.requestOperation(account, Operation.getBalance()));
                    } catch (BankException e) {
                        System.out.printf("bank exception: %s%n", e.getMessage());
                    }
                }
            }
        } catch (java.util.NoSuchElementException e) {
      /* EOF */
        }
    }

    private void runTestsBuffered(long number) throws RemoteException {
        BufferedAccount account = bank.getBufferedAccount(number);
        System.out.printf("balance: %d cents%n", account.balance());
        Scanner in = new Scanner(System.in); // not closed on purpose
        try {
            while (true) {
                String line = in.nextLine().trim();
                if (line.isEmpty()) {
                    System.out.printf("balance: %d cents%n", account.balance());
                    continue;
                }
                double amount = Double.parseDouble(line);
                int cents = (int) Math.round(amount * 100);
                if (amount == 0) {
                    System.out.println(account.sync());
                    System.out.printf("balance: %d cents%n", account.balance());
                    break;
                }
                if (amount > 0) {
                    account.deposit(cents);
                    System.out.printf("balance: %d cents%n", account.balance());
                } else {
                    try {
                        account.withdraw(-cents);
                        System.out.printf("balance: %d cents%n", account.balance());
                    } catch (BankException e) {
                        System.out.printf("bank exception: %s%n", e.getMessage());
                    }
                }
            }
        } catch (java.util.NoSuchElementException e) {
      /* EOF */
        }
    }

    /**
     * Command-line client.  The first parameter is the name of a bank server (of the form {@code
     * host:port}). The name of the bank is {@link BankServer#BANK_NAME}.
     * <p>
     * <p>If there are no other arguments, a new account is created.  Otherwise, If the second
     * argument is negative, the corresponding account is closed. Otherwise, if the number is
     * positive, an interaction starts with this account.</p>
     * <p>
     * <p>The third argument, if present, specifies how the interaction proceeds:
     * <p>
     * <ul>
     * <p>
     * <li>0: use {@code BufferedAccount}</li>
     * <p>
     * <li>1: use {@code requestOperation}</li>
     * <p>
     * <li>any other value (or no argument): use {@code RemoteAccount}</li>
     * <p>
     * </ul>
     * </p>
     */
    public static void main(String[] args) throws Exception {
        String service = "rmi://" + args[0] + "/" + BankServer.BANK_NAME;

        TestClient client = new TestClient(service);
        try {
            if (args.length == 1) {
                client.createAccount();
                return;
            }
            long id = Long.parseLong(args[1]);
            if (id < 0) {
                client.closeAccount(-id);
                return;
            }
            String type = (args.length > 2) ? args[2] : "R";
            switch (type) {
                case "0":
                    client.runTestsBuffered(id);
                    break;
                case "1":
                    client.runTestsOperation(id);
                    break;
                default:
                    client.runTestsRemote(id);
            }
        } catch (BankException e) {
            System.out.printf("bank exception: %s%n", e.getMessage());
        }
    }
}
