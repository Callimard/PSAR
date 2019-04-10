package common.message;

import peersim.core.Node;

/**
 * <p>Class abstraite representant un message.</p>
 * <p>Elle contient l'ID du noeud envoyeur du message, l'ID du receveur et l'ID de la ressource concernee.</p>
 */
public abstract class Message {

    // Variables.

    /**
     * <p>L'ID de la ressource concernee.</p>
     */
    private final int resourceID;

    /**
     * <p>ID du noeud envoyeur du message.</p>
     */
    private final Node sender;

    /**
     * <p>ID du noeud receveur.</p>
     */
    private final Node receiver;

    // Constructors.

    /**
     *
     * @param resourceID
     * @param sender
     * @param receiver
     */
    protected Message(int resourceID, Node sender, Node receiver) {
        this.resourceID = resourceID;
        this.sender = sender;
        this.receiver = receiver;
    }

    // Methods

    @Override
    public String toString() {
        return "[M = " + this.getClass().getSimpleName() + " Sender = " + this.sender.getID() + " Receiver = " + this.receiver.getID() + " R = " + this.resourceID + "]";
    }

    // Getters and Setters.

    public int getResourceID() {
        return this.resourceID;
    }

    public Node getSender() {
        return this.sender;
    }

    public Node getReceiver() {
        return this.receiver;
    }

}
