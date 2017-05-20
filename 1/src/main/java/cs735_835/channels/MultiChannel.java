package cs735_835.channels;

import java.util.LinkedList;

public class MultiChannel<T> implements Channel<T> {

    private long count;
    private int listCount;
    private final LinkedList<T>[] inputQueues;
    private final int queueCount;
    private LinkedList<T> outputQueue;
    private final Object[] sharedLocks;

    public MultiChannel(int queueCount) {
//        System.err.println("Multichannel with queueCount: "+queueCount);
        listCount = queueCount ;
        count = 0;
        this.queueCount = queueCount;
        inputQueues = new LinkedList[queueCount];
        sharedLocks = new Object[queueCount];
        for(int i = 0; i <queueCount;i++){
            inputQueues[i]=new LinkedList<>();
            sharedLocks[i]=new Object();
        }
        outputQueue = new LinkedList();
    }

    public int queueCount() {
            return this.queueCount;
    }

    public T get() {
//        System.err.println("MultiChannel get");
        synchronized (this) {
            if (outputQueue.size() > 0) {
//                System.err.println("MultiChannel outputQueue.size() = "+outputQueue.size());
                return outputQueue.poll();
            }

//                System.err.println("MultiChannel inputQueues.length = "+inputQueues.length);
        for(int i = 0; i<listCount;i++){
            synchronized (sharedLocks[i]){
                while(inputQueues[i].peek()!=null){
                    outputQueue.add(inputQueues[i].poll());
                    count++;
                }
            }
        }
        return outputQueue.poll();
        }
    }

    public void put(T value, int q) {
        if((q<0)||(q>queueCount()-1))throw new IllegalArgumentException("Value parameter is null");
        if(value==null)throw new IllegalArgumentException("Value parameter is null");
        synchronized (sharedLocks[q]){
            inputQueues[q].add(value);
        }
    }

    public long totalCount() {
        synchronized (this){
            return count;
        }
//        throw new UnsupportedOperationException("to be implemented");

    }
}
