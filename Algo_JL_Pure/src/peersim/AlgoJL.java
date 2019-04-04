package peersim;

import common.message.CounterMessage;
import common.message.CounterRequest;
import common.message.TokenRequest;
import common.message.TokenMessage;
import common.util.Token;
import peersim.config.Configuration;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.transport.Transport;

import java.util.Set;
import java.util.TreeSet;

public class AlgoJL implements EDProtocol {

    // Variables.

    /**
     * <p>Le protocol de transport.</p>
     */
    private final int transport;

    /**
     * <p>Nombre de ressource disponnible dans le systeme.</p>
     */
    private final int nbResource;

    /**
     * <p>Le PID du protocole AlgoJL.</p>
     */
    private final int myPid;

    /**
     * <p>Le noeud sur lequel l'instance du protocole est. (Initialisee grace au controler {@link InitJL} qui fait appel a la methode {@link AlgoJL#setNode(Node)}</p>
     */
    private Node node;

    /**
     * <p>Represente les jetons de chaque ressource. Les ressources sont presente ou non sur le site. Pour le savoir il faut appeler la methode {@link Token#isHere()}.</p>
     */
    private Token[] arrayToken;

    /**
     * <p>Point sur le noeud à qui il faut s'addresser pour acceder à la ressource d'indice i.</p>
     * <p>Si dynamicTree[i] est null alors cela signifie que nous possedons la ressource i.</p>
     */
    private Node[] dynamicTree;

    /**
     * <p>Vecteur de compteur. Tous initialement a 0.</p>
     */
    private long[] counterVector;

    /**
     * <p>ID de la requete courante.</p>
     */
    private int requestID = 0;

    /**
     * <p>Decris l'etat du noeud.</p>
     * <p>Initialement à {@link State#NOTHING}</p>
     *
     * @see RequestingCS
     */
    private AlgoJL.State state = State.NOTHING;

    /**
     * <p>Permet de gerer les receptions des compteurs et des ressources.</p>
     * <p>Null si aucune requete n'est en cours.</p>
     */
    private RequestingCS currentRequestingCS;

    // Constructors.

    public AlgoJL(String prefix) {
        this(Configuration.getPid(prefix + ".tr"), Configuration.getInt(prefix + "nb_resource"), Configuration.lookupPid(prefix.split("\\.")[prefix.split("\\.").length - 1]));
    }

    private AlgoJL(int transport, int nbResource, int myPid) {
        this.myPid = myPid;
        this.transport = transport;
        this.nbResource = nbResource;

        this.arrayToken = new Token[this.nbResource];
        for (int i = 0; i < this.arrayToken.length; i++) {
            this.arrayToken[i] = new Token(this, i);
        }

        this.dynamicTree = new Node[this.nbResource];

        System.out.println("Nb ressource = " + this.nbResource);

        this.counterVector = new long[this.nbResource];
        for (int i = 0; i < this.counterVector.length; i++) {
            this.counterVector[i] = 0;
        }
    }

    // Methods.

    /**
     * <p>Doit etre lance quand on veut faire une demande de CS. Ne peut-etre lance qu'une fois, il faut ensuite passer en CS puis faire {@link AlgoJL#releaseCS()} pour pouvoir refaire requestCS.</p>
     *
     * @param resources
     */
    public void requestCS(Set<Integer> resources) {
        if (this.currentRequestingCS == null && this.state == State.NOTHING) {
            this.currentRequestingCS = new RequestingCS(resources);

            this.requestID++;

            this.setState(State.WAIT_S);

            for (int resourceID : resources) {
                if (!this.getToken(resourceID).isHere()) { // Si le jeton n'est pas present sur notre noeud.
                    CounterRequest counterRequest = new CounterRequest(resourceID, this.requestID, this.node, this.dynamicTree[resourceID]);
                    this.sendCounterRequest(counterRequest);
                } // Sinon le constructeur de RequestingCS a deja mis a jour les compteurs pour les jetons deja present sur le noeud.
            }
        } else {
            if (this.currentRequestingCS != null)
                System.err.println("ATTENTION!!! DEMANDE DE CS ALORS QU'IL Y EN A UNE DEJA EN COURS.");

            if (this.state != State.NOTHING)
                System.err.println("ATTENTION!!! DEMANDE DE CS ALORS QUE L'ETAT N'EST PAS NOTHING.");
        }
    }

