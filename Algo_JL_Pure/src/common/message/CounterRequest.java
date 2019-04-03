package common.message;

import peersim.core.Node;

/**
 * <p>Represente une requete de compteur.</p>
 */
public class CounterRequest extends Request{

    // Variables.

    // Constructors.

    /**
     *
     * @param resourceID
     * @param requestID
     * @param sender
     * @param receiver
     */
    public CounterRequest(int resourceID, long requestID, Node sender, Node receiver) {
        super(resourceID, requestID, sender, receiver);
    }

    // Methods.

    // Getters and Setters.


}
