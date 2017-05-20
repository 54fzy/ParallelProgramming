// $Id: BankServer.java 59 2017-03-23 19:03:40Z abcdef $

package cs735_835.remoteBank;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * Sample bank server.  For convenience, this server is capable of running its own registry.
 */
public class BankServer {

    public static final String BANK_NAME = "BankofUNH";

    private final LocalBank bank;
    private Registry registry;

    /**
     * Creates a server for the given bank.
     */
    public BankServer(LocalBank bank) {
        this.bank = bank;
    }

    /**
     * Starts the server by binding it to a registry.
     * <p>
     * <ul>
     * <p>
     * <li>If {@code port} is positive, the server attempts to locate a registry at this port.</li>
     * <p>
     * <li>If {@code port} is negative, the server attempts to start a new registry at this
     * port.</li>
     * <p>
     * <li>If {@code port} is 0, the server attempts to start a new registry at a randomly chosen
     * port.</li>
     * <p>
     * </ul>
     *
     * @return the registry port
     */
    public synchronized int start(int port) throws RemoteException {
        if (registry != null)
            throw new IllegalStateException("server already running");
        Registry reg;
        if (port > 0) { // registry already exists
            reg = LocateRegistry.getRegistry(port);
        } else if (port < 0) { // create on given port
            port = -port;
            reg = LocateRegistry.createRegistry(port);
        } else { // create registry on random port
            Random rand = new Random();
            int tries = 0;
            while (true) {
                port = 51076;
//                port = 50000 + rand.nextInt(10000);
                try {
                    reg = LocateRegistry.createRegistry(port);
                    break;
                } catch (RemoteException e) {
                    if (++tries < 10 && e.getCause() instanceof java.net.BindException)
                        continue;
                    throw e;
                }
            }
        }
        reg.rebind(bank.name, bank);
        registry = reg;
        return port;
    }

    /**
     * Stops the server by removing the bank form the registry.  The bank is left exported.
     */
    public synchronized void stop() {
        if (registry != null) {
            try {
                registry.unbind(bank.name);
            } catch (Exception e) {
                System.err.printf("unable to stop: %s%n", e.getMessage());
            } finally {
                registry = null;
            }
        }
    }

    /**
     * Prints a bank status (all accounts with their balances).
     */
    public synchronized void printStatus() {
        System.out.printf("%nBank status:");
        Map<Long, Integer> balances = bank.currentBalances();
        if (balances.isEmpty()) {
            System.out.printf(" no accounts%n");
            return;
        }
        System.out.println();
        for (Map.Entry<Long, Integer> e : balances.entrySet())
            System.out.printf("  account %19d: balance = $%.2f%n", e.getKey(), e.getValue() / 100.0);
    }

    /**
     * Command-line program.  Single (optional) argument is a port number (see {@link #start(int)}).
     */
    public static void main(String[] args) throws Exception {
        int port = 0;
        if (args.length > 0)
            port = Integer.parseInt(args[0]);
        LocalBank bank = new LocalBank(BANK_NAME);
        BankServer server = new BankServer(bank);
        try {
            port = server.start(port);
            System.out.printf("server running on port %d%n", port);
            ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
            exec.scheduleAtFixedRate(server::printStatus, 1, 1, MINUTES);
        } catch (RemoteException e) {
            Throwable t = e.getCause();
            if (t instanceof java.net.ConnectException)
                System.err.println("unable to connect to registry: " + t.getMessage());
            else if (t instanceof java.net.BindException)
                System.err.println("cannot start registry: " + t.getMessage());
            else
                System.err.println("cannot start server: " + e.getMessage());
            UnicastRemoteObject.unexportObject(bank, false);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }
}
