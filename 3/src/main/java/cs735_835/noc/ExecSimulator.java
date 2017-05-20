// $Id: ExecSimulator.java 58 2017-03-07 20:17:15Z abcdef $

package cs735_835.noc;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecSimulator extends AbstractSimulator {
    private final ExecutorService exs;
    private final int n;
    public ExecSimulator(Network network, ExecutorService exec, int nbTasks) {
        super(network);
        this.exs = exec;
        this.n = nbTasks;
    }

    /**
    * Command-line application. It is called as:
    * <pre>
    * ExecSimulator &lt;width&gt; &lt;height&gt; &lt;traffic file&gt; &lt;#threads&gt;
    * &lt;granularity&gt;
    * </pre>
    */
    public static void main(String[] args) throws Exception {
        int width = Integer.parseInt(args[0]);
        int height = Integer.parseInt(args[1]);
        int nbWorkers = Integer.parseInt(args[3]);
        int nbTasks = Integer.parseInt(args[4]);
        List<Message> messages = Message.readMessagesFromURL(new URL(args[2]));
        Network network = new Network(width, height);
        ExecutorService exec = Executors.newFixedThreadPool(nbWorkers);
        ExecSimulator sim = new ExecSimulator(network, exec, nbTasks);
        messages.forEach(network::injectMessage);
        sim.timedSimulate();
        exec.shutdown();
    }

    void runSimulation(){
        SimpleClock clock = new SimpleClock();
        network.setClock(clock);
        List<Core> cores = network.allCores();
        List<Router> routers = network.allRouters();
        List<Wire> wires = network.allWires();
        ArrayList<Callable<Object>> todoFirst = new ArrayList<>();
        ArrayList<Callable<Object>> todoSecond = new ArrayList<>();
        int N = network.height * network.width;
        ArrayList<Runnable> routerRunnables = new ArrayList<>();
        ArrayList<Runnable> wireRunnables = new ArrayList<>();
        ArrayList<Runnable> coreRunnables = new ArrayList<>();
        ArrayList[] rs = new ArrayList[n];
        ArrayList[] cs = new ArrayList[n];
        ArrayList[] ws = new ArrayList[2*n];
        int idx;
        int k = 0;
        for(int i = 0 ; i< n ; i++){
            cs[i] = new ArrayList();
            rs[i] = new ArrayList();
        }

        for(int i = 0 ; i< 2*n ; i++){
            ws[i] = new ArrayList();
        }

        for(idx = 0 ; idx<N; idx++) {
            rs[k].add(routers.get(idx));
            k = (k<n-1)?k+1:0;
        }
        for(ArrayList<Router> arrayList: rs){
            Runnable runnableRoute = () -> {
                for(Router r:arrayList){
                    r.route();
                }
            };
            routerRunnables.add(runnableRoute);
        }
        for(Runnable r:routerRunnables){
            todoFirst.add(Executors.callable(r));
        }
        k = 0;
        for(idx = 0; idx < wires.size(); idx++) {
            ws[k].add(wires.get(idx));
            k = (k<(2*n-1))?k+1:0;
        }
        for(ArrayList<Wire> arrayList: ws){
            Runnable runnableTransfer = () -> {
                for(Wire w :arrayList){
                    w.transfer();
                }
            };
            wireRunnables.add(runnableTransfer);
        }

        for(Runnable r:wireRunnables){
            todoSecond.add(Executors.callable(r));
        }
        k = 0;
        for(idx = 0 ; idx<N; idx++) {
            cs[k].add(cores.get(idx));
            k = (k<n-1)?k+1:0;
        }
        for(ArrayList<Core> arrayList: cs){
//            System.err.println("c"+arrayList.size());
            Runnable runnableProcess = () -> {
                for(Core c :arrayList){
                    c.process();
                }
            };
            coreRunnables.add(runnableProcess);
        }
        for(Runnable r:coreRunnables){
            todoSecond.add(Executors.callable(r));
        }
//        System.err.println("Second Stage tasks :"+todoSecond.size());
        do {
            clock.step();
            try {
                exs.invokeAll(todoFirst);
                exs.invokeAll(todoSecond);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            afterEachStep(clock.getTime());
        }while(network.isActive());
    }
}