package common.message;

import common.util.Token;
import peersim.core.Node;

/**
 * <p>Classe representant un message transportant un jeton.</p>
 * <p>Ne contient que le jeton et l'ID de la ressource associe.</p>
 */
public class TokenMessage extends Message {

    // Variables.

    /**
     * <p>Le jeton associe a la ressource.</p>
     */
    private final Token token;

    // Constructors.

    /**
     *
     * @param token
     * @param resourceID
     * @param sender
     * @param receiver
     */
    public TokenMessage(Token token, int resourceID, Node sender, Node receiver) {
        super(resourceID, sender, receiver);

        assert token != null : "Token null";
        assert token.getResourceID() == resourceID;

        this.token = token;
    }

    // Getters and Setters.

    public Token getToken() {
        return token;
    }
}
