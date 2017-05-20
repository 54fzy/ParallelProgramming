// $Id

package cs735_835.noc;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Network {

    private AtomicInteger numMessages  = new AtomicInteger(0);
    private AtomicInteger pmes  = new AtomicInteger(0);

    private HashMap sendTimes = new HashMap<Integer, Long>();
    private final Object sendTimesLock = new Object();
    private HashMap receiveTimes = new HashMap<Integer, Long>();
    private final Object recTimesLock = new Object();
//
    public HashMap<Integer, Long> getSendTimes(){
        return sendTimes;
    }

    public HashMap<Integer, Long> getRecTimes(){
        return receiveTimes;
    }

    private Message placeholder = new Message(-1, 666,666, 666 ,666 ,666);

    private class extendedWire implements Wire{
        private Message msg = placeholder;
        private Message receivedmsg = placeholder;
        private final Object lock = new Object();
        int col;
        int row;
        int destCol;
        int destRow;
        int id;
        int destid;
        private extendedWire dest;

        void setDest(extendedWire dest_){
            dest = dest_;
        }

        @Override
        public boolean transfer() {
            if (dest == null) {
                return true;
            }
            synchronized (lock) {
//                synchronized (dest.lock) {
                    if ((msg != placeholder) && (dest.msg == placeholder)) {
                        dest.msg = msg;
                        msg = placeholder;
                    }
                }
//            }
            return true;
        }
    }

    private class extendedRouter implements Router{

        private extendedCore core = null;
        private int row = -1;
        private int col = -1;
        private final extendedWire north = new extendedWire();
        private final extendedWire south = new extendedWire();
        private final extendedWire west = new extendedWire();
        private final extendedWire east = new extendedWire();

        ArrayList<extendedWire> getWires(){
            return new ArrayList<>(Arrays.asList(north, south, west, east));
        }

        private void set(int col_, int row_, extendedCore cr){
            synchronized (this) {
                col = col_;
                row = row_;
                core = cr;
            }
        }

        @Override
        public boolean isActive() {
            synchronized (north.lock) {
                synchronized (north.lock) {
                    synchronized (north.lock) {
                        synchronized (north.lock) {
            if (north.msg != placeholder) return true;
            if (north.receivedmsg != placeholder) return true;
//            System.err.println("1");
            if (south.msg != placeholder) return true;
            if (south.receivedmsg != placeholder) return true;
//            System.err.println("2");
            if (east.msg != placeholder) return true;
            if (east.receivedmsg != placeholder) return true;
//            System.err.println("3");
            if (west.msg != placeholder) return true;
            if (west.receivedmsg != placeholder) return true;
//            System.err.println("4");
            if (core.isActive()) return true;
//            System.err.println("5");
                        }
                    }
                }
            }
            return false;
        }
        @Override
        public void route() {
            synchronized (north.lock) {
                synchronized (north.lock) {
                    synchronized (north.lock) {
                        synchronized (north.lock) {

            if((north.msg.destCol()==col)&&(north.msg.destRow()==row)){
                north.msg.receive(clock.getTime());
                synchronized (core.recLock) {
                    core.receivedMessages.add(north.msg);
                }
                north.msg = placeholder;
            }
            if((west.msg.destCol()==col)&&(west.msg.destRow()==row)){
                west.msg.receive(clock.getTime());
                synchronized (core.recLock) {
                    core.receivedMessages.add(west.msg);
                }
                west.msg = placeholder;
            }if((north.msg!=placeholder)&&(south.msg==placeholder)){
                south.msg = north.msg;
                north.msg = placeholder;
            }if((west.msg != placeholder)&&
                    (east.msg == placeholder)&&
                    (west.msg.destCol() != col)){
//                System.err.println("Case 4, core["+row+","+col+"], message :"+west.msg.getId());
                east.msg = west.msg;
                west.msg = placeholder;

            }if((west.msg != placeholder)&&
                (south.msg == placeholder)&&
                (west.msg.destCol() == col)){
//                System.err.println("Case 5, core["+row+","+col+"], message :"+west.msg.getId());
                south.msg = west.msg;
                west.msg = placeholder;
            }
            Message m;
            synchronized (core.pendLock) {
                if (core.pendingMessages.peek() == null) {
                    return;
                } else {
                    m = core.pendingMessages.peek();
                    if(m.getScheduledTime()<=clock.getTime()){
                        m = core.pendingMessages.poll();
                    }else{
                        return;
                    }
//                    pmes.incrementAndGet();
                }
//            }
                if((m.destCol() == col)&&(m.destRow() == row)){
//                    System.err.println("Case 6a, core["+row+","+col+"], message :"+m.getId());
                    m.receive(clock.getTime());
//                    synchronized (recTimesLock) {
//                        receiveTimes.put(m.getId(), System.nanoTime());
//                    }
//                    synchronized (core.recLock) {
                        core.receivedMessages.add(m);
//                    }
                    return;
                }
//                synchronized (south.lock) {
                    if ((m.destCol() == col) && (south.msg == placeholder)) {
                        south.msg = m;
                        return;
                    }
//                }
//                synchronized (south.lock){
                if((m.destCol() != col)&&(east.msg==placeholder)) {
//                    System.err.println("Case 6, core["+row+","+col+"], message :"+m.getId());
                    east.msg = m;
                    return;
                }
//                synchronized (core.pendLock){
                    core.pendingMessages.add(m);
//                }
                }
//            }
        }
                    }
                }
            }
        }
    }

    private class extendedCore implements Core{
        private Comparator<Message> timeComp = (o1, o2) -> {
            if(o1.getScheduledTime()>o2.getScheduledTime()){
                return 1;
            }
            if(o1.getScheduledTime()==o2.getScheduledTime()){
                return o1.compareTo(o2);
            }
            return -1;
        };

        private PriorityQueue<Message> receivedMessages = new PriorityQueue<>();
        private PriorityQueue<Message> scheduledMessages = new PriorityQueue<>(timeComp);
        private PriorityQueue<Message> pendingMessages = new PriorityQueue<>(timeComp);
        private final Object pendLock = new Object();
        private final Object schedLock = new Object();
        private final Object recLock = new Object();

        private int row_ = 0;
        private int col_ = 0;

        private void set(int col, int row){
            col_ =col;
            row_ = row;
        }

        @Override
        public void scheduleMessage(Message message) {
//            System.err.println("Core "+row_+" "+col_);
//            System.err.println("Message id: "+message.getId()+" from ["
//                    +message.sourceCol()+" "+message.sourceRow()+"]"+" to ["
//                    +message.destCol()+" "+message.destRow()+"] at "+ message.getScheduledTime());
            if((message.sourceRow()!= row_)||(message.sourceCol()!=col_)){
                throw new IllegalArgumentException("Message source is not the same as core");
            }
            synchronized (schedLock) {
                scheduledMessages.add(message);
            }
        }

        @Override
        public boolean isActive() {
            synchronized (schedLock) {
                synchronized (pendLock) {
                    if (scheduledMessages.peek()!=null) {
                        return true;
                    }
                    if (pendingMessages.peek()!=null) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void process() {
//            System.err.println("Core "+row_+" "+col_+" at time "+clock.getTime());
            Message m;
//            System.err.println(scheduledMessages.peek().getId()+" "+
//                    scheduledMessages.peek().getScheduledTime()+" "+
//            clock.getTime());
//            synchronized (schedLock){
//                synchronized (pendLock){
                    while(scheduledMessages.peek()!=null) {
                        if (scheduledMessages.peek().getScheduledTime() > clock.getTime()) {
                            return;
                        }
                        m = scheduledMessages.poll();
                        m.send(clock.getTime());
                        if(m.isTracked())System.err.println(m.toString());
                        pendingMessages.add(m);
//                        synchronized (sendTimesLock) {
//                            sendTimes.put(m.getId(), System.nanoTime());
//                        }
//                        numMessages.incrementAndGet();
                    }
//                }
//            }
        }

        @Override
        public Collection<Message> receivedMessages() {
            synchronized (recTimesLock) {
                return receivedMessages;
            }
        }
    }

    private class extendedClock implements Clock {
        AtomicInteger time = new AtomicInteger(0);

        @Override
        public int getTime() {
            return time.get();
        }


        public void step(){
            time.incrementAndGet();
        }
    }

    final int width, height;

    private extendedCore[][] cores;
    private extendedRouter[][] routers;
    private extendedWire[][][] wires;
    private extendedWire[][][] activeWires;
    private final Object wireLock = new Object();
    private final Object routerLock = new Object();
    private final Object coreLock = new Object();
    private Clock clock;

    public Network(int w, int h) {
        width = w;
        height = h;
        synchronized (coreLock) {
            cores = new extendedCore[w][h];
        }

        synchronized (routerLock) {
            routers = new extendedRouter[w][h];
        }

        synchronized (wireLock) {
            wires = new extendedWire[w][h][4];
            activeWires = new extendedWire[w][h][2];
        }
//        clock = new extendedClock();

        for(int col_ = 0; col_ < w; col_++){
            for(int row_ = 0; row_ < h; row_++){
                extendedCore c = new extendedCore();
                c.set(col_, row_);
                extendedRouter r = new extendedRouter();
                r.set(col_, row_, c);
                ArrayList<extendedWire> routerWires = r.getWires();
                cores[col_][row_] = c;
                routers[col_][row_] = r;
                wires[col_][row_][0] = routerWires.get(0);
                wires[col_][row_][1] = routerWires.get(1);
                wires[col_][row_][2] = routerWires.get(2);
                wires[col_][row_][3] = routerWires.get(3);
                activeWires[col_][row_][0] = routerWires.get(1);
                activeWires[col_][row_][1] = routerWires.get(3);
            }
        }
        for(int col_ = 0; col_ < w; col_++){
            for(int row_ = 0; row_ < h; row_++){
                int row;
                int col;
                if(row_==h-1){
                    row = 0;
                }else{
                    row = row_+1;
                }
                if(col_==w-1){
                    col = 0;
                }else{
                    col = col_+1;
                }
                wires[col_][row_][0].setDest(null);
                wires[col_][row_][0].id = 0;
                wires[col_][row_][0].col = col_;
                wires[col_][row_][0].row = row_;
                wires[col_][row_][0].destRow = row_;
                wires[col_][row_][0].destCol = col_;
                wires[col_][row_][0].destid = 1;

                wires[col_][row_][1].setDest(routers[col_][row].north);
                wires[col_][row_][1].id = 1;
                wires[col_][row_][1].col = col_;
                wires[col_][row_][1].row = row_;
                wires[col_][row_][1].destRow = row;
                wires[col_][row_][1].destCol = col_;
                wires[col_][row_][1].destid = 0;

                wires[col_][row_][2].setDest(null);
                wires[col_][row_][2].id = 2;
                wires[col_][row_][2].col = col_;
                wires[col_][row_][2].row = row_;
                wires[col_][row_][2].destRow = row_;
                wires[col_][row_][2].destCol = col_;
                wires[col_][row_][2].destid = 3;

                wires[col_][row_][3].setDest(routers[col][row_].west);
                wires[col_][row_][3].id = 3;
                wires[col_][row_][3].col = col_;
                wires[col_][row_][3].row = row_;
                wires[col_][row_][3].destRow = row_;
                wires[col_][row_][3].destCol = col;
                wires[col_][row_][3].destid = 2;
            }
        }
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

    public boolean isActive(){
        for(extendedRouter ra[]: routers){
            for(extendedRouter r: ra){
                if(r.isActive()){
                    return true;
                }
            }
        }
        return false;
    }

    public Router getRouter(int row, int col) {
        return routers[col][row];
    }

    public Wire getVWire(int row, int col){
        return routers[col][row].south;
    }

    public Wire getHWire(int row, int col){
        return routers[col][row].east;
    }

    public Core getCore(int row, int col) {
        return cores[col][row];
    }

    public List<Router> allRouters() {
        ArrayList<Router> routersArray = new ArrayList();
        for (int j = 0; j<routers[0].length; j++) {
            for (int i = 0; i < routers.length; i++) {
                routersArray.add(routers[i][j]);
            }
        }
        return routersArray;
    }

    public List<Wire> allWires() {
        List<Wire> wiresArray = new ArrayList<>();
        for (int j = 0; j<routers[0].length; j++) {
            for (int i = 0; i < wires.length; i++) {
                wiresArray.addAll(Arrays.asList(activeWires[i][j]).subList(0, 2));
            }
        }
        return wiresArray;
    }

    public List<Core> allCores() {
        ArrayList<Core> coresArray = new ArrayList();
        for (int j = 0; j<cores[0].length; j++) {
            for (int i = 0; i < cores.length; i++) {
                coresArray.add(cores[i][j]);
            }
        }
        return coresArray;
    }

    public void injectMessage(Message msg) {
        if(((msg.destRow()>height-1))||(msg.destCol()<0)||(msg.destCol()>(width-1))||(msg.destCol()<0)){
            throw new IllegalArgumentException("Core position out of bounds");
        }
        cores[msg.sourceCol()][msg.sourceRow()].scheduleMessage(msg);
        numMessages.incrementAndGet();
    }

    protected void beforeRouter(int row, int col) {

    }
}