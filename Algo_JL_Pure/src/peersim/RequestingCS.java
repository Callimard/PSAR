package peersim;

import common.message.CounterMessage;
import common.message.Message;
import common.message.TokenMessage;
import common.message.TokenRequest;
import common.util.Token;
import peersim.core.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class RequestingCS {

    // Variables.

    /**
     * <p>Reference directe sur l'algo pour le mettre a jour.</p>
     */
    private final AlgoJL parent;

    /**
     * <p>La note de chaque requete de jeton envoye pour cette section critque. Utile pour comparer avec d'autres requete que l'on recevra plus tard -> {@link AlgoJL#sendMessage(Message)}.</p>
     * <p>Pour eviter tout probleme si jamais on a pas encore calculer notre mark, initialisee a {@link Double#MAX_VALUE}</p>
     */
    private double myRequestMark = Double.MAX_VALUE;

    /**
     * <p>Le set de ressources qui représente toutes les ressources que l'on veut pour entrer en CS.</p>
     */
    private final Set<Integer> resourceSet;

    /**
     * <p>Le set de compteur que l'on a recu, Initialement vide.</p>
     * <p>Le set est directement mis a jour dans le constructeur. On regarde si certains compteurs des ressourced demandees ne sont pas deja present sur le noeud.</p>
     */
    private Set<Integer> counterReceived;

    /**
     * <p>Le set de ressource que l'on possede. Initialement vide.</p>
     * <p>Le set est directement mis a jour dans le constructeur. On regarde si certains jeton des ressource demandees ne sont pas deja present sur le noeud.</p>
     */
    private Set<Integer> tokenReceived;

    private List<TokenRequest> listTokenRequestSend;


    // Constructors.

    /**
     * <p>Initialise par default tous les set de receptions a vide.</p>
     * <p>Cependant, regarde si certains jeton de ressources demandees ne sont pas deja present sur le noeud. Si c'est le cas,
     * met a jours les set de reception et aussi le vecteur de compteur. (Cela signifie que la fonction {@link AlgoJL#requestCS(Set)} verifie seulement si oui ou non le jeton est present,
     * si le jeton est deja present, aucun traitement est a faire, le constructeur de {@link RequestingCS} le fait automatiquement.</p>
     *
     * @param resourceSet le set de ressource requises pour entrer en CS.
     */
    RequestingCS(Set<Integer> resourceSet, AlgoJL parent) {
        this.parent = parent;

        this.resourceSet = resourceSet;

        this.counterReceived = new TreeSet<>();
        for (int resourceID = 0; resourceID < this.parent.getArrayToken().length; resourceID++) {
            Token token = this.parent.getToken(resourceID);
            if (token.isHere() && this.resourceSet.contains(resourceID)) {
                this.counterReceived.add(resourceID);
                this.parent.setCounter(resourceID, token.incrementCounter());
            }
        }

        this.tokenReceived = new TreeSet<>();
        for (int resourceID = 0; resourceID < this.parent.getArrayToken().length; resourceID++) {
            Token token = this.parent.getToken(resourceID);
            if (token.isHere() && this.resourceSet.contains(resourceID)) {
                this.tokenReceived.add(resourceID);
            }
        }

        this.listTokenRequestSend = new ArrayList<>();
    }

    // Methods.

    /**
     * @return true si tous les compteurs de chaque ressources ont ete recu, sinon false.
     */
    boolean allCounterAreReceived() {
        return this.counterReceived.containsAll(this.resourceSet);
    }

    /**
     * @return true si tous les jetons de chaque ressources ont ete recu, sinon false.
     */
    boolean allTokenAreReceived() {
        return this.tokenReceived.containsAll(this.resourceSet);
    }

    /**
     * <p>Met a jour le vecteur de compteur et les liens de l'arbre dynamique.</p>
     * <p>Ne fait aucun traitement qui aurait un rapport avec le fait que tout les compteurs ont ete recus. Apres avoir appele cette methode,
     * il faut appeler la mehtode {@link RequestingCS#allCounterAreReceived()} pour savoir si tous les compteurs sont recu et ensuite effectuer le traitement approprie.</p>
     *
     * @param counterMessage le message de compteur que l'on vient de recevoir
     */
    void receiveCounter(CounterMessage counterMessage) {
        int resourceID = counterMessage.getResourceID();
        long counter = counterMessage.getCounter();
        Node sender = counterMessage.getSender();

        System.out.println("-------------------------------------------------------------------------");

        System.out.println("Node = " + this.parent.getNode().getID() + " Reception counter pour = " + resourceID);
        System.out.println("SENDER = " + sender.getID());

        boolean res = this.counterReceived.add(resourceID);
        if (res) {

            System.out.println("Node = " + this.parent.getNode().getID() + " AJOUT POUR = " + resourceID);

            this.parent.setCounter(resourceID, counter);
            this.parent.setNodeLink(resourceID, sender);

            System.out.println("-------------------------------------------------------------------------");
        } else {
            try {
                throw new Exception();
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("ATTENTION!!! Reception d'un COUNTER que l'on a deja recu.");

            System.out.println("Node = " + this.parent.getNode().getID() + " DEJA RECU POUR = " + resourceID);

            System.out.println("-------------------------------------------------------------------------");
        }
    }

    /**
     * <p>Met a jour le set de ressources recues et les liens de l'arbre dynamique.</p>
     * <p>Ne fait aucun traitement qui aurait un rapport avec le fait que toutes les ressources ont ete recues. Apres avoir appele cette methode,
     * il faut appeler la mehtode {@link RequestingCS#allTokenAreReceived()} pour savoir si tous les jetons sont recu et ensuite effectuer le traitement approprie.</p>
     *
     * @param tokenMessage le message de jeton que l'on vient de recevoir
     */
    void receiveToken(TokenMessage tokenMessage) {
        int resourceID = tokenMessage.getResourceID();
        Token token = tokenMessage.getToken();

        boolean res = this.tokenReceived.add(tokenMessage.getResourceID());
        if (res) {
            this.parent.updateToken(resourceID, token);
            this.parent.setNodeLink(resourceID, null);
            this.parent.getArrayToken()[resourceID].setHere(true);

            if (this.isCounterNeeded(resourceID)) {
                this.counterReceived.add(resourceID);
                this.parent.setCounter(resourceID, this.parent.getToken(resourceID).incrementCounter());
            }
        } else {
            System.err.println("ATTENTION!!! Reception d'un TOKEN que l'on a deja recu.");
        }
    }

    /**
     * <p>Est appele lorsqu'on envoit un jeton que nous possedons et qui est requit pour entre en CS.</p>
     * <p>S'occupe de mettre a jour les lien de l'arbre dynamique et le contenu du token sur le site.</p>
     * <p>Le set de ressource recue est aussi mis a jour.</p>
     *
     * @param resourceID    l'ID de la ressource concernee
     * @param tokenReceiver le noeud qui va recevoir le jeton.
     * @return le jeton clone que l'on peut envoye.
     */
    Token sendToken(int resourceID, Node tokenReceiver) {
        Token tokenSend = (Token) this.parent.getArrayToken()[resourceID].clone();
        this.parent.getArrayToken()[resourceID].setHere(false);
        this.parent.getArrayToken()[resourceID].clearAllQueue();
        this.parent.setNodeLink(resourceID, tokenReceiver);

        this.tokenReceived.remove(resourceID);

        return tokenSend;
    }

    /**
     * @param resourceID l'ID de la ressource concernee
     * @return true si le compteur de la ressource est un compteur dont on veut la valeur et qu'on n'a pas encore recu, sinon false.
     */
    boolean isCounterNeeded(int resourceID) {
        return this.resourceSet.contains(resourceID) && !this.counterReceived.contains(resourceID);
    }

    /**
     * @param resourceID l'ID de la ressource concernee
     * @return true si c'est une ressource dont on a besoin pour entrer en CS, sinon false.
     */
    boolean isTokenNeeded(int resourceID) {
        return this.resourceSet.contains(resourceID);
    }

    public boolean addTokenRequestSend(TokenRequest tokenRequest) {
        if (!this.listTokenRequestSend.contains(tokenRequest)) {
            this.listTokenRequestSend.add(tokenRequest);
            return true;
        } else {
            System.out.println("TOKEN_REQUEST DEJA ENREGISTREE.");
            return false;
        }
    }

    // Getters and Setters.

    public Set<Integer> getResourceSet() {
        return this.resourceSet;
    }

    public double getMyRequestMark() {
        return this.myRequestMark;
    }

    public void setMyRequestMark(double myRequestMark) {
        this.myRequestMark = myRequestMark;
    }

    public List<TokenRequest> getListTokenRequestSend() {
        return this.listTokenRequestSend;
    }

}
