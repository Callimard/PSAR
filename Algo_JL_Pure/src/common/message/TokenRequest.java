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
    public TokenRequest(double mark, int resourceID, int requestID, Node sender, Node receiver) {
        super(resourceID, requestID, sender, receiver);

        this.mark = mark;
    }

    // Methods.

    // Getters and Setters.

    public double getMark() {
        return mark;
    }
}
