import cs735_835.remoteBank.*;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.testng.Assert.*;

public class SampleTests {

    Bank bank;
    LocalBank server;

    @BeforeMethod
    void setup() throws RemoteException {
        server = new LocalBank("TestBank");
        bank = (Bank) LocalBank.toStub(server);
    }

    @AfterMethod
    void teardown() throws RemoteException {
        LocalBank.unexportObject(server, false);
    }

    @Test(description = "balance/deposit/withdrawal on a RemoteAccount")
    void test1() throws Exception {
        long id = bank.openAccount();
        assertTrue(Arrays.equals(bank.getAllAccounts(), new long[]{id}));
        RemoteAccount account = bank.getRemoteAccount(id);
        assertEquals(account.accountNumber(), id);
        Record r = account.getBalance();
        assertEquals(r.cents, 0);
        assertEquals(r.toString(), "balance of account #" + id + ": $0.00");
        r = account.deposit(42);
        assertEquals(r.cents, 42);
        assertEquals(r.toString(), "deposit to account #" + id + ": $0.42");
        r = account.getBalance();
        assertEquals(r.cents, 42);
        assertEquals(r.toString(), "balance of account #" + id + ": $0.42");
        assertEquals(bank.closeAccount(id), 42);
        assertEquals(bank.getAllAccounts().length, 0);
    }

    @Test(description = "records from BufferedAccount operations")
    void test2() throws Exception {
        long id = bank.openAccount();
        BufferedAccount account = bank.getBufferedAccount(id);
        account.deposit(117);
        account.deposit(2);
        assertEquals(account.balance(), 119);
        Record r = account.sync();
        assertEquals(account.balance(), 119);
        assertEquals(r.cents, 119);
        assertEquals(r.toString(), "deposit to account #" + id + ": $1.19");
        account.withdraw(33);
        assertEquals(account.balance(), 86);
        r = account.sync();
        assertEquals(account.balance(), 86);
        assertEquals(r.cents, 33);
        assertEquals(r.toString(), "withdrawal from account #" + id + ": $0.33");
        account.withdraw(11);
        account.deposit(11);
        assertNull(account.sync());
        assertEquals(bank.closeAccount(id), 86);
    }

    @Test(description = "two independent accounts used directly")
    void test3() throws Exception {
        Operation balance = Operation.getBalance();
        long account1 = bank.openAccount();
        long account2 = bank.openAccount();
        Record r1 = bank.requestOperation(account1, Operation.deposit(123));
        assertEquals(bank.requestOperation(account1, balance).cents, 123);
        assertEquals(bank.requestOperation(account2, balance).cents, 0);
        Record r2 = bank.requestOperation(account2, Operation.deposit(321));
        assertEquals(bank.requestOperation(account1, balance).cents, 123);
        assertEquals(bank.requestOperation(account2, balance).cents, 321);
        assertEquals(r1.cents, 123);
        assertEquals(r2.cents, 321);
        assertEquals(r1.toString(), "deposit to account #" + account1 + ": $1.23");
        assertEquals(r2.toString(), "deposit to account #" + account2 + ": $3.21");
        assertEquals(bank.closeAccount(account1), 123);
        r2 = bank.requestOperation(account2, Operation.withdraw(21));
        assertEquals(r2.cents, 21);
        assertEquals(r2.toString(), "withdrawal from account #" + account2 + ": $0.21");
        assertEquals(bank.requestOperation(account2, balance).cents, 300);
        assertEquals(bank.closeAccount(account2), 300);
        assertEquals(bank.getAllAccounts().length, 0);
    }

    @Test(description = "balance returned at closing time")
    void test4() throws Exception {
        long id = bank.openAccount();
        RemoteAccount account = bank.getRemoteAccount(id);
        account.deposit(42);
        assertEquals(bank.closeAccount(id), 42);
    }

    @Test(description = "RemoteAccount operations (parallel)")
    void test5() throws Exception {
        int n = 32; // keep the number of threads low or the sockets cannot handle it
        int m = 1000;
        List<Future<Long>> futures = new java.util.ArrayList<>(n);
        ExecutorService exec = Executors.newCachedThreadPool();
        for (int i = 0; i < n; i++)
            futures.add(exec.submit(() -> {
                RemoteAccount account = bank.getRemoteAccount(bank.openAccount());
                for (int j = 0; j < m; j++)
                    account.deposit(1);
                return account.accountNumber();
            }));
        exec.shutdown();
        for (Future<Long> f : futures) {
            long id = f.get();
            RemoteAccount account = bank.getRemoteAccount(id);
            assertEquals(account.getBalance().cents, m);
            assertEquals(bank.closeAccount(id), m);
        }
        assertEquals(bank.getAllAccounts().length, 0);
        exec.awaitTermination(5, SECONDS);
    }

    @Test(description = "single account, multiple server-side BufferedAccount used in parallel")
    void test6() throws Exception {
        int n = 32;
        int m = 1000;
        long id = bank.openAccount();
        List<Future<Void>> futures = new java.util.ArrayList<>(n);
        ExecutorService exec = Executors.newCachedThreadPool();
        for (int i = 0; i < n; i++)
            futures.add(exec.submit(() -> {
                BufferedAccount account = bank.getBufferedAccount(id);
                for (int j = 0; j < m; j++)
                    account.deposit(1);
                account.sync();
                return null;
            }));
        exec.shutdown();
        for (Future<Void> f : futures)
            f.get(); // allows for checking exceptions
        BufferedAccount account = bank.getBufferedAccount(id);
        assertEquals(account.balance(), n * m);
        assertEquals(bank.closeAccount(id), n * m);
        assertEquals(bank.getAllAccounts().length, 0);
        exec.awaitTermination(5, SECONDS);
    }

    @Test(expectedExceptions = BankException.class)
    void test7() throws Exception {
        RemoteAccount account = bank.getRemoteAccount(bank.openAccount());
        account.deposit(42);
        account.withdraw(133);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    void test8() throws Exception {
        Operation.deposit(-5);
    }
}
