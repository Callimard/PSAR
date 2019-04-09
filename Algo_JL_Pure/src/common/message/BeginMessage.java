package common.message;

import peersim.core.Node;

public class BeginMessage extends Message {

    // Constructors.

    /**
     * @param resourceID
     * @param sender
     * @param receiver
     */
    public BeginMessage(int resourceID, Node sender, Node receiver) {
        super(resourceID, sender, receiver);
    }
}
