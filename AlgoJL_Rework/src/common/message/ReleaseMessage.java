package common.message;

import peersim.core.Node;

/**
 * <p>Message envoye automatiquement au bout d'un certain temps par un noeud qui vient d'entrer en CS. Cela permet ensuite a ce noeud de relacher la CS et donc de simuler un temps passe en CS.</p>
 */
public class ReleaseMessage extends Message {

    // Constructors.

    /**
     * @param resourceID
     * @param sender
     * @param receiver
     */
    public ReleaseMessage(int resourceID, Node sender, Node receiver) {
        super(resourceID, sender, receiver);
    }
}
