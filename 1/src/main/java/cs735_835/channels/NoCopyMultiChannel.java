package cs735_835.channels;

import java.util.LinkedList;

public class NoCopyMultiChannel<T> implements Channel<T> {
    private long count;
    private int listCount;
    private final LinkedList<T>[] inputQueues;
    private LinkedList<T> outputQueue;
    private final Object sharedLocks[];
    private final Object tokenLock;
    private volatile int listToken;

    public NoCopyMultiChannel(int queueCount) {
//        System.err.println("NoCopyMultiChannel with queueCount: "+queueCount);
        listCount = queueCount ;
        count = 0;
        listToken = 0;
        inputQueues = new LinkedList[queueCount];
        sharedLocks = new Object[queueCount];
        for(int i = 0; i <queueCount;i++){
            inputQueues[i]=new LinkedList<>();
            sharedLocks[i]=new Object();
        }
        tokenLock = new Object();
        outputQueue = new LinkedList();
    }

    public int queueCount() {
            return listCount;
    }

    public T get() {
        T ret = null;
        synchronized (this) {
            if (outputQueue.size() > 0) {
                count++;
                return outputQueue.poll();
            }
            synchronized (sharedLocks[listToken]) {
                outputQueue = inputQueues[listToken];
                inputQueues[listToken] = new LinkedList<T>();
                listToken = listToken<(listCount-1)?(listToken+1):0;
            }
            ret = outputQueue.poll();
            if (ret != null) count++;
            return ret;
        }
    }

    public void put(T value, int q) {
        if((q<0)||(q>this.listCount-1))throw new IllegalArgumentException("Value parameter is null");
        if(value==null)throw new IllegalArgumentException("Value parameter is null");
        synchronized (sharedLocks[q]) {
            inputQueues[q].add(value);
        }
    }

    public long totalCount() {
        synchronized (this) {
            return count;
        }
    }
}
