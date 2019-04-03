package common.message;

import peersim.core.Node;

/**
 * <p>Represente une requete de ressource.</p>
 * <p>Lorsqu'un noeud veut une ou plusieurs ressource, il cree une requete pour chaque ressource et lui envoie.</p>
 */
public class TokenRequest extends Message {

    // Variables.

    /**
     * <p>L'ID de la ressource a qui est destine la requete.</p>
     */
    private final int resourceID;

    /**
     * <p>L'id de la requete, permet ensuite de comparer pour savoir s'il elles sont obselete ou non.</p>
     */
    private final long requestID;

    /**
     * <p>La note calcule à partir du vecteur de counter donne en parametre au constructeur.</p>
     */
    private final double mark;

    // Constructors.

    /**
     *
     * @param resourceID
     * @param requestID
     * @param vectorCounter
     * @param sender
     */
    public TokenRequest(int resourceID, long requestID, long[] vectorCounter, Node sender) {
        super(sender);

        this.resourceID = resourceID;
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

    public long getRequestID() {
        return requestID;
    }

    public double getMark() {
        return mark;
    }
}
