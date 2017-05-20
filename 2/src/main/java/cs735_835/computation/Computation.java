package cs735_835.computation;

import javax.print.attribute.standard.Finishings;
import java.security.cert.TrustAnchor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.*;
import java.util.function.Function;

public class Computation<A> {

    private final FutureTask compTask;
    private Runnable superRunnable;
//    private final Object listLock = new Object();
    private final LinkedList<Runnable> cbList = new LinkedList<>();
    private final LinkedList<Computation> contList = new LinkedList<>();
    private final LinkedList<Function> funcList = new LinkedList<>();
    private final LinkedList<Computation> parallelContList = new LinkedList<>();
    private volatile boolean isFinished = false;
    private volatile boolean cbFinished = false;
    private volatile boolean contFinished = false;
    private volatile boolean pcontFinished = false;
    private volatile A ret;
    private volatile boolean cancelled = false;
    private volatile boolean interrupted = false;
    private final Object listLock = new Object();

    private <A> Computation(Callable<A> task){
        this.compTask = new FutureTask<>(task);
        this.superRunnable = createRunnable();
    }

    private <A> Computation(Callable<A> task, Runnable[] callbacks) {
        for(Runnable r : callbacks){
            cbList.add(r);
        }
        this.compTask = new FutureTask<>(task);
        this.superRunnable = createRunnable();
    }

    private Runnable createRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                System.err.println("Running Computation: " + compTask.toString());
                compTask.run();
                System.err.println("Ran Computation: " + compTask.toString());
                try {
                    ret = (A) compTask.get();
                } catch (ExecutionException  | InterruptedException ee){
//                cancelled = true;
                    isFinished= true;
                    interrupted = true;
                } catch (CancellationException ce){
                    isFinished = true;
                    cancelled = true;
                }

                while (!cbFinished && !cancelled) {
                    Runnable run;
                    synchronized (cbList) {
                        run = cbList.poll();
                        if (run == null) {
//                            System.err.println("CB poll returns null");
                            cbFinished = true;
                            isFinished = true;
                            break;
                        }
                    }
                    synchronized (run) {
                        try {
                            System.err.println("Callback: " + run.toString());
                            run.run();
                            System.err.println("Callback Completed: " + run.toString());
                        } catch (Exception e) {
//                            e.printStackTrace();
                        }
                    }
                }
                cbFinished = true;
                isFinished = true;
//                System.err.println("Starting Continuations, Finished: " + isFinished + ", Ret: " + ret);

                while ((!interrupted)&&(!pcontFinished)) {
                    Computation c;
                    Computation pc;
                    synchronized (contList) {
                        synchronized (parallelContList){
                            pc = parallelContList.poll();
                            c = contList.poll();
                            if((c==null)&&(pc==null)){
                                contFinished = true;
                                pcontFinished = true;
                                break;
                            }
                        }
                    }

                    if (pc != null) {
                        synchronized (pc) {
                            System.err.println("Parallel Continuation: " + pc.toString());
                            Thread t = new Thread(pc.superRunnable);
                            t.start();
                            System.err.println("Parallel Continuation complete: " + pc.toString());
                        }
                    }

                    if (c != null) {
                        synchronized (c) {
                            contFinished = false;
                            System.err.println("Continuation: " + c.toString());
                            c.superRunnable.run();
                            try {
                                c.get();
                            } catch (InterruptedException | CancellationException | ExecutionException e) {
                                c.cancel();
//                                e.printStackTrace();
                            }
                            System.err.println("Continuation complete: " + c.toString());
                        }
                    }
                }
                System.err.println(this + " done.");
            }
        };
    }

    public static <A> Computation<A> newComputation(Callable<A> task, Runnable... callbacks) {
        Computation comp = new Computation(task, callbacks);
        Thread thread = new Thread(comp.superRunnable);
        thread.start();
        return comp;
    }

    private void clear() {
        interrupted = true;
        synchronized (cbList){
            cbList.clear();}
        synchronized (contList){
            contList.clear();}
        synchronized (parallelContList){
            parallelContList.clear();
        }
    }

    private void cancel(){
        System.err.println("cancelling "+this.toString());
        cancelled = true;
        interrupted = true;
        contFinished = true;
        isFinished = true;
        cbFinished = true;
        compTask.cancel(true);
//        System.err.println(compTask.isDone());
        synchronized (contList){
            for(Computation c : contList){
                c.cancel();
//                c.clear();
            }
//            contList.clear();
        }
        synchronized (parallelContList){
            for(Computation c : parallelContList){
                c.cancel();
//                c.clear();
            }
//            parallelContList.clear();
        }

    }

    public boolean isFinished() {
        return this.isFinished;
    }

    public A get() throws InterruptedException, ExecutionException, CancellationException {
        System.err.println("In get "+this.compTask.toString()+" ret:"+ret+" cancelled: "+cancelled );
        try {
            ret = (A) compTask.get();
        } catch (ExecutionException  | InterruptedException ee){
//                cancelled = true;
                isFinished= true;
                while(!cbFinished){}
                interrupted = true;
                cancel();
                throw ee;
        } catch (CancellationException ce){
            cancelled = true;
            isFinished = true;
            cancel();
            throw new CancellationException();
        }

        while(!cbFinished) {
            if (cancelled) {
                cancel();
                throw new CancellationException();
            }
        }
        System.err.println("Out of get "+this.compTask.toString()+" ret:"+ret+" cancelled: "+cancelled );
        return ret;
    }

    public void onComplete(Runnable callback){
        synchronized (cbList) {
            if(isFinished()){
                System.err.println("onComplete #1 "+callback.toString());
                try {
                    callback.run();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }else{
                System.err.println("onComplete #2 "+callback.toString());
                cbList.add(callback);
            }
        }
    }

    public <B> Computation<B> map(Function<? super A, B> f) {
        Callable<B> run = () -> f.apply(ret);
        Computation comp;
        synchronized (contList) {
            if((!this.pcontFinished)||(!this.contFinished)){
                comp = new Computation(run);
//                funcList.add(f);
                contList.add(comp);
                System.err.println("Continuation before execution: " + f.toString());
                return comp;
            }
        }
        run = () -> f.apply(ret);
        System.err.println("Continuation after execution: "+f.toString());
        comp = new Computation(run);
//        System.err.println("Continuation after execution 2: "+f.toString());
        comp.superRunnable.run();
        return comp;
    }

    public <B> Computation<B> mapParallel(Function<? super A, B> f) {
        Callable<B> run = () -> f.apply(ret);
        Computation comp;
        synchronized (parallelContList) {
            if(!this.pcontFinished){
                comp = new Computation(run);
                parallelContList.add(comp);
                System.err.println("Parallel Continuation before execution: " + f.toString());
                return comp;
            }
        }
        run = () -> f.apply(ret);
        System.err.println("Parallel Continuation after execution: "+f.toString());
        comp = newComputation(run);
        return comp;
    }
}