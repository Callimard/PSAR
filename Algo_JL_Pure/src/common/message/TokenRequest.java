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
     *
     * @param vectorCounter
     * @param resourceID
     * @param requestID
     * @param sender
     * @param receiver
     */
    public TokenRequest(long[] vectorCounter, int resourceID, long requestID, Node sender, Node receiver) {
        super(resourceID, requestID, sender, receiver);

        long average = 0;
        for (int i = 0; i < vectorCounter.length; i++) {
            average += vectorCounter[i];
        }

        this.mark = ((double) average) / ((double) vectorCounter.length);
    }

    /**
     *
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

    // Getters and Setters.

    public double getMark() {
        return mark;
    }
}
