// $Id: TestTermination.java 59 2017-03-23 19:03:40Z abcdef $

package cs735_835;

import cs735_835.remoteBank.Bank;
import cs735_835.remoteBank.LocalBank;
import cs735_835.remoteBank.RemoteAccount;

class TestTermination {

    public static void main(String[] args) throws Exception {
        Bank bank = (Bank) LocalBank.toStub(new LocalBank("TestBank"));
        long id = bank.openAccount();
        System.out.printf("account #%d created%n", id);
        RemoteAccount account = bank.getRemoteAccount(id);
        System.out.println(account);
        System.out.println(account.deposit(2017));
        System.out.println(account.withdraw(2000));
        System.out.println(account.getBalance());
    }
}
