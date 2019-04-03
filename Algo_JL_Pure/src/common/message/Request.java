package common.message;

import peersim.core.Node;

/**
 * <p>En plus d'etre un message, contient l'ID de la requete</p>
 */
public class Request extends Message {

    // Variables.

    private final int requestID;

    // Constructors.

    /**
     *
     * @param resourceID
     * @param requestID
     * @param sender
     * @param receiver
     */
    public Request(int resourceID, long requestID, Node sender, Node receiver) {
        super(resourceID, sender, receiver);

        this.requestID = resourceID;
    }

    // Getters and Setters.

    public int getRequestID() {
        return requestID;
    }
}
