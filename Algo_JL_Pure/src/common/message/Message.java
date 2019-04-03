package common.message;

import peersim.core.Node;

/**
 * <p>Class abstraite representant un message.</p>
 * <p>Elle contient uniquement l'ID du noeud envoyeur de message.</p>
 */
public abstract class Message {

    // Variables.

    /**
     * <p>ID du noeud envoyeur du message.</p>
     */
    private final Node sender;

    // Constructors.

    protected Message(Node sender) {
        this.sender = sender;
    }

    // Getters and Setters.

    public Node getSender() {
        return this.sender;
    }

}
