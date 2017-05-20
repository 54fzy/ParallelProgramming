package grading;

import cs735_835.remoteBank.*;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.rmi.RemoteException;

public class ExceptionTests {

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

  @Test(expectedExceptions = BankException.class)
  void test1a() throws Exception {
    RemoteAccount account = bank.getRemoteAccount(bank.openAccount());
    account.deposit(42);
    account.withdraw(133);
  }

  @Test(expectedExceptions = BankException.class)
  void test1b() throws Exception {
    BufferedAccount account = bank.getBufferedAccount(bank.openAccount());
    account.deposit(42);
    account.withdraw(133);
  }

  @Test(expectedExceptions = BankException.class)
  void test2() throws Exception {
    long id = bank.openAccount();
    bank.closeAccount(id);
    bank.closeAccount(id);
  }

  @Test(expectedExceptions = BankException.class)
  void test3() throws Exception {
    bank.closeAccount(1234);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  void test4() throws Exception {
    Operation.deposit(-5);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  void test5() throws Exception {
    Operation.withdraw(-3);
  }

  @Test(expectedExceptions = BankException.class)
  void test6() throws Exception {
    long id = bank.openAccount();
    RemoteAccount account = bank.getRemoteAccount(id);
    bank.closeAccount(id);
    account.deposit(1);
  }

  @Test(expectedExceptions = BankException.class)
  void test7() throws Exception {
    long id = bank.openAccount();
    RemoteAccount account = bank.getRemoteAccount(id);
    account.deposit(100);
    bank.closeAccount(id);
    account.withdraw(1);
  }
}

