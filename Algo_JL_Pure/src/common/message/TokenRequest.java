package common.message;

import peersim.core.Node;

/**
 * <p>Represente une requete de ressource.</p>
 * <p>Lorsqu'un noeud veut une ou plusieurs ressource, il cree une requete pour chaque ressource et lui envoie.</p>
 */
public class TokenRequest extends Request {

    // Variables.

    /**
     * <p>La note calcule à partir du vecteur de counter donne en parametre au constructeur.</p>
     */
    private final double mark;

    // Constructors.

    /**
     * @param mark
     * @param resourceID
     * @param requestID
     * @param sender
     * @param receiver
     */
    public TokenRequest(double mark, int resourceID, long requestID, Node sender, Node receiver) {
        super(resourceID, requestID, sender, receiver);

        this.mark = mark;
    }

    // Methods.

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (o == this)
            return true;
        if (o instanceof TokenRequest) {
            TokenRequest tR = (TokenRequest) o;

            return tR.mark == this.mark && tR.getResourceID() == this.getResourceID() && tR.getRequestID() == this.getRequestID() && tR.getSender().equals(this.getSender());
        } else
            return false;
    }

    // Getters and Setters.

    public double getMark() {
        return mark;
    }
}
