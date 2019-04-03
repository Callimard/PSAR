package common;

/**
 * <p>Represente une requete de compteur.</p>
 */
public class CounterRequest {

    // Variables.

    /**
     * <p>L'ID de la ressource a qui est destine la requete.</p>
     */
    private final int resourceID;

    /**
     * <p>L'ID du noeud qui envoie la requete.</p>
     */
    private final long nodeID;

    /**
     * <p>L'id de la requete, permet ensuite de comparer pour savoir s'il elles sont obselete ou non.</p>
     */
    private final long requestID;

    // Constructors.

    public CounterRequest(int resourceID, long nodeID, long requestID) {
        this.resourceID = resourceID;
        this.nodeID = nodeID;
        this.requestID = requestID;
    }

    // Methods.

    // Getters and Setters.

    public int getResourceID() {
        return this.resourceID;
    }

    public long getNodeID() {
        return nodeID;
    }

    public long getRequestID() {
        return requestID;
    }

}
