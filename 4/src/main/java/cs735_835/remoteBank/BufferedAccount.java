// $Id: BufferedAccount.java 59 2017-03-23 19:03:40Z abcdef $

package cs735_835.remoteBank;

import java.rmi.RemoteException;

public class BufferedAccount implements java.io.Serializable {

    private final long id;
    private final RemoteAccount acc;
    private int balance;
    private int tempB;
    public BufferedAccount(RemoteAccount account) throws RemoteException {
        synchronized (this){
            acc = account;
            id = acc.accountNumber();
            setBalance();
            tempB = balance;
        }
    }

    public long accountNumber() {
        return id;
    }

    public void deposit(int cents) {
        synchronized (this) {
            if (cents < 0) throw new IllegalArgumentException("Negative Deposit");
            tempB += cents;
        }
    }

    public void withdraw(int cents){
        synchronized (this) {
            if (cents < 0) throw new IllegalArgumentException("Negative Withdrawal");
            if (cents > tempB) throw new BankException("Insufficent Funds");
            tempB -= cents;
        }
    }

    public int balance(){
        return tempB;
    }

    private void setBalance(){
        String[] tokens;
        try {
            tokens = acc.getBalance().toString().split(" ");
        } catch (RemoteException e) {
            return;
        }
        balance = (int)(
                Double.parseDouble(
                        tokens[tokens.length-1].replaceAll("\\$", "")
                )*100);
    }

    public Record sync() throws RemoteException {
        Record r;
        synchronized (this){
            if(tempB<balance){
                r = acc.withdraw(balance - tempB);
                setBalance();
                return r;
            }
            if(tempB>balance){
                r = acc.deposit(tempB - balance);
                setBalance();
                return r;
            }
        }
        setBalance();
        return null;
    }
}