    public void releaseCS() {
        // TODO
    }

    private void receiveCounterRequest(CounterRequest counterRequest) {
        int resourceID = counterRequest.getResourceID();
        int requestID = counterRequest.getRequestID();
        Node sender = counterRequest.getSender();

        if (this.getToken(counterRequest.getResourceID()).isHere()) {
            Transport tr = (Transport) this.node.getProtocol(this.transport);

            CounterMessage counterMessage = new CounterMessage(this.getToken(resourceID).incrementCounter(), resourceID, this.node, sender);

            this.sendCounter(counterMessage);
        } else { // Si on a pas le jeton, on transmet.
            CounterRequest cR = new CounterRequest(resourceID, requestID, sender, this.dynamicTree[resourceID]);
            this.sendCounterRequest(cR);
        }
    }

    private void sendCounterRequest(CounterRequest counterRequest) {
        // TODO
    }

    private void receiveTokenRequest(TokenRequest tokenRequest) {
        int resourceID = tokenRequest.getResourceID();
        Node sender = tokenRequest.getSender();

        if (this.getToken(resourceID).isHere()) {
            if (this.state == State.WAIT_S || (this.currentRequestingCS != null && !this.currentRequestingCS.isTokenNeeded(resourceID))) { // Si c'est une ressource dont on a pas besoin ou qu'on attend encore tout les compteurs.
                Token tokenSend = this.currentRequestingCS.sendToken(resourceID, sender);
                TokenMessage tokenMessage = new TokenMessage(tokenSend, resourceID, this.node, sender);
                this.sendToken(tokenMessage);
            } else {
                if (!this.arrayToken[resourceID].contains(tokenRequest)) {
                    if (this.state == State.WAIT_CS && this.compareRequest(tokenRequest, this.currentRequestingCS.getMyRequestMark())) { // Si la requete recue est plus prioritaire.
                        TokenRequest myTokenRequest = new TokenRequest(this.currentRequestingCS.getMyRequestMark(), resourceID, this.requestID, this.node, this.node);

                        this.arrayToken[resourceID].addTokenRequest(myTokenRequest);

                        Token tokenSend = this.currentRequestingCS.sendToken(resourceID, sender);

                        TokenMessage tokenMessage = new TokenMessage(tokenSend, resourceID, this.node, sender);

                        this.sendToken(tokenMessage);
                    } else {
                        this.arrayToken[resourceID].addTokenRequest(tokenRequest);
                    }
                }
            }

        } else { // Si on a pas le jeton, on transmet.
            TokenRequest tR = new TokenRequest(tokenRequest.getMark(), tokenRequest.getResourceID(), tokenRequest.getRequestID(), tokenRequest.getSender(), this.dynamicTree[tokenRequest.getResourceID()]);
            this.sendTokenRequest(tR);
        }
    }

    private void sendTokenRequest(TokenRequest tokenRequest) {
        // TODO
    }

    private void receiveCounter(CounterMessage counterMessage) {
        this.currentRequestingCS.receiveCounter(counterMessage);

        if (this.currentRequestingCS.allCounterAreReceived()) {
            this.setState(State.WAIT_CS);

            double mark = this.computeMark();
            this.currentRequestingCS.setMyRequestMark(mark);

            for (int resource : this.currentRequestingCS.getResourceSet()) {
                if (!this.arrayToken[resource].isHere()) {
                    TokenRequest tokenRequest = new TokenRequest(mark, resource, this.requestID, this.node, this.dynamicTree[resource]);

                    this.sendTokenRequest(tokenRequest);
                }
            }
        }
    }

    private void sendCounter(CounterMessage counterMessage) {
        // TODO
    }

    private void receiveToken(TokenMessage tokenMessage) {
        // TODO
    }

    private void sendToken(TokenMessage tokenMessage) {
        // TODO
    }

    /**
     * <p>Retourne la moyenne du vecteur de compteurs.</p>
     *
     * @return la valeur de la note des requete de jetons courant en se basant sur le vecteur de compteur.
     * @see {@link AlgoJL#counterVector}
     */
    private double computeMark() {
        double sum = 0.0;

        for (long counter : this.counterVector) {
            sum += (double) counter;
        }

        return sum / ((double) this.counterVector.length);
    }

