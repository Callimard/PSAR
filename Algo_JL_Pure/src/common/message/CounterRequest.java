package common.message;

import peersim.core.Node;

/**
 * <p>Represente une requete de compteur.</p>
 */
public class CounterRequest extends Message{

    // Variables.

    /**
     * <p>L'ID de la ressource a qui est destine la requete.</p>
     */
    private final int resourceID;

    /**
     * <p>L'id de la requete, permet ensuite de comparer pour savoir s'il elles sont obselete ou non.</p>
     */
    private final long requestID;

    // Constructors.

    /**
     *
     * @param resourceID
     * @param requestID
     * @param sender
     */
    public CounterRequest(int resourceID, long requestID, Node sender) {
        super(sender);

        this.resourceID = resourceID;
        this.requestID = requestID;
    }

    // Methods.

    // Getters and Setters.

    public int getResourceID() {
        return this.resourceID;
    }

    public long getRequestID() {
        return requestID;
    }

}
