// $Id: LocalBank.java 59 2017-03-23 19:03:40Z abcdef $

package cs735_835.remoteBank;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class LocalBank extends UnicastRemoteObject implements Bank {

    public class RAccount extends UnicastRemoteObject implements RemoteAccount{
        private int balance;
        private final long id;
        private volatile boolean open;
        private final Object balLock;

        RAccount(long i, int b) throws RemoteException{
            synchronized (this) {
                id = i;
                balance = b;
                open = true;
                balLock = new Object();
            }
        }

        void close() throws RemoteException{
            open = false;
        }

        @Override
        public long accountNumber() throws RemoteException {
            return id;
        }

        @Override
        public Record deposit(int cents) throws RemoteException {
            synchronized (balLock) {
                if (!open) throw new BankException("Account closed");
                if (cents<0) throw new IllegalArgumentException("Negative Deposit");
                balance += cents;
//                return new Record(Record.DEPOSIT_RECORD_TYPE.replaceAll("%s", Long.toString(id)), cents);
                return new Record(String.format(Record.DEPOSIT_RECORD_TYPE, id), cents);
            }
        }

        @Override
        public Record withdraw(int cents) throws RemoteException {
            synchronized (balLock) {
                if (!open) throw new BankException("Account closed");
                if (cents<0) throw new IllegalArgumentException("Negative Withdrawal");
                if (cents>balance) throw new BankException("Insufficient funds");
                balance -= cents;
//                return new Record(Record.WITHDRAWAL_RECORD_TYPE.replaceAll("%s", Long.toString(id)), cents);
                return new Record(String.format(Record.WITHDRAWAL_RECORD_TYPE, id), cents);
            }
        }

        @Override
        public Record getBalance() throws RemoteException {
            synchronized (this) {
                if (!open) return new Record(String.format(Record.BALANCE_RECORD_TYPE, id), 0);
                return new Record(String.format(Record.BALANCE_RECORD_TYPE, id), balance);
            }
        }
    }

    final String name;

    private final ConcurrentHashMap<Long, RAccount> accounts;
    private final AtomicLong accountsCount;
    public LocalBank(String name) throws RemoteException {
        this.name = name;
        synchronized (this){
            accounts = new ConcurrentHashMap<>();
            accountsCount = new AtomicLong(0);
        }
    }

    @Override
    public String toString() {
        return "LocalBank: " + name;
    }

    synchronized public long[] getAllAccounts() {
        ConcurrentHashMap.KeySetView<Long, RAccount> keyset = accounts.keySet();
        long[] keys = new long[keyset.size()];
        int idx = 0;
//        synchronized (accountsLock){
        for (Long key : keyset) {
                keys[idx] = key;
                idx++;
            }
//        }
        return keys;
    }

    synchronized public long openAccount() throws RemoteException {
//        synchronized (accountsLock){
            accountsCount.incrementAndGet();
            try {
                accounts.put(accountsCount.get(), new RAccount(accountsCount.get(), 0));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return accountsCount.get();
//        }
    }

    synchronized public RemoteAccount getRemoteAccount(long number) throws RemoteException {
        RAccount acc = accounts.get(number);
        if(acc==null)throw new BankException("Account not found");
        return acc;
    }

    synchronized public BufferedAccount getBufferedAccount(long number) throws RemoteException {
        return new BufferedAccount(accounts.get(number));
    }

    synchronized public int closeAccount(long number) throws RemoteException {
        int balance = -1;
        RAccount ra = accounts.remove(number);
        if(ra!=null){
            ra.close();
            balance = ra.balance;
        }
        return balance;
    }

    synchronized public Record requestOperation(long number, Operation operation) {
        RAccount acc = accounts.get(number);
        if(acc==null)throw new BankException("Account not found");
            String op = operation.toString();
            String[] tokens = op.split(" ");
            if(tokens[0].equals("WITHDRAW")){
                int c = Integer.parseInt(tokens[1]);
                try {
                    return acc.withdraw(c);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }else if(tokens[0].equals("DEPOSIT")){
                int c = Integer.parseInt(tokens[1]);
                try {
                    return acc.deposit(c);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }else if(tokens[0].equals("BALANCE")){
                try {
                    return acc.getBalance();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
        }
        return null;
    }

    synchronized Map<Long, Integer> currentBalances() { // non remote method
        Map<Long, Integer> balances = new HashMap<>();
            for(HashMap.Entry m: accounts.entrySet()){
                    balances.put((long) m.getKey(), ((RAccount)m.getValue()).balance);
            }
        return balances;
    }
}