    /**
     * @param tokenRequestReceived
     * @param myRequestMark
     * @return true si la requete de jeton recue est plus prioritaire que la requete que nous avons envoye pour la demande de CS courante, sinon false.
     */
    private boolean compareRequest(TokenRequest tokenRequestReceived, double myRequestMark) {
        return tokenRequestReceived.getMark() < myRequestMark || ((tokenRequestReceived.getMark() == myRequestMark) && tokenRequestReceived.getSender().getID() < this.getNode().getID());
    }

    @Override
    public void processEvent(Node node, int i, Object o) {
        // TODO Reception de CounterRequest, TokenRequest, CounterMessage, TokenMessage.
        if (o instanceof CounterRequest) {
            this.receiveCounterRequest((CounterRequest) o);
        } else if (o instanceof CounterMessage) {
            this.receiveCounter((CounterMessage) o);
        } else if (o instanceof TokenRequest) {
            this.receiveTokenRequest((TokenRequest) o);
        } else if (o instanceof TokenMessage) {
            this.receiveToken((TokenMessage) o);
        } else {
            throw new RuntimeException("Mauvais event");
        }
    }

    @Override
    public Object clone() {
        return new AlgoJL(this.transport, this.nbResource, this.myPid);
    }

    /**
     * <p>Methode appelee a l'initialisation pour le premiere noeud. Ce premier noeud possedera toutes les ressource au debut.</p>
     */
    public void setAllResourcesHere() {

        System.out.println("Je suis la dans le setAllResourceHere.");

        for (int i = 0; i < this.arrayToken.length; i++) {
            this.arrayToken[i].setHere(true);
        }
    }

    /**
     * <p>Permet de faire pointer notre noeud sur le noeud link pour la ressource precisee en parametres.</p>
     * <p><strong>ATTENTION!</strong> Le jeton contenue dans {@link AlgoJL#arrayToken} et associe a la ressource n'est pas mis a jour avec cette methode.</p>
     *
     * @param resourceID
     * @param link
     * @see Token#updateToken(Token)
     */
    public void setNodeLink(int resourceID, Node link) {
        this.dynamicTree[resourceID] = link;
    }

    /**
     * <p>Met a jour le counter associe a la ressource.</p>
     *
     * @param resourceID
     * @param counter
     */
    private void setCounter(int resourceID, long counter) {
        this.counterVector[resourceID] = counter;
    }

    /**
     * @param resourceID
     * @return le jeton assoicie a la ressource.
     */
    private Token getToken(int resourceID) {
        return this.arrayToken[resourceID];
    }

    /**
     * <p>Met a jour le jeton associe a la ressource en se referenceant sur le jeton reference, entre en parametre.</p>
     *
     * @param resourceID
     * @param tokenRef
     */
    private void updateToken(int resourceID, Token tokenRef) {
        this.arrayToken[resourceID].updateToken(tokenRef);
    }

    // Getters and Setters.

    public int getMyPid() {
        return this.myPid;
    }

    public int getNbResource() {
        return this.nbResource;
    }

    /**
     * @return le noeud sur lequel est l'instance de ce protocole. Si la valeur n'a pas ete initialise, retourne null.
     */
    public Node getNode() {
        return this.node;
    }

    /**
     * <p>Met la valeur de {@link AlgoJL#node} a node. Si la valeur a deja ete initialise, aucun changement n'est fait.</p>
     *
     * @param node
     */
    public void setNode(Node node) {
        if (this.node == null) {
            this.node = node;
        }
    }

    public AlgoJL.State getState() {
        return this.state;
    }

    private void setState(AlgoJL.State state) {
        this.state = state;
    }

    // Public enum.

    public enum State {
        NOTHING, // On ne fait rien.
        WAIT_S, // On attend les compteurs.
        WAIT_CS, // on attend les jetons.
        IN_CS; // On est en section critique.
    }

    // Private class.

    /**
     * <p>Maintient les set qui permettent de savoir quels compteur a ete recu et quel ressource a ete recue.</p>
     */
    private class RequestingCS {

        // Variables.

        /**
         * <p>Reference directe sur l'algo pour le mettre a jour.</p>
         */
        private final AlgoJL parent;

        /**
         * <p>La note de chaque requete de jeton envoye pour cette section critque. Utile pour comparer avec d'autres requete que l'on recevra plus tard -> {@link AlgoJL#sendTokenRequest(TokenRequest)}.</p>
         */
        private double myRequestMark;

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


