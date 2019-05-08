package common.message;

import peersim.core.Node;

import java.util.Set;
import java.util.TreeSet;

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
    private Node sender;

    /**
     * <p>ID du noeud receveur.</p>
     */
    private Node receiver;

    private final Set<Long> visitedNode = new TreeSet<>();

    // Constructors.

    /**
     * @param resourceID
     * @param sender
     * @param receiver
     */
    protected Message(int resourceID, Node sender, Node receiver) {

        assert sender != null : "Sender null";
        assert receiver != null : "Receiver null";

        this.resourceID = resourceID;
        this.sender = sender;
        this.receiver = receiver;
    }

    // Methods

    @Override
    public String toString() {
        return "[M = " + this.getClass().getSimpleName()
                + " Sender = " + this.sender.getID()
                + " Receiver = " + this.receiver.getID()
                + " R = " + this.resourceID + "]";
    }

    public boolean addAllVisitedNode(Set<Long> visitedNode) {
        return this.visitedNode.addAll(visitedNode);
    }

    public boolean addVisitedNode(Node visitedNode) {
        return this.visitedNode.add(visitedNode.getID());
    }

    /**
     * @param visitedNode
     * @return true si le noeud en parametre est un noeud deja visite par le message.
     */
    public boolean isVisitedNode(Node visitedNode) {
        return this.visitedNode.contains(visitedNode.getID());
    }

    public Set<Long> getVisitedNode() {
        return this.visitedNode;
    }

    // Getters and Setters.

    public int getResourceID() {
        return this.resourceID;
    }

    public Node getSender() {
        return this.sender;
    }

    public void setSender(Node sender) {
        this.sender = sender;
    }

    public Node getReceiver() {
        return this.receiver;
    }

    public void setReceiver(Node receiver) {
        this.receiver = receiver;
    }

}
