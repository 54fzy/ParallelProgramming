package grading;

import cs735_835.remoteBank.*;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class AccountTests {

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

  @Test(description = "RemoteAccount operations (sequential)")
  void test1a() throws Exception {
    int n = 16;
    int m = 1000;
    Set<RemoteAccount> accounts = new java.util.HashSet<>(n);
    for (int i = 0; i < n; i++)
      accounts.add(bank.getRemoteAccount(bank.openAccount()));
    for (RemoteAccount account : accounts)
      for (int i = 0; i < m; i++)
        account.deposit(1);
    long sum = 0;
    for (RemoteAccount account : accounts)
      sum += account.getBalance().cents;
    assertEquals(sum, n * m);
    for (RemoteAccount account : accounts)
      assertEquals(bank.closeAccount(account.accountNumber()), m);
    assertEquals(bank.getAllAccounts().length, 0);
  }

  @Test(description = "server-side BufferedAccount operations (sequential)")
  void test1b() throws Exception {
    int n = 16;
    int m = 1000;
    Set<BufferedAccount> accounts = new java.util.HashSet<>(n);
    for (int i = 0; i < n; i++)
      accounts.add(bank.getBufferedAccount(bank.openAccount()));
    for (BufferedAccount account : accounts)
      for (int i = 0; i < m; i++)
        account.deposit(1);
    for (BufferedAccount account : accounts)
      assertEquals(bank.getBufferedAccount(account.accountNumber()).balance(), 0);
    long sum = 0;
    for (BufferedAccount account : accounts) {
      account.sync();
      sum += account.balance();
    }
    assertEquals(sum, n * m);
    for (BufferedAccount account : accounts) {
      assertEquals(bank.getBufferedAccount(account.accountNumber()).balance(), m);
      assertEquals(bank.closeAccount(account.accountNumber()), m);
    }
    assertEquals(bank.getAllAccounts().length, 0);
  }

  @Test(description = "client-side BufferedAccount operations (sequential)")
  void test1c() throws Exception {
    int n = 16;
    int m = 1000;
    Set<RemoteAccount> accounts = new java.util.HashSet<>(n);
    for (int i = 0; i < n; i++)
      accounts.add(bank.getRemoteAccount(bank.openAccount()));
    for (RemoteAccount account : accounts) {
      BufferedAccount buffer = new BufferedAccount(account);
      for (int i = 0; i < m; i++)
        buffer.deposit(1);
      assertEquals(account.getBalance().cents, 0);
      buffer.sync();
    }
    long sum = 0;
    for (RemoteAccount account : accounts)
      sum += account.getBalance().cents;
    assertEquals(sum, n * m);
    for (RemoteAccount account : accounts) {
      assertEquals(bank.getRemoteAccount(account.accountNumber()).getBalance().cents, m);
      assertEquals(bank.getBufferedAccount(account.accountNumber()).balance(), m);
      assertEquals(bank.closeAccount(account.accountNumber()), m);
    }
    assertEquals(bank.getAllAccounts().length, 0);
  }

  @Test(description = "direct operations (sequential)")
  void test1d() throws Exception {
    int n = 16;
    int m = 1000;
    Set<Long> accounts = new java.util.HashSet<>(n);
    Operation deposit = Operation.deposit(1);
    Operation balance = Operation.getBalance();
    for (int i = 0; i < n; i++)
      accounts.add(bank.openAccount());
    for (Long account : accounts)
      for (int i = 0; i < m; i++)
        bank.requestOperation(account, deposit);
    long sum = 0;
    for (Long account : accounts)
      sum += bank.requestOperation(account, balance).cents;
    assertEquals(sum, n * m);
    for (Long account : accounts)
      assertEquals(bank.closeAccount(account), m);
    assertEquals(bank.getAllAccounts().length, 0);
  }

  @Test(description = "RemoteAccount operations (parallel)")
  void test2a() throws Exception {
    int n = 16; // keep the number of threads low or the sockets cannot handle it
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

  @Test(description = "server-side BufferedAccount operations (parallel)")
  void test2b() throws Exception {
    int n = 16; // keep the number of threads low or the sockets cannot handle it
    int m = 1000;
    List<Future<Long>> futures = new java.util.ArrayList<>(n);
    ExecutorService exec = Executors.newCachedThreadPool();
    for (int i = 0; i < n; i++)
      futures.add(exec.submit(() -> {
        BufferedAccount account = bank.getBufferedAccount(bank.openAccount());
        for (int j = 0; j < m; j++)
          account.deposit(1);
        account.sync();
        return account.accountNumber();
      }));
    exec.shutdown();
    for (Future<Long> f : futures) {
      long id = f.get();
      BufferedAccount account = bank.getBufferedAccount(id);
      assertEquals(account.balance(), m);
      assertEquals(bank.closeAccount(id), m);
    }
    assertEquals(bank.getAllAccounts().length, 0);
    exec.awaitTermination(5, SECONDS);
  }

  @Test(description = "client-side BufferedAccount operations (parallel)")
  void test2c() throws Exception {
    int n = 16; // keep the number of threads low or the sockets cannot handle it
    int m = 1000;
    List<Future<Long>> futures = new java.util.ArrayList<>(n);
    ExecutorService exec = Executors.newCachedThreadPool();
    for (int i = 0; i < n; i++)
      futures.add(exec.submit(() -> {
        RemoteAccount account = bank.getRemoteAccount(bank.openAccount());
        BufferedAccount buffer = new BufferedAccount(account);
        for (int j = 0; j < m; j++)
          buffer.deposit(1);
        buffer.sync();
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

  @Test(description = "direct operations (parallel)")
  void test2d() throws Exception {
    int n = 16; // keep the number of threads low or the sockets cannot handle it
    int m = 1000;
    List<Future<Long>> futures = new java.util.ArrayList<>(n);
    Operation deposit = Operation.deposit(1);
    Operation balance = Operation.getBalance();
    ExecutorService exec = Executors.newCachedThreadPool();
    for (int i = 0; i < n; i++)
      futures.add(exec.submit(() -> {
        long account = bank.openAccount();
        for (int j = 0; j < m; j++)
          bank.requestOperation(account, deposit);
        return account;
      }));
    exec.shutdown();
    for (Future<Long> f : futures) {
      long account = f.get();
      assertEquals(bank.requestOperation(account, balance).cents, m);
      assertEquals(bank.closeAccount(account), m);
    }
    assertEquals(bank.getAllAccounts().length, 0);
    exec.awaitTermination(5, SECONDS);
  }

  @Test(description = "single account, multiple RemoteAccount stubs used in parallel")
  void test3a() throws Exception {
    int n = 16;
    int m = 1000;
    long id = bank.openAccount();
    List<Future<Void>> futures = new java.util.ArrayList<>(n);
    ExecutorService exec = Executors.newCachedThreadPool();
    for (int i = 0; i < n; i++)
      futures.add(exec.submit(() -> {
        RemoteAccount account = bank.getRemoteAccount(id);
        for (int j = 0; j < m; j++)
          account.deposit(1);
        return null;
      }));
    exec.shutdown();
    for (Future<Void> f : futures)
      f.get(); // allows for checking exceptions
    RemoteAccount account = bank.getRemoteAccount(id);
    assertEquals(account.getBalance().cents, n * m);
    assertEquals(bank.closeAccount(id), n * m);
    assertEquals(bank.getAllAccounts().length, 0);
    exec.awaitTermination(5, SECONDS);
  }

  @Test(description = "single account, multiple server-side BufferedAccount used in parallel")
  void test3b() throws Exception {
    int n = 16;
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

  @Test(description = "single account, multiple client-side BufferedAccount on the same stub used in parallel")
  void test3c() throws Exception {
    int n = 16;
    int m = 1000;
    RemoteAccount account = bank.getRemoteAccount(bank.openAccount());
    List<Future<Void>> futures = new java.util.ArrayList<>(n);
    ExecutorService exec = Executors.newCachedThreadPool();
    for (int i = 0; i < n; i++)
      futures.add(exec.submit(() -> {
        BufferedAccount buffer = new BufferedAccount(account);
        for (int j = 0; j < m; j++)
          buffer.deposit(1);
        buffer.sync();
        return null;
      }));
    exec.shutdown();
    for (Future<Void> f : futures)
      f.get(); // allows for checking exceptions
    assertEquals(account.getBalance().cents, n * m);
    assertEquals(bank.closeAccount(account.accountNumber()), n * m);
    assertEquals(bank.getAllAccounts().length, 0);
    exec.awaitTermination(5, SECONDS);
  }

  @Test(description = "single account, multiple direct operations in parallel")
  void test3d() throws Exception {
    int n = 16;
    int m = 1000;
    long account = bank.openAccount();
    List<Future<Void>> futures = new java.util.ArrayList<>(n);
    Operation deposit = Operation.deposit(1);
    Operation balance = Operation.getBalance();
    ExecutorService exec = Executors.newCachedThreadPool();
    for (int i = 0; i < n; i++)
      futures.add(exec.submit(() -> {
        for (int j = 0; j < m; j++)
          bank.requestOperation(account, deposit);
        return null;
      }));
    exec.shutdown();
    for (Future<Void> f : futures)
      f.get(); // allows for checking exceptions
    assertEquals(bank.requestOperation(account, balance).cents, n * m);
    assertEquals(bank.closeAccount(account), n * m);
    assertEquals(bank.getAllAccounts().length, 0);
    exec.awaitTermination(5, SECONDS);
  }

  @Test(description = "randomized RemoteAccount operations (parallel)")
  void test4a() throws Exception {
    int n = 16; // keep the number of threads low or the sockets cannot handle it
    int m = 100;
    int p = 100;
    List<Future<Integer>> futures = new java.util.ArrayList<>(n);
    ExecutorService exec = Executors.newCachedThreadPool();
    for (int i = 0; i < n; i++)
      futures.add(exec.submit(() -> {
        Random rand = new Random();
        int balance = 0;
        for (int j = 0; j < p; j++) {
          RemoteAccount account = bank.getRemoteAccount(bank.openAccount());
          for (int k = 0; k < m; k++) {
            int x = rand.nextInt(199) - 100;
            if (x > 0) {
              account.deposit(x);
              balance += x;
            } else if (x < 0) {
              int y = -x;
              if (y <= account.getBalance().cents) {
                account.withdraw(y);
                balance -= y;
              }
            }
          }
        }
        return balance;
      }));
    exec.shutdown();
    int sum1 = 0, sum2 = 0;
    for (Future<Integer> f : futures)
      sum1 += f.get();
    long[] numbers = bank.getAllAccounts();
    assertEquals(numbers.length, n * p);
    for (long id : numbers)
      sum2 += bank.closeAccount(id);
    assertEquals(sum1, sum2);
    assertEquals(bank.getAllAccounts().length, 0);
  }

  @Test(description = "randomized server-side BufferedAccount operations (parallel)")
  void test4b() throws Exception {
    int n = 16; // keep the number of threads low or the sockets cannot handle it
    int m = 100;
    int p = 100;
    List<Future<Integer>> futures = new java.util.ArrayList<>(n);
    ExecutorService exec = Executors.newCachedThreadPool();
    for (int i = 0; i < n; i++)
      futures.add(exec.submit(() -> {
        Random rand = new Random();
        int balance = 0;
        for (int j = 0; j < p; j++) {
          BufferedAccount account = bank.getBufferedAccount(bank.openAccount());
          for (int k = 0; k < m; k++) {
            int x = rand.nextInt(199) - 100;
            if (x > 0) {
              account.deposit(x);
              balance += x;
            } else if (x < 0) {
              int y = -x;
              if (y <= account.balance()) {
                account.withdraw(y);
                balance -= y;
              }
            }
          }
          account.sync();
        }
        return balance;
      }));
    exec.shutdown();
    int sum1 = 0, sum2 = 0;
    for (Future<Integer> f : futures)
      sum1 += f.get();
    long[] numbers = bank.getAllAccounts();
    assertEquals(numbers.length, n * p);
    for (long id : numbers)
      sum2 += bank.closeAccount(id);
    assertEquals(sum1, sum2);
    assertEquals(bank.getAllAccounts().length, 0);
  }

  @Test(description = "randomized client-side BufferedAccount operations (parallel)")
  void test4c() throws Exception {
    int n = 16; // keep the number of threads low or the sockets cannot handle it
    int m = 100;
    int p = 100;
    List<Future<Integer>> futures = new java.util.ArrayList<>(n);
    ExecutorService exec = Executors.newCachedThreadPool();
    for (int i = 0; i < n; i++)
      futures.add(exec.submit(() -> {
        Random rand = new Random();
        RemoteAccount account = bank.getRemoteAccount(bank.openAccount());
        int balance = 0;
        for (int j = 0; j < p; j++) {
          BufferedAccount buffer = new BufferedAccount(account);
          for (int k = 0; k < m; k++) {
            int x = rand.nextInt(199) - 100;
            if (x > 0) {
              buffer.deposit(x);
              balance += x;
            } else if (x < 0) {
              int y = -x;
              if (y <= buffer.balance()) {
                buffer.withdraw(y);
                balance -= y;
              }
            }
          }
          buffer.sync();
        }
        return balance;
      }));
    exec.shutdown();
    int sum1 = 0, sum2 = 0;
    for (Future<Integer> f : futures)
      sum1 += f.get();
    long[] numbers = bank.getAllAccounts();
    assertEquals(numbers.length, n);
    for (long id : numbers)
      sum2 += bank.closeAccount(id);
    assertEquals(sum1, sum2);
    assertEquals(bank.getAllAccounts().length, 0);
  }

  @Test(description = "randomized direct operations (parallel)")
  void test4d() throws Exception {
    int n = 16; // keep the number of threads low or the sockets cannot handle it
    int m = 100;
    int p = 100;
    List<Future<Integer>> futures = new java.util.ArrayList<>(n);
    Operation balanceOp = Operation.getBalance();
    ExecutorService exec = Executors.newCachedThreadPool();
    for (int i = 0; i < n; i++)
      futures.add(exec.submit(() -> {
        Random rand = new Random();
        int balance = 0;
        for (int j = 0; j < p; j++) {
          long account = bank.openAccount();
          for (int k = 0; k < m; k++) {
            int x = rand.nextInt(199) - 100;
            if (x > 0) {
              bank.requestOperation(account, Operation.deposit(x));
              balance += x;
            } else if (x < 0) {
              int y = -x;
              if (y <= bank.requestOperation(account, balanceOp).cents) {
                bank.requestOperation(account, Operation.withdraw(y));
                balance -= y;
              }
            }
          }
        }
        return balance;
      }));
    exec.shutdown();
    int sum1 = 0, sum2 = 0;
    for (Future<Integer> f : futures)
      sum1 += f.get();
    long[] numbers = bank.getAllAccounts();
    assertEquals(numbers.length, n * p);
    for (long id : numbers)
      sum2 += bank.closeAccount(id);
    assertEquals(sum1, sum2);
    assertEquals(bank.getAllAccounts().length, 0);
  }

  @Test(description = "parallel withdrawals using RemoteAccount")
  void test5a() throws Exception {
    int n = 16; // keep the number of threads low or the sockets cannot handle it
    int m = n / 2;
    CountDownLatch ready = new CountDownLatch(n);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Void>> futures = new java.util.ArrayList<>(n);
    ExecutorService exec = Executors.newCachedThreadPool();
    long id = bank.openAccount();
    bank.requestOperation(id, Operation.deposit(m));
    for (int i = 0; i < n; i++)
      futures.add(exec.submit(() -> {
        RemoteAccount account = bank.getRemoteAccount(id);
        ready.countDown();
        start.await();
        account.withdraw(1);
        return null;
      }));
    exec.shutdown();
    ready.await();
    start.countDown();
    int successes = 0, failures = 0;
    for (Future<Void> f : futures) {
      try {
        f.get();
        successes += 1;
      } catch (ExecutionException e) {
        Throwable t = e.getCause();
        if (t instanceof BankException)
          failures += 1;
        else
          fail("unexpected exception", t);
      }
    }
    assertEquals(successes, m);
    assertEquals(failures, n - m);
    assertEquals(bank.closeAccount(id), 0);
    exec.awaitTermination(5, SECONDS);
  }

  @Test(description = "parallel withdrawals using BufferedAccount")
  void test5b() throws Exception {
    int n = 16; // keep the number of threads low or the sockets cannot handle it
    int m = n / 2;
    CountDownLatch ready = new CountDownLatch(n);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Void>> futures = new java.util.ArrayList<>(n);
    ExecutorService exec = Executors.newCachedThreadPool();
    long id = bank.openAccount();
    bank.requestOperation(id, Operation.deposit(m));
    for (int i = 0; i < n; i++)
      futures.add(exec.submit(() -> {
        BufferedAccount account = bank.getBufferedAccount(id);
        account.withdraw(1);
        ready.countDown();
        start.await();
        account.sync();
        return null;
      }));
    exec.shutdown();
    ready.await();
    start.countDown();
    int successes = 0, failures = 0;
    for (Future<Void> f : futures) {
      try {
        f.get();
        successes += 1;
      } catch (ExecutionException e) {
        Throwable t = e.getCause();
        if (t instanceof BankException)
          failures += 1;
        else
          fail("unexpected exception", t);
      }
    }
    assertEquals(successes, m);
    assertEquals(failures, n - m);
    assertEquals(bank.closeAccount(id), 0);
    exec.awaitTermination(5, SECONDS);
  }

  @Test(description = "parallel withdrawals using direct operations")
  void test5c() throws Exception {
    int n = 16; // keep the number of threads low or the sockets cannot handle it
    int m = n / 2;
    CountDownLatch ready = new CountDownLatch(n);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Void>> futures = new java.util.ArrayList<>(n);
    ExecutorService exec = Executors.newCachedThreadPool();
    long id = bank.openAccount();
    bank.requestOperation(id, Operation.deposit(m));
    for (int i = 0; i < n; i++)
      futures.add(exec.submit(() -> {
        Operation w = Operation.withdraw(1);
        ready.countDown();
        start.await();
        bank.requestOperation(id, w);
        return null;
      }));
    exec.shutdown();
    ready.await();
    start.countDown();
    int successes = 0, failures = 0;
    for (Future<Void> f : futures) {
      try {
        f.get();
        successes += 1;
      } catch (ExecutionException e) {
        Throwable t = e.getCause();
        if (t instanceof BankException)
          failures += 1;
        else
          fail("unexpected exception", t);
      }
    }
    assertEquals(successes, m);
    assertEquals(failures, n - m);
    assertEquals(bank.closeAccount(id), 0);
    exec.awaitTermination(5, SECONDS);
  }
}