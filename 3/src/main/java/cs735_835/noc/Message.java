// $Id: Message.java 58 2017-03-07 20:17:15Z abcdef $

package cs735_835.noc;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class Message implements Comparable<Message> {

    private final int id;
    private final int sourceRow ;
    private final int sourceCol ;
    private final int destRow ;
    private final int destCol ;
    private final int scheduledTime ;
    private volatile int sendTime ;
    private volatile int recTime ;
    private volatile boolean isTracked = false;
    private volatile boolean isReceived = false;
    private volatile boolean isSent = false;

    public int compareTo(Message m) {
        if(this.id>m.id){
            return 1;
        }else if (this.id==m.id){
            return 0;
        }else{
            return -1;
        }
    }

    @Override
    public boolean equals(Object o) {
        return(this == o);
    }

    @Override
    public int hashCode() {
        return  this.id;
    }

    public Message(int id, int scheduledTime, int sourceRow, int sourceCol, int destRow, int destCol) {
        if(scheduledTime<0) {
            throw new IllegalArgumentException("scheduledTime must be positive");
        }
        this.id = id;
        this.scheduledTime = scheduledTime;
        this.sourceRow = sourceRow;
        this.sourceCol = sourceCol;
        this.destRow = destRow;
        this.destCol = destCol;
        this.sendTime = -1;
        this.recTime = -1;
        this.isSent = false;
        this.isReceived = false;
    }

    @Override
    public String toString() {
        if((isSent)&&(isReceived)){
            return ("msg "+id+" sent by ("+sourceRow+", "+sourceCol+") at "+sendTime+", delivered to ("+destRow+", "+destCol+") at "+getReceiveTime());
        }
        if(isSent){
            return ("msg "+id+" sent by ("+sourceRow+", "+sourceCol+") at "+sendTime+" (never delivered)");
        }
        return("msg "+id+" (never sent)");
    }

    public int sourceRow() {
        return sourceRow;
    }

    public int sourceCol() {
        return sourceCol;
    }

    public int destRow() {
        return destRow;
    }

    public int destCol() {
        return destCol;
    }

    public int getId() {
        return id;
    }

    public boolean isTracked() {
        return isTracked;
    }

    public void setTracked(boolean set) {
        isTracked = set;
    }

    public int getScheduledTime() {
        return scheduledTime;
    }

    public int getSendTime() {
        return sendTime;
    }

    public int getReceiveTime() {
        return recTime;
    }

    public void send(int time) {
        synchronized (this) {
            if (hasBeenSent()) {
                throw new IllegalStateException("Message " + id + " already sent");
            }
            if (time <= 0) {
                throw new IllegalArgumentException("Send time must be positive " + id);
            }
            sendTime = time;
            isSent = true;
//        if(isTracked()){
//            System.err.println(toString());
//        }
        }
    }

    public void receive(int time) {
        synchronized (this) {
            if (hasBeenReceived()) {
                throw new IllegalStateException("Message " + id + " already received");
            }
            if (!hasBeenSent()) {
                throw new IllegalStateException("Message " + id + " has not been sent yet");
            }
            if ((time <= 0) || (time < getSendTime())) {
                throw new IllegalArgumentException("Receive time must be positive " + id);
            }
            recTime = time;
            isReceived = true;
            if (isTracked()) {
                System.err.println(toString());
            }
        }
    }

    public boolean hasBeenReceived() {
        return isReceived;
    }

    public boolean hasBeenSent() {
        return isSent;
    }

    public static List<Message> readMessagesFromURL(URL url) throws java.io.IOException {
        ArrayList<Message> list = new ArrayList<>();
        boolean tracked = false;
        int id;
        int schedTime;
        int srcRow;
        int srcCol;
        int destRow;
        int destCol;
        try {
            Scanner s = new Scanner(url.openStream());
            while(s.hasNextLine()){
                try {
                    String line = s.nextLine();
                    if(line==null)break;
                    if(line.isEmpty())continue;
                    if(line.contains("*")){tracked = true;}
                    line = line.replaceAll("\\*","");
                    line = line.replaceAll("\\s+","");

                    String[] tokens = line.split("\\(");
                    id = Integer.parseInt(tokens[0].replaceAll("\\(",""));
                    String[] time = tokens[2].split("\\)");
                    schedTime = Integer.parseInt(time[time.length-1]);
                    String[] source = tokens[1].replaceAll("\\(","").
                            replaceAll("\\)","").split(",");
                    srcRow = Integer.parseInt(source[0]);
                    srcCol = Integer.parseInt(source[1]);
                    String[] dest = tokens[2].replaceAll("\\(","").
                            replaceAll("\\)"+time[time.length-1],"").split(",");
                    destRow = Integer.parseInt(dest[0]);
                    destCol = Integer.parseInt(dest[1]);
//                    String[] tokens = line.trim().replace("(","").split("\\s+");
//                    id = Integer.parseInt(tokens[0]);
//                    srcRow = Integer.parseInt((tokens[1].replace(",","")).replace("(",""));
//                    srcCol = Integer.parseInt(tokens[2].replace(")",""));
//                    destRow = Integer.parseInt((tokens[3].replace(",","")).replace("(",""));
//                    destCol = Integer.parseInt(tokens[4].replace(")",""));
//                    schedTime = Integer.parseInt(tokens[5]);
                    Message m = new Message(id, schedTime, srcRow, srcCol, destRow, destCol);
                    if(tracked){
                        m.setTracked(true);
                    }
                    list.add(m);
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        }
        catch(IOException ex) {
            // there was some connection problem, or the file did not exist on the server,
            // or your URL was not in the right format.
            // think about what to do now, and put it here.
            ex.printStackTrace(); // for now, simply output it.
        }
        return list;
    }

    /**
    * A utility method to generate messages with a uniform distribution.  The messages are printed on
    * {@code System.out} in non-decreasing order of their sending times.
    *
    * @param rate   the rate at which messages are generated (in messages per cycle)
    * @param count  the number of messages
    * @param seed   the seed used for random number generation
    * @param width  the width of the network; sources and destination will have a column between 0
    *               and {@code width - 1}
    * @param height the height of the network; sources and destination will have a row between 0
    *               and {@code height - 1}
    */
    public static void uniformRandomTraffic(long seed, int width, int height,
                                          double rate, int count) {
        Random rand = new Random(seed);
        double d = 2 / rate;
        double time = 1;
        for (int i = 1; i <= count; i++) {
          time += rand.nextDouble() * d;
          System.out.printf("%10d (%3d,%3d) (%3d,%3d) %10.0f%n",
              i,
              rand.nextInt(height), rand.nextInt(width), rand.nextInt(height), rand.nextInt(width),
              time);
        }
    }

    /**
    * Command-line application to generate random lists of messages. It is called as:
    * <pre>Message &lt;width&gt; &lt;height&gt; &lt;rate&gt; &lt;count&gt;</pre>
    * It calls method {@code uniformRandomTraffic} with a seed equal to 735835
    *
    * @see #uniformRandomTraffic
    */
    public static void main(String[] args) {
        int width = Integer.parseInt(args[0]);
        int height = Integer.parseInt(args[1]);
        double rate = Double.parseDouble(args[2]);
        int count = Integer.parseInt(args[3]);
        uniformRandomTraffic(735835, width, height, rate, count);
    }
}
