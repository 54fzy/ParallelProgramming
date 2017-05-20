package grading;

import cs735_835.remoteBank.*;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Set;

import static org.testng.Assert.*;

public class BankTests {

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
  void test1a() throws Exception {
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

  @Test(description = "balance/deposit/withdrawal on a BufferedAccount")
  void test1b() throws Exception {
    long id = bank.openAccount();
    BufferedAccount account = bank.getBufferedAccount(id);
    assertEquals(account.accountNumber(), id);
    assertEquals(account.balance(), 0);
    account.deposit(42);
    assertEquals(account.balance(), 42);
    Record r = account.sync();
    assertEquals(r.cents, 42);
    assertEquals(r.toString(), "deposit to account #" + id + ": $0.42");
    assertEquals(bank.closeAccount(id), 42);
    assertEquals(bank.getAllAccounts().length, 0);
  }

  @Test(description = "balance/deposit/withdrawal by direct operations")
  void test1c() throws Exception {
    long account = bank.openAccount();
    Record r = bank.requestOperation(account, Operation.getBalance());
    assertEquals(r.cents, 0);
    assertEquals(r.toString(), "balance of account #" + account + ": $0.00");
    r = bank.requestOperation(account, Operation.deposit(42));
    assertEquals(r.cents, 42);
    assertEquals(r.toString(), "deposit to account #" + account + ": $0.42");
    r = bank.requestOperation(account, Operation.getBalance());
    assertEquals(r.cents, 42);
    assertEquals(r.toString(), "balance of account #" + account + ": $0.42");
    assertEquals(bank.closeAccount(account), 42);
    assertEquals(bank.getAllAccounts().length, 0);
  }

  @Test(description = "records from RemoteAccount operations")
  void test2a() throws Exception {
    long id = bank.openAccount();
    RemoteAccount account = bank.getRemoteAccount(id);
    Record r1 = account.deposit(117);
    Record r2 = account.deposit(2);
    Record r3 = account.withdraw(33);
    assertEquals(r1.cents, 117);
    assertEquals(r2.cents, 2);
    assertEquals(r3.cents, 33);
    assertEquals(r1.toString(), "deposit to account #" + id + ": $1.17");
    assertEquals(r2.toString(), "deposit to account #" + id + ": $0.02");
    assertEquals(r3.toString(), "withdrawal from account #" + id + ": $0.33");
    Record r = account.getBalance();
    assertEquals(r.cents, 86);
    assertEquals(r.toString(), "balance of account #" + id + ": $0.86");
    assertEquals(bank.closeAccount(id), 86);
  }

  @Test(description = "records from BufferedAccount operations")
  void test2b() throws Exception {
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

  @Test(description = "records from direct operations")
  void test2c() throws Exception {
    long account = bank.openAccount();
    Operation balance = Operation.getBalance();
    bank.requestOperation(account, Operation.deposit(117));
    Record r = bank.requestOperation(account, Operation.deposit(2));
    assertEquals(r.cents, 2);
    assertEquals(r.toString(), "deposit to account #" + account + ": $0.02");
    r = bank.requestOperation(account, balance);
    assertEquals(r.cents, 119);
    assertEquals(r.toString(), "balance of account #" + account + ": $1.19");
    r = bank.requestOperation(account, Operation.withdraw(33));
    assertEquals(r.cents, 33);
    assertEquals(r.toString(), "withdrawal from account #" + account + ": $0.33");
    r = bank.requestOperation(account, balance);
    assertEquals(r.cents, 86);
    assertEquals(r.toString(), "balance of account #" + account + ": $0.86");
    assertEquals(bank.closeAccount(account), 86);
  }

  @Test(description = "two independent RemoteAccount instances")
  void test3a() throws Exception {
    long id1 = bank.openAccount();
    assertTrue(Arrays.equals(bank.getAllAccounts(), new long[]{id1}));
    long id2 = bank.openAccount();
    assertTrue(Arrays.equals(bank.getAllAccounts(),
        (id1 < id2) ? new long[]{id1, id2} : new long[]{id2, id1}));
    assertNotEquals(id1, id2);
    RemoteAccount account1 = bank.getRemoteAccount(id1);
    RemoteAccount account2 = bank.getRemoteAccount(id2);
    assertNotEquals(account1, account2);
    Record r1 = account1.deposit(123);
    assertEquals(account1.getBalance().cents, 123);
    assertEquals(account2.getBalance().cents, 0);
    Record r2 = account2.deposit(321);
    assertEquals(account1.getBalance().cents, 123);
    assertEquals(account2.getBalance().cents, 321);
    assertEquals(r1.cents, 123);
    assertEquals(r2.cents, 321);
    assertEquals(r1.toString(), "deposit to account #" + id1 + ": $1.23");
    assertEquals(r2.toString(), "deposit to account #" + id2 + ": $3.21");
    assertEquals(bank.closeAccount(id1), 123);
    assertTrue(Arrays.equals(bank.getAllAccounts(), new long[]{id2}));
    account2.withdraw(21);
    assertEquals(account2.getBalance().cents, 300);
    assertEquals(bank.closeAccount(id2), 300);
    assertEquals(bank.getAllAccounts().length, 0);
  }

  @Test(description = "two independent BufferedAccount instances")
  void test3b() throws Exception {
    long id1 = bank.openAccount();
    long id2 = bank.openAccount();
    BufferedAccount account1 = bank.getBufferedAccount(id1);
    BufferedAccount account2 = bank.getBufferedAccount(id2);
    assertNotEquals(account1, account2);
    account1.deposit(123);
    assertEquals(account1.balance(), 123);
    assertEquals(account2.balance(), 0);
    account2.deposit(321);
    assertEquals(account1.balance(), 123);
    assertEquals(account2.balance(), 321);
    Record r1 = account1.sync();
    Record r2 = account2.sync();
    assertEquals(r1.cents, 123);
    assertEquals(r2.cents, 321);
    assertEquals(r1.toString(), "deposit to account #" + id1 + ": $1.23");
    assertEquals(r2.toString(), "deposit to account #" + id2 + ": $3.21");
    assertEquals(bank.closeAccount(id1), 123);
    assertTrue(Arrays.equals(bank.getAllAccounts(), new long[]{id2}));
    account2.withdraw(21);
    assertEquals(account2.balance(), 300);
    r2 = account2.sync();
    assertEquals(r2.cents, 21);
    assertEquals(r2.toString(), "withdrawal from account #" + id2 + ": $0.21");
    assertEquals(bank.closeAccount(id2), 300);
    assertEquals(bank.getAllAccounts().length, 0);
  }

  @Test(description = "two independent accounts used directly")
  void test3c() throws Exception {
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

  @Test(description = "two RemoteAccount stubs on the same account")
  void test4() throws Exception {
    long id = bank.openAccount();
    RemoteAccount account1a = bank.getRemoteAccount(id);
    RemoteAccount account1b = bank.getRemoteAccount(id);
    assertNotSame(account1a, account1b);
    assertEquals(account1a, account1b);
    account1a.deposit(100);
    assertEquals(account1a.getBalance().cents, 100);
    assertEquals(account1b.getBalance().cents, 100);
    account1b.withdraw(75);
    assertEquals(account1a.getBalance().cents, 25);
    assertEquals(account1b.getBalance().cents, 25);
    assertEquals(bank.closeAccount(id), 25);
  }

  @Test(description = "1000 accounts")
  void test5() throws Exception {
    int n = 1000;
    long[] numbers = new long[n];
    Set<RemoteAccount> accounts = new java.util.HashSet<>(n);
    for (int i = 0; i < n; i++) {
      long id = bank.openAccount();
      numbers[i] = id;
      accounts.add(bank.getRemoteAccount(id));
    }
    Arrays.sort(numbers);
    assertTrue(Arrays.equals(bank.getAllAccounts(), numbers));
    for (RemoteAccount account : accounts) {
      account.deposit(2);
      account.withdraw(1);
    }
    for (RemoteAccount account : accounts)
      assertEquals(bank.closeAccount(account.accountNumber()), 1);
    assertEquals(bank.getAllAccounts().length, 0);
  }

  @Test(description = "Operation.getBalance() is a singleton")
  void test6() throws Exception {
    assertSame(Operation.getBalance(), Operation.getBalance());
  }

  @Test(description = "balance returned at closing time")
  void test7() throws Exception {
    long id = bank.openAccount();
    RemoteAccount account = bank.getRemoteAccount(id);
    account.deposit(42);
    assertEquals(bank.closeAccount(id), 42);
  }

  @Test(description = "balance is zero after closing")
  void test8() throws Exception {
    long id = bank.openAccount();
    RemoteAccount account = bank.getRemoteAccount(id);
    account.deposit(42);
    bank.closeAccount(id);
    Record r = account.getBalance();
    assertEquals(r.cents, 0);
  }

  @Test(description = "buffered operations are buffered")
  void test9() throws Exception {
    RemoteAccount account = bank.getRemoteAccount(bank.openAccount());
    BufferedAccount buffer = new BufferedAccount(account);
    account.deposit(42);
    buffer.deposit(38);
    assertEquals(account.getBalance().cents, 42);
    assertEquals(buffer.balance(), 38);
    buffer.deposit(100);
    assertEquals(buffer.sync().cents, 138);
    assertEquals(buffer.balance(), 180);
  }

  @Test(description = "new balance acquired when syncing buffered accounts")
  void test10() throws Exception {
    long id = bank.openAccount();
    BufferedAccount buffered = bank.getBufferedAccount(id);
    buffered.deposit(100);
    bank.requestOperation(id, Operation.deposit(42));
    buffered.withdraw(50);
    buffered.withdraw(50);
    assertEquals(buffered.balance(), 0);
    assertNull(buffered.sync());
    assertEquals(buffered.balance(), 42);
  }
}
