package common.message;

import peersim.core.Node;

/**
 * <p>Classe representant un message transportant un compteur.</p>
 * <p>Ne contient que le compteur et l'ID de la ressource associe.</p>
 */
public class CounterMessage extends Message {

    // Variables.

    /**
     * <p>L'ID de la ressource.</p>
     */
    private final int resourceID;

    /**
     * <p>Le compteur.</p>
     */
    private final long counter;

    // Constructors.

    public CounterMessage(int resourceID, long counter, Node sender) {
        super(sender);

        this.resourceID = resourceID;
        this.counter = counter;
    }

    // Getters and Setters.

    public int getResourceID() {
        return this.resourceID;
    }

    public long getCounter() {
        return this.counter;
    }
}
