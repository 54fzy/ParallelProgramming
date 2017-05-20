package cs735_835;

import cs735_835.remoteBank.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Random;

/**
 * Created by lydakis-local on 3/28/17.
 */
public class CompareClient {

    public static int setBalance(Record r){
        String[] tokens;tokens = r.toString().split(" ");
        return (int)(
                Double.parseDouble(
                        tokens[tokens.length-1].replaceAll("\\$", "")
                )*100);
    }

    public static void main(String[] args) throws RemoteException, NotBoundException, MalformedURLException {
        System.err.println("Running client in port: " + args[0]);
        System.err.println("Bank Name: " + BankServer.BANK_NAME);
        String service = "rmi://" + args[0] + "/" + BankServer.BANK_NAME;
        Bank bank =  (Bank) java.rmi.Naming.lookup(service);

        int opnum = Integer.parseInt(args[1]);
        int syncTime = Integer.parseInt(args[2]);
        String outputfile = args[3];
        System.err.println("Number of Operation: "+opnum);
        System.err.println("Sync every "+syncTime+" steps");
        Long start;

        Long opLatency;
        Long remoteLatency;
        Long bufLatency;

        Long bufDepTime = 0L;
        Long bufWithTime = 0L;
        Long bufBalTime = 0L;
        Long bufSyncTime = 0L;
        Long bufDepCount = 0L;
        Long bufWithCount = 0L;
        Long bufBalCount = 0L;
        Long bufSyncCount = 0L;

        Long opDepTime = 0L;
        Long opWithTime = 0L;
        Long opBalTime = 0L;
        Long opDepCount = 0L;
        Long opWithCount = 0L;
        Long opBalCount = 0L;

        Long remDepTime = 0L;
        Long remWithTime = 0L;
        Long remBalTime = 0L;
        Long remDepCount = 0L;
        Long remWithCount = 0L;
        Long remBalCount = 0L;

        int depAmount = 0;
        int withAmount = 0;
        int initialBalance;
        int curBalance;
        bank.openAccount();
        RemoteAccount rem = bank.getRemoteAccount(1);
        BufferedAccount buf = bank.getBufferedAccount(1);

        bank.requestOperation(1, Operation.withdraw(1000000));

        Random rand = new Random();
        start = System.nanoTime();
        initialBalance = setBalance(bank.requestOperation(1, Operation.getBalance()));
        for(int i = 0; i < opnum; i++){
            if(rand.nextDouble()>.5){
                int dep = (int)(rand.nextDouble()*1000);
                depAmount += dep;
//                System.err.println("Operation Depositing "+dep);
                Long t = System.nanoTime();
                bank.requestOperation(1, Operation.deposit(dep));
                opDepTime += (System.nanoTime() - t);
                opDepCount++;
            }else{
                Long t = System.nanoTime();
                int b = setBalance(bank.requestOperation(1, Operation.getBalance()));
                opBalTime += (System.nanoTime() - t);
                opBalCount ++;
                if (b>0) {
                    try{
                    int w = (int)(rand.nextDouble()*b);
                    withAmount += w;
                    System.err.println("Operation Withdraw " + w);
                    t = System.nanoTime();
                    bank.requestOperation(1, Operation.withdraw(w));
                    opWithTime += (System.nanoTime() - t);
                    opWithCount ++;
                } catch (Exception e){
//                    e.printStackTrace();
                }
                }
            }
        }
        opLatency = System.nanoTime() - start;
//
//        curBalance = setBalance(bank.requestOperation(3, Operation.getBalance()));
//        assert(curBalance == (initialBalance+depAmount-withAmount));
//
//        initialBalance = setBalance(rem.getBalance());
//        depAmount = 0;
//        withAmount = 0;
//
//        start = System.nanoTime();
//        for(int i = 0; i < opnum; i++){
//            if(rand.nextDouble()>.5){
//                int dep = (int)(rand.nextDouble()*1000);
//                depAmount += dep;
////                System.err.println("Remote Depositing "+dep);
//                Long t = System.nanoTime();
//                rem.deposit(dep);
//                remDepTime += (System.nanoTime() - t);
//                remDepCount ++;
//            }else{
//                Long t = System.nanoTime();
//                int b = setBalance(rem.getBalance());
//                remBalTime += (System.nanoTime() - t);
//                remBalCount ++;
//                if(b>0) {
//                    try{
//                    int w = (int)(rand.nextDouble()*b);
//                    withAmount += w;
////                    System.err.println("Remote Withdrawal " + w);
//                    t = System.nanoTime();
//                    rem.withdraw(w);
//                    remWithTime += (System.nanoTime() - t);
//                    remWithCount ++;
//                } catch (Exception e){
////                    e.printStackTrace();
//                }
//                }
//            }
//        }
//        remoteLatency = System.nanoTime() - start;
//
//        curBalance = setBalance(rem.getBalance());
//        assert(curBalance == (initialBalance+depAmount-withAmount));

        initialBalance = buf.balance();
        depAmount = 0;
        withAmount = 0;

        start = System.nanoTime();
        for(int i = 0; i < opnum; i++){
            if(rand.nextDouble()>.5){
                int dep = (int)(rand.nextDouble()*1000);
                depAmount += dep;
                System.err.println("Buffered Depositing "+dep);
                Long t = System.nanoTime();
                buf.deposit(dep);
                bufDepTime += (System.nanoTime() - t);
                bufDepCount ++;
            }else{
                Long t = System.nanoTime();
                int b = buf.balance();
                bufBalTime += (System.nanoTime() - t);
                bufBalCount ++;
                if(b>0) {
                    try {
                        int w = (int) (rand.nextDouble() * b);
                        withAmount += w;
                    System.err.println("Buffered Withdrawal " + w);
//                    t = System.nanoTime();
                        buf.withdraw(w);
                        bufWithTime += (System.nanoTime() - t);
                        bufWithCount++;
                    } catch (Exception e){
//                        e.printStackTrace();
                    }
                }
            }
            if((i%syncTime)==0){
//                System.err.println("Synching");
                Long t = System.nanoTime();
//                buf.sync();
                bufSyncTime += (System.nanoTime() - t);
                bufSyncCount ++;
            }
        }

        buf.sync();
        bufLatency = System.nanoTime() - start;

        curBalance = buf.balance();
        assert(curBalance == (initialBalance+depAmount-withAmount));
//
//        System.err.println("Operations Latency: "+opLatency/1e9);
//        System.err.println("Average Operations Latency: "+opLatency/opnum/1e9);
//        System.err.println("Remote Latency: "+remoteLatency/1e9);
//        System.err.println("Average Remote Latency: "+remoteLatency/opnum/1e9);
//        System.err.println("Buffered Latency: "+bufLatency/1e9);
//        System.err.println("Average Buffered Latency: "+bufLatency/opnum/1e9);

//        try{
//            BufferedWriter outputWriter = null;
//            outputWriter = new BufferedWriter(new FileWriter(outputfile));
//            outputWriter.write("Operations Syncrate " +
//                    "opLatency avgOpLatency " +
//                    "opDepCount opDeptime avgOpDepTime " +
//                    "opWithCount opWithTime avgOpWithTime " +
//                    "opBalCount opBalTime avgOpBalTime "+
//                    "RemLatency avgRemLatency " +
//                    "RemDepCount RemDeptime avgRemDepTime " +
//                    "RemWithCount RemWithTime avgRemWithTime " +
//                    "RemBalCount RemBalTime avgRemBalTime "+
//                    "BuffLatency avgBuffLatency " +
//                    "BuffDepCount BuffDeptime avgBuffDepTime " +
//                    "BuffWithCount BuffWithTime avgBuffWithTime " +
//                    "BuffBalCount BuffBalTime avgBuffBalTime "+
//                    "BuffSyncCount BuffSyncTime avgBuffSyncTime\n");
//            outputWriter.write(opnum+" "+syncTime+" "+
//                    opLatency/1e9+" "+opLatency/opnum/1e9+" "+
//                    opDepCount+" "+opDepTime/1e9+" "+opDepTime/opDepCount/1e9+" "+
//                    opWithCount+" "+opWithTime/1e9+" "+opWithTime/opWithCount/1e9+" "+
//                    opBalCount+" "+opBalTime/1e9+" "+opBalTime/opBalCount/1e9+" "+
//                    remoteLatency/1e9+" "+remoteLatency/opnum/1e9+" "+
//                    remDepCount+" "+remDepTime/1e9+" "+remDepTime/remDepCount/1e9+" "+
//                    remWithCount+" "+remWithTime/1e9+" "+remWithTime/remWithCount/1e9+" "+
//                    remBalCount+" "+remBalTime/1e9+" "+remBalTime/remBalCount/1e9+" "+
//                    bufLatency/1e9+" "+bufLatency/opnum/1e9+" "+
//                    bufDepCount+" "+bufDepTime/1e9+" "+bufDepTime/bufDepCount/1e9+" "+
//                    bufWithCount+" "+bufWithTime/1e9+" "+bufWithTime/bufWithCount/1e9+" "+
//                    bufBalCount+" "+bufBalTime/1e9+" "+bufBalTime/bufBalCount/1e9+" "+
//                    bufSyncCount+" "+bufSyncTime/1e9+" "+bufSyncTime/bufSyncCount/1e9+"\n");
//            outputWriter.close();
//        }catch (Exception e){
//            e.printStackTrace();
//        }finally {
//        }

    }

}
