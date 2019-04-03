package common;

/**
 * <p>Represente une requete de ressource.</p>
 * <p>Lorsqu'un noeud veut une ou plusieurs ressource, il cree une requete pour chaque ressource et lui envoie.</p>
 */
public class ResourceRequest {

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

    /**
     * <p>La note calcule à partir du vecteur de counter donne en parametre au constructeur.</p>
     */
    private final double mark;

    // Constructors.

    public ResourceRequest(int resourceID, long nodeID, long requestID, long vectorCounter[]) {
        this.resourceID = resourceID;
        this.nodeID = nodeID;
        this.requestID = requestID;

        long average = 0;
        for (int i = 0; i < vectorCounter.length; i++) {
            average += vectorCounter[i];
        }

        this.mark = ((double) average) / ((double) vectorCounter.length);
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

    public double getMark() {
        return mark;
    }
}
