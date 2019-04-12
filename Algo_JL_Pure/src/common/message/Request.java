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
     * @param resourceID
     * @param requestID
     * @param sender
     * @param receiver
     */
    public Request(int resourceID, int requestID, Node sender, Node receiver) {
        super(resourceID, sender, receiver);

        this.requestID = requestID;
    }

    // Methods.

    @Override
    public String toString() {
        return "[R = " + this.getClass().getSimpleName() + " Req_ID = " + this.requestID + " " + super.toString() + "]";
    }

    // Getters and Setters.

    public int getRequestID() {
        return requestID;
    }
}
