package common.message;

import peersim.core.Node;

/**
 * <p>Classe representant un message transportant un compteur.</p>
 * <p>Ne contient que le compteur et l'ID de la ressource associe.</p>
 */
public class CounterMessage extends Message {

    // Variables.

    /**
     * <p>Le compteur.</p>
     */
    private final long counter;

    // Constructors.

    /**
     * @param counter
     * @param resourceID
     * @param sender
     * @param receiver
     */
    public CounterMessage(long counter, int resourceID, Node sender, Node receiver) {
        super(resourceID, sender, receiver);

        this.counter = counter;
    }

    // Getters and Setters.

    public long getCounter() {
        return this.counter;
    }
}
