package peersim;

import common.message.CounterMessage;
import common.message.Message;
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
     * <p>La note de chaque requete de jeton envoye pour cette section critque. Utile pour comparer avec d'autres requete que l'on recevra plus tard -> {@link AlgoJL#sendMessage(Message, boolean)}.</p>
     * <p>Pour eviter tout probleme si jamais on a pas encore calculer notre mark, initialisee a {@link Double#MAX_VALUE}</p>
     */
    private double myRequestMark = Double.MAX_VALUE;

    /**
     * <p>Le set de ressources qui représente toutes les ressources que l'on veut pour entrer en CS.</p>
     */
    private final Set<Integer> resourceRequiredSet;

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

    /**
     * <p>Toutes les requete de token que l'on a envoyee pour netre en CS.</p>
     */
    private List<TokenRequest> listTokenRequestSend;


    // Constructors.

    /**
     * <p>Initialise par default tous les set de receptions a vide.</p>
     * <p>Cependant, regarde si certains jeton de ressources demandees ne sont pas deja present sur le noeud. Si c'est le cas,
     * met a jours les set de reception et aussi le vecteur de compteur. (Cela signifie que la fonction verifie seulement si oui ou non le jeton est present,
     * si le jeton est deja present, aucun traitement est a faire, le constructeur de {@link RequestingCS} le fait automatiquement.</p>
     *
     * @param resourceSet le set de ressource requises pour entrer en CS.
     */
    public RequestingCS(Set<Integer> resourceSet, AlgoJL parent) {
        this.parent = parent;

        this.resourceRequiredSet = resourceSet;

        this.counterReceived = new TreeSet<>();
        for (int resourceID = 0; resourceID < this.parent.getArrayToken().length; resourceID++) {
            if (this.parent.hasToken(resourceID) && this.resourceRequiredSet.contains(resourceID)) {
                this.counterReceived.add(resourceID);
                this.parent.setCounter(resourceID, this.parent.getToken(resourceID).incrementCounter());
            }
        }

        this.tokenReceived = new TreeSet<>();
        for (int resourceID = 0; resourceID < this.parent.getArrayToken().length; resourceID++) {
            if (this.parent.hasToken(resourceID) && this.resourceRequiredSet.contains(resourceID)) {
                this.tokenReceived.add(resourceID);
            }
        }

        this.listTokenRequestSend = new ArrayList<>();
    }

    // Methods.

    /**
     * @return true si tous les compteurs de chaque ressources ont ete recu, sinon false.
     */
    public boolean allCounterAreReceived() {
        return this.counterReceived.containsAll(this.resourceRequiredSet);
    }

    /**
     * @return true si tous les jetons de chaque ressources ont ete recu, sinon false.
     */
    public boolean allTokenAreReceived() {
        return this.tokenReceived.containsAll(this.resourceRequiredSet);
    }

    /**
     * <p>Met a jour le vecteur de compteur et les liens de l'arbre dynamique.</p>
     * <p>Ne fait aucun traitement qui aurait un rapport avec le fait que tout les compteurs ont ete recus. Apres avoir appele cette methode,
     * il faut appeler la mehtode {@link RequestingCS#allCounterAreReceived()} pour savoir si tous les compteurs sont recu et ensuite effectuer le traitement approprie.</p>
     *
     * @param counterMessage le message de compteur que l'on vient de recevoir
     */
    public void receiveCounter(CounterMessage counterMessage) {
        int resourceID = counterMessage.getResourceID();
        long counter = counterMessage.getCounter();
        Node sender = counterMessage.getSender();

        /*System.out.println("-------------------------------------------------------------------------");

        System.out.println("Node = " + this.parent.getNode().getID() + " Reception counter pour = " + resourceID);
        System.out.println("SENDER = " + sender.getID());*/

        boolean res = this.counterReceived.add(resourceID);

        assert res : "Sender = " + sender.getID() + " N = " + this.parent.getNode().getID();

        if (res) {
            /*System.out.println("Node = " + this.parent.getNode().getID() + " AJOUT POUR = " + resourceID);*/
            this.parent.setCounter(resourceID, counter);
            this.parent.setNodeLink(resourceID, sender);
            /*System.out.println("-------------------------------------------------------------------------");*/
        } else {
            try {
                throw new Exception("PB - > ATTENTION!!! Reception d'un COUNTER que l'on a deja recu.");
            } catch (Exception e) {
                e.printStackTrace();
            }
            /*System.out.println("N = " + this.parent.getNode().getID() + " PB -> DEJA RECU POUR = " + resourceID);
            System.out.println("-------------------------------------------------------------------------");*/
        }
    }

    /**
     * <p>Met a jour le set de ressources recues et les liens de l'arbre dynamique.</p>
     * <p>Ne fait aucun traitement qui aurait un rapport avec le fait que toutes les ressources ont ete recues. Apres avoir appele cette methode,
     * il faut appeler la mehtode {@link RequestingCS#allTokenAreReceived()} pour savoir si tous les jetons sont recu et ensuite effectuer le traitement approprie.</p>
     *
     * @param token le jeton que l'on vient de recevoir
     */
    public void receiveToken(Token token) {
        int resourceID = token.getResourceID();

        boolean res = this.tokenReceived.add(token.getResourceID());
        if (res) {
            this.parent.tokenArrived(token);
            this.parent.setNodeLink(resourceID, null);

            if (this.isCounterNeeded(resourceID)) {
                this.counterReceived.add(resourceID);
                this.parent.getArrayToken()[resourceID].putLastReqC(this.parent.getNode().getID(), this.parent.getRequestID());
                this.parent.setCounter(resourceID, this.parent.getToken(resourceID).incrementCounter());
            }
        } else {
           /* System.err.println("N = " + this.parent.getNode().getID() + "PB - > ATTENTION!!! Reception d'un TOKEN que l'on a deja recu.");*/
        }
    }

    public void removeTokenReceived(int resourceID) {
        this.tokenReceived.remove(resourceID);
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
    /*public void sendToken(int resourceID, Node tokenReceiver) {
        this.parent.setNodeLink(resourceID, tokenReceiver);
        this.tokenReceived.remove(resourceID);
    }*/

    /**
     * @param resourceID l'ID de la ressource concernee
     * @return true si le compteur de la ressource est un compteur dont on veut la valeur et qu'on n'a pas encore recu, sinon false.
     */
    public boolean isCounterNeeded(int resourceID) {
        return this.resourceRequiredSet.contains(resourceID) && !this.counterReceived.contains(resourceID);
    }

    /**
     * @param resourceID l'ID de la ressource concernee
     * @return true si c'est une ressource dont on a besoin pour entrer en CS, sinon false.
     */
    public boolean isTokenNeeded(int resourceID) {
        return this.resourceRequiredSet.contains(resourceID);
    }

    public boolean addTokenRequestSend(TokenRequest tokenRequest) {
        if (!this.listTokenRequestSend.contains(tokenRequest)) {
            this.listTokenRequestSend.add(tokenRequest);
            return true;
        } else {
            /*System.out.println("TOKEN_REQUEST DEJA ENREGISTREE.");*/
            return false;
        }
    }

    public TokenRequest getTokenRequestSend(int resourceID) {
        for (TokenRequest tokenRequest : this.listTokenRequestSend) {
            if (tokenRequest.getResourceID() == resourceID) {
                return tokenRequest;
            }
        }

        return null;
    }

    // Getters and Setters.

    public Set<Integer> getMissingResource() {
        Set<Integer> missingResource = new TreeSet<>();

        for (int resource : this.resourceRequiredSet) {
            if (!this.tokenReceived.contains(resource)) {
                missingResource.add(resource);
            }
        }

        return missingResource;
    }

    public Set<Integer> getResourceRequiredSet() {
        return this.resourceRequiredSet;
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
