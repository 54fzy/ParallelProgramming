// $Id: NaiveExecSimulator.java 58 2017-03-07 20:17:15Z abcdef $

package cs735_835.noc;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class NaiveExecSimulator extends AbstractSimulator {

    private ExecutorService exs;

    public NaiveExecSimulator(Network network, ExecutorService exec) {
        super(network);
        this.exs = exec;
    }

    /**
    * Command-line application. It is called as:
    * <pre>
    * NaiveExecSimulator &lt;width&gt; &lt;height&gt; &lt;traffic file&gt; &lt;#threads&gt;
    * </pre>
    */
    public static void main(String[] args) throws Exception {
        int width = Integer.parseInt(args[0]);
        int height = Integer.parseInt(args[1]);
        int nbWorkers = Integer.parseInt(args[3]);
        List<Message> messages = Message.readMessagesFromURL(new URL(args[2]));
        Network network = new Network(width, height);
        ExecutorService exec = Executors.newFixedThreadPool(nbWorkers);
        NaiveExecSimulator sim = new NaiveExecSimulator(network, exec);
        messages.forEach(network::injectMessage);
        sim.timedSimulate();
        exec.shutdown();
    }

    void runSimulation() {
        SimpleClock clock = new SimpleClock();
        network.setClock(clock);
        List<Core> cores = network.allCores();
        List<Router> routers = network.allRouters();
        List<Wire> wires = network.allWires();

        ArrayList<Callable<Object>> todoFirst = new ArrayList<>();
        ArrayList<Callable<Object>> todoSecond = new ArrayList<>();

        for(Router r : routers){
            todoFirst.add(Executors.callable(r::route));
        }

        for(Core c: cores){
            todoSecond.add(Executors.callable(c::process));
        }

        for(Wire w : wires){
            todoSecond.add(Executors.callable((Runnable) w::transfer));
        }
        do {
            clock.step(); // steps to 1
            try {
                exs.invokeAll(todoFirst);
                exs.invokeAll(todoSecond);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            afterEachStep(clock.getTime());
        } while (network.isActive());
    }
}
