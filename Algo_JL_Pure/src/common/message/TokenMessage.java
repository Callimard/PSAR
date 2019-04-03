package common.message;

import common.util.Token;

/**
 * <p>Classe representant un message transportant un jeton.</p>
 * <p>Ne contient que le jeton et l'ID de la ressource associe.</p>
 */
public class TokenMessage {

    // Variables.

    /**
     * <p>L'ID de la ressource.</p>
     */
    private final int resourceID;

    /**
     * <p>Le jeton associe a la ressource.</p>
     */
    private final Token token;

    // Constructors.

    public TokenMessage(int resourceID, Token token) {
        this.resourceID = resourceID;
        this.token = token;
    }

    // Getters and Setters.

    public int getResourceID() {
        return resourceID;
    }

    public Token getToken() {
        return token;
    }
}