        // Constructors.

        /**
         * <p>Initialise par default tous les set de receptions a vide.</p>
         * <p>Cependant, regarde si certains jeton de ressources demandees ne sont pas deja present sur le noeud. Si c'est le cas,
         * met a jours les set de reception et aussi le vecteur de compteur. (Cela signifie que la fonction {@link AlgoJL#requestCS(Set)} verifie seulement si oui ou non le jeton est present,
         * si le jeton est deja present, aucun traitement est a faire, le constructeur de {@link RequestingCS} le fait automatiquement.</p>
         *
         * @param resourceSet
         */
        RequestingCS(Set<Integer> resourceSet) {
            this.parent = AlgoJL.this;

            this.resourceSet = resourceSet;

            this.counterReceived = new TreeSet<>();
            for (int resourceID = 0; resourceID < this.parent.arrayToken.length; resourceID++) {
                Token token = this.parent.getToken(resourceID);
                if (token.isHere() && this.resourceSet.contains(resourceID)) {
                    this.counterReceived.add(resourceID);
                    this.parent.setCounter(resourceID, token.incrementCounter());
                }
            }

            this.tokenReceived = new TreeSet<>();
            for (int resourceID = 0; resourceID < this.parent.arrayToken.length; resourceID++) {
                Token token = this.parent.getToken(resourceID);
                if (token.isHere() && this.resourceSet.contains(resourceID)) {
                    this.tokenReceived.add(resourceID);
                }
            }
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
         * @param counterMessage
         */
        void receiveCounter(CounterMessage counterMessage) {
            int resourceID = counterMessage.getResourceID();
            long counter = counterMessage.getCounter();
            Node sender = counterMessage.getSender();

            boolean res = this.counterReceived.add(resourceID);
            if (res) {
                this.parent.setCounter(resourceID, counter);
                this.parent.setNodeLink(resourceID, sender);
            } else {
                System.err.println("ATTENTION!!! Reception d'un COUNTER que l'on a deja recu.");
            }
        }

        /**
         * <p>Met a jour le set de ressources recues et les liens de l'arbre dynamique.</p>
         * <p>Ne fait aucun traitement qui aurait un rapport avec le fait que toutes les ressources ont ete recues. Apres avoir appele cette methode,
         * il faut appeler la mehtode {@link RequestingCS#allTokenAreReceived()} pour savoir si tous les jetons sont recu et ensuite effectuer le traitement approprie.</p>
         *
         * @param tokenMessage
         */
        void receiveToken(TokenMessage tokenMessage) {
            int resourceID = tokenMessage.getResourceID();
            Token token = tokenMessage.getToken();

            boolean res = this.tokenReceived.add(tokenMessage.getResourceID());
            if (res) {
                this.parent.updateToken(resourceID, token);
                this.parent.setNodeLink(resourceID, null);

                if (this.isCounterNeeded(resourceID)) {
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
         * @param resourceID
         * @param tokenReceiver
         * @return le jeton clone que l'on peut envoye.
         */
        Token sendToken(int resourceID, Node tokenReceiver) {
            Token tokenSend = (Token) this.parent.arrayToken[resourceID].clone();
            this.parent.arrayToken[resourceID].clearAllQueue();
            this.parent.setNodeLink(resourceID, tokenReceiver);

            this.tokenReceived.remove(resourceID);

            return tokenSend;
        }

        /**
         * @param resourceID
         * @return true si le compteur de la ressource est un compteur dont on veut la valeur et qu'on n'a pas encore recu, sinon false.
         */
        boolean isCounterNeeded(int resourceID) {
            return this.resourceSet.contains(resourceID) && !this.counterReceived.contains(resourceID);
        }

        /**
         * @param resourceID
         * @return true si c'est une ressource dont on a besoin pour entrer en CS, sinon false.
         */
        boolean isTokenNeeded(int resourceID) {
            return this.resourceSet.contains(resourceID);
        }

        // Getters and Setters.

        public double getMyRequestMark() {
            return this.myRequestMark;
        }

        public void setMyRequestMark(double myRequestMark) {
            this.myRequestMark = myRequestMark;
        }

        // Getters and Setters.

        public Set<Integer> getResourceSet() {
            return this.resourceSet;
        }
    }
}
