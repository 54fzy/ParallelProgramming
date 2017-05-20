// $Id: SeqSimulator.java 58 2017-03-07 20:17:15Z abcdef $

package cs735_835.noc;

import java.net.URL;
import java.util.List;

/** A sequential network simulator for networks-on-chips.
 *  It is intended to be set up and run within the same thread.
 *
 * @author  Michel Charpentier
 */
public class SeqSimulator extends AbstractSimulator {

    /** Builds a new simulator. */
    public SeqSimulator (Network network) {
        super(network);
    }

    void runSimulation() {
        SimpleClock clock = new SimpleClock();
        network.setClock(clock);
        List<Core> cores = network.allCores();
        List<Router> routers = network.allRouters();
        List<Wire> wires = network.allWires();
        do {
            clock.step(); // steps to 1
            for (Router router : routers) {
                router.route();
            }
            for (Wire wire : wires) {
                wire.transfer();
            }
            for (Core core : cores) {
                core.process();
            }
            afterEachStep(clock.getTime());
        } while (network.isActive());
    }

    /** Command-line application. It is called as:
    * <pre>
    * SeqSimulator &lt;width&gt; &lt;height&gt; &lt;traffic file&gt;
    * </pre>
    */
    public static void main (String[] args) throws Exception {
        int width = Integer.parseInt(args[0]);
        int height = Integer.parseInt(args[1]);
        List<Message> messages = Message.readMessagesFromURL(new URL(args[2]));
        Network network = new Network(width, height);
        SeqSimulator sim = new SeqSimulator(network);
        messages.forEach(network::injectMessage);
        sim.timedSimulate();
    }
}
