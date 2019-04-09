package peersim;

import common.message.*;
import common.util.Token;
import peersim.config.Configuration;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;
import peersim.transport.Transport;

import java.util.*;

public class AlgoJL implements EDProtocol {

    // Constants.

    private final int MIN_CS;
    private final int MAX_CS;

    // Variables.

    /**
     * <p>Le protocol de transportPID.</p>
     */
    private final int transportPID;

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

    /**
     * <p>La map des requete pendante, permet de verifier si une requete est obselete ou pas.</p>
     */
    private List<Request> listPendingRequest = new LinkedList<>();

    private int nb_cs;

    /**
     * <p>La liste des set de ressources de chaque demande de CS que le noeud doit effectuer.</p>
     */
    private List<Set<Integer>> listSetRequestCS = new ArrayList<>();

    /**
     * <p>Permet de recuperer le la requete CS courante, doit etre incremente à chaque fin de CS.</p>
     */
    private int iteListSetRequestCS = 0;

    // Constructors.

    public AlgoJL(String prefix) {
        this(Configuration.getPid(prefix + ".tr"), Configuration.getInt(prefix + "nb_resource"),
                Configuration.lookupPid(prefix.split("\\.")[prefix.split("\\.").length - 1]),
                Configuration.getInt(prefix + ".nb_cs"),
                Configuration.getInt(prefix + ".min_cs"),
                Configuration.getInt(prefix + ".max_cs"));
    }

    private AlgoJL(int transportPID, int nbResource, int myPid, int nb_cs, int min_cs, int max_cs) {
        this.MIN_CS = min_cs;
        this.MAX_CS = max_cs;

        this.myPid = myPid;
        this.transportPID = transportPID;
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

        this.nb_cs = nb_cs;
        // On cree les set de resources de chaque Requete CS.
        for (int i = 0; i < this.nb_cs; i++) {
            Set<Integer> setResources = new TreeSet<Integer>();

            int nbRes = (int) ((Math.random() * (this.nbResource + 1)) + 1);

            int j = 0;

            while (j < nbRes) {
                int generate = (int) ((Math.random() * (this.nbResource)));

                if (setResources.add(generate))
                    j++;
            }

            this.listSetRequestCS.add(setResources);
        }

        for (int i = 0; i < this.listSetRequestCS.size(); i++) {
            System.out.println("CS n°" + i + " = " + this.listSetRequestCS.get(i));
        }
    }

    // Methods.

    /**
     * <p>Doit etre lance quand on veut faire une demande de CS. Ne peut-etre lance qu'une fois, il faut ensuite passer en CS puis faire {@link AlgoJL#releaseCS()} pour pouvoir refaire requestCS.</p>
     *
     * @param resources le set de ressources requises pour entrer en CS.
     */
    public void requestCS(Set<Integer> resources) {
        if (this.currentRequestingCS == null && this.state == State.NOTHING) {
            this.currentRequestingCS = new RequestingCS(resources);

            this.requestID++;

            this.setState(State.WAIT_S);

            for (int resourceID : resources) {
                if (!this.getToken(resourceID).isHere()) { // Si le jeton n'est pas present sur notre noeud.
                    CounterRequest counterRequest = new CounterRequest(resourceID, this.requestID, this.node, this.dynamicTree[resourceID]);
                    this.sendMessage(counterRequest);
                } // Sinon le constructeur de RequestingCS a deja mis a jour les compteurs pour les jetons deja present sur le noeud.
            }
        } else {
            if (this.currentRequestingCS != null)
                System.err.println("ATTENTION!!! DEMANDE DE CS ALORS QU'IL Y EN A UNE DEJA EN COURS.");

            if (this.state != State.NOTHING)
                System.err.println("ATTENTION!!! DEMANDE DE CS ALORS QUE L'ETAT N'EST PAS NOTHING.");
        }
    }

    /**
     * <p>Relache la CS. Est appelee lorsque le message {@link ReleaseMessage} est recu.</p>
     */
    public void releaseCS() {
        this.setState(State.NOTHING);

        Set<Integer> resourceRequired = this.currentRequestingCS.getResourceSet();

        for (int resourceID : resourceRequired) {
            Token token = this.arrayToken[resourceID];

            token.putLastCS(this.node, this.requestID);

            if (!token.tokenRequestQueueEmpty()) {
                TokenRequest headTokenRequest = token.nextTokenRequest();

                Token tokenSend = this.currentRequestingCS.sendToken(resourceID, headTokenRequest.getSender());

                TokenMessage tokenM = new TokenMessage(tokenSend, headTokenRequest.getResourceID(), this.node, headTokenRequest.getSender());

                this.sendMessage(tokenM);
            }
        }

        this.iteListSetRequestCS++;

        this.currentRequestingCS = null;

        int delay = Util.generateRandom(this.MIN_CS, this.MAX_CS);
        EDSimulator.add(delay, new BeginMessage(-1, null, null), this.node, this.myPid);
    }

    private void receiveCounterRequest(CounterRequest counterRequest) {
        int resourceID = counterRequest.getResourceID();
        int requestID = counterRequest.getRequestID();
        Node sender = counterRequest.getSender();

        if (this.isObsoletedRequest(counterRequest))
            return;

        if (this.getToken(counterRequest.getResourceID()).isHere()) {
            CounterMessage counterMessage = new CounterMessage(this.getToken(resourceID).incrementCounter(), resourceID, this.node, sender);

            this.arrayToken[requestID].putLastReqC(sender, counterRequest.getRequestID());

            this.sendMessage(counterMessage);
        } else { // Si on a pas le jeton, on transmet.
            CounterRequest cR = new CounterRequest(resourceID, requestID, sender, this.dynamicTree[resourceID]);
            this.listPendingRequest.add(cR);
            this.sendMessage(cR);
        }
    }

    private void receiveTokenRequest(TokenRequest tokenRequest) {
        int resourceID = tokenRequest.getResourceID();
        Node sender = tokenRequest.getSender();

        if (this.isObsoletedRequest(tokenRequest))
            return;

        if (this.getToken(resourceID).isHere()) {
            if (this.state == State.WAIT_S || (this.currentRequestingCS != null && !this.currentRequestingCS.isTokenNeeded(resourceID))) { // Si c'est une ressource dont on a pas besoin ou qu'on attend encore tout les compteurs.
                this.sendToken(resourceID, sender);
            } else {
                if (!this.arrayToken[resourceID].contains(tokenRequest)) {
                    if (this.state == State.WAIT_CS && this.compareRequest(tokenRequest, this.currentRequestingCS.getMyRequestMark())) { // Si la requete recue est plus prioritaire.
                        TokenRequest myTokenRequest = new TokenRequest(this.currentRequestingCS.getMyRequestMark(), resourceID, this.requestID, this.node, this.node);

                        this.arrayToken[resourceID].addTokenRequest(myTokenRequest);

                        this.sendToken(resourceID, sender);
                    } else {
                        this.arrayToken[resourceID].addTokenRequest(tokenRequest);
                    }
                }
            }

        } else { // Si on a pas le jeton, on transmet.
            TokenRequest tR = new TokenRequest(tokenRequest.getMark(), tokenRequest.getResourceID(), tokenRequest.getRequestID(), tokenRequest.getSender(), this.dynamicTree[tokenRequest.getResourceID()]);
            this.listPendingRequest.add(tR);
            this.sendMessage(tR);
        }
    }

    private void sendToken(int resourceID, Node receiver) {
        Token tokenSend = this.currentRequestingCS.sendToken(resourceID, receiver);
        TokenMessage tokenMessage = new TokenMessage(tokenSend, resourceID, this.node, receiver);

        this.setNodeLink(resourceID, receiver);

        this.sendMessage(tokenMessage);
    }

    private void receiveCounter(CounterMessage counterMessage) {
        this.currentRequestingCS.receiveCounter(counterMessage);

        if (this.currentRequestingCS.allCounterAreReceived()) {
            this.receivedAllCounter();
        }
    }

    private void receiveToken(TokenMessage tokenMessage) {
        this.currentRequestingCS.receiveToken(tokenMessage);

        for (Request request : this.listPendingRequest) {
            if (this.isObsoletedRequest(request))
                continue;

            if (request instanceof CounterRequest) {
                this.arrayToken[request.getResourceID()].putLastReqC(request.getSender(), request.getRequestID());
                CounterMessage counterMessage = new CounterMessage(this.arrayToken[request.getResourceID()].incrementCounter(), request.getResourceID(), this.node, request.getSender());
                this.sendMessage(counterMessage);
            } else if (request instanceof TokenRequest) {
                if (!this.arrayToken[request.getResourceID()].contains((TokenRequest) request)) {
                    this.arrayToken[request.getResourceID()].addTokenRequest((TokenRequest) request);
                }
            }  // else LoanRequest.
        }

        if (this.currentRequestingCS.allTokenAreReceived()) {
            this.setState(State.IN_CS);

            // Genère un evenement qui lancera le relachement de la CS.

            System.out.println("Le noeud " + this.node.getID() + " est en CS avec les ressources = " + this.currentRequestingCS.getResourceSet());

            int delay = Util.generateRandom(this.MIN_CS, this.MAX_CS);
            EDSimulator.add(delay, new ReleaseMessage(-1, null, null), this.node, this.myPid);
        }

        if (this.state == State.WAIT_S && this.currentRequestingCS.allCounterAreReceived()) {
            this.receivedAllCounter();
        }

        Set<Token> ownedToken = this.getOwnedToken();

        for (Token token : ownedToken) {
            if (!token.tokenRequestQueueEmpty()) {
                TokenRequest headTokenRequest = token.seeHeadTokenRequestQueue();

                if (this.state == State.WAIT_S) {
                    headTokenRequest = token.nextTokenRequest();
                    this.sendToken(headTokenRequest.getResourceID(), headTokenRequest.getSender());
                } else if (this.state == State.WAIT_CS) {
                    if (this.compareRequest(headTokenRequest, this.currentRequestingCS.getMyRequestMark())) {
                        headTokenRequest = token.nextTokenRequest();
                        this.sendToken(headTokenRequest.getResourceID(), headTokenRequest.getSender());
                    }
                }
            }
        }

    }

    /**
     * <p>Envoie un message. (Toutes les infos comme a qui doit etre envoye le message sont dans le message).</p>
     *
     * @param message le message a envoyer
     */
    private void sendMessage(Message message) {
        Transport tr = (Transport) this.node.getProtocol(this.transportPID);

        tr.send(message.getSender(), message.getReceiver(), message, this.myPid);
    }

    /**
     * <p>Retourne la moyenne du vecteur de compteurs.</p>
     *
     * @return la valeur de la note des requete de jetons courant en se basant sur le vecteur de compteur.
     * @see AlgoJL#counterVector
     */
    private double computeMark() {
        double sum = 0.0;

        for (long counter : this.counterVector) {
            sum += (double) counter;
        }

        return sum / ((double) this.counterVector.length);
    }

    /**
     * <p>Effectue le traitement a faire lorsqu'on a recu tous les coutner.</p>
     */
    private void receivedAllCounter() {
        this.setState(State.WAIT_CS);

        double mark = this.computeMark();
        this.currentRequestingCS.setMyRequestMark(mark);

        for (int resource : this.currentRequestingCS.getResourceSet()) {
            if (!this.arrayToken[resource].isHere()) {
                TokenRequest tokenRequest = new TokenRequest(mark, resource, this.requestID, this.node, this.dynamicTree[resource]);

                this.sendMessage(tokenRequest);
            }
        }
    }

    /**
     * @param request la requete a verifier
     * @return true si la requete est obselete sinon false.
     */
    private boolean isObsoletedRequest(Request request) {
        if (request instanceof CounterRequest) {
            return this.arrayToken[request.getResourceID()].getLastReqC(request.getSender()) > request.getRequestID();
        } else if (request instanceof TokenRequest) {
            return this.arrayToken[request.getResourceID()].getLastCS(request.getSender()) > request.getRequestID();
        } else {
            // Pour les LoanRequest.
            return false;
        }
    }

    /**
     * @param tokenRequestReceived la requete que l'on a recue et que l'on va comparer a notre note
     * @param myRequestMark        la note de nos requete de jeton courantes.
     * @return true si la requete de jeton recue est plus prioritaire que la requete que nous avons envoye pour la demande de CS courante, sinon false.
     */
    private boolean compareRequest(TokenRequest tokenRequestReceived, double myRequestMark) {
        return tokenRequestReceived.getMark() < myRequestMark || ((tokenRequestReceived.getMark() == myRequestMark) && tokenRequestReceived.getSender().getID() < this.getNode().getID());
    }

    /**
     * @return un set de tous les jetons present sur le noeud.
     */
    private Set<Token> getOwnedToken() {
        Set<Token> setToken = new TreeSet<>();

        for (Token token : this.arrayToken) {
            if (token.isHere())
                setToken.add(token);
        }

        return setToken;
    }

    @Override
    public void processEvent(Node node, int i, Object o) {
        // TODO Reception de CounterRequest, TokenRequest, CounterMessage, TokenMessage.
        if (i == this.myPid) {
            if (o instanceof CounterRequest) {
                this.receiveCounterRequest((CounterRequest) o);
            } else if (o instanceof CounterMessage) {
                this.receiveCounter((CounterMessage) o);
            } else if (o instanceof TokenRequest) {
                this.receiveTokenRequest((TokenRequest) o);
            } else if (o instanceof TokenMessage) {
                this.receiveToken((TokenMessage) o);
            } else if (o instanceof ReleaseMessage) {
                this.releaseCS();
            } else if (o instanceof BeginMessage) {
                Set<Integer> setResource = this.listSetRequestCS.get(this.iteListSetRequestCS);
                this.requestCS(setResource);
            } else {
                throw new RuntimeException("Mauvais event");
            }
        } else {
            throw new RuntimeException("Mauvais ID");
        }
    }

    @Override
    public Object clone() {
        return new AlgoJL(this.transportPID, this.nbResource, this.myPid, this.nb_cs, this.MIN_CS, this.MAX_CS);
    }

    /**
     * <p>Methode appelee a l'initialisation pour le premiere noeud. Ce premier noeud possedera toutes les ressource au debut.</p>
     */
    public void setAllResourcesHere() {

        System.out.println("Je suis la dans le setAllResourceHere.");

        for (Token token : this.arrayToken) {
            token.setHere(true);
        }
    }

    /**
     * <p>Permet de faire pointer notre noeud sur le noeud link pour la ressource precisee en parametres.</p>
     * <p><strong>ATTENTION!</strong> Le jeton contenue dans {@link AlgoJL#arrayToken} et associe a la ressource n'est pas mis a jour avec cette methode.</p>
     *
     * @param resourceID l'ID de la ressource concernee
     * @param link       le nouveau noeud lien pour cette ressour (peut etre null)
     * @see Token#updateToken(Token)
     */
    public void setNodeLink(int resourceID, Node link) {
        this.dynamicTree[resourceID] = link;
    }

    /**
     * <p>Met a jour le counter associe a la ressource.</p>
     *
     * @param resourceID l'ID de la ressource concernee
     * @param counter    la nouvelle valeur du counter
     */
    private void setCounter(int resourceID, long counter) {
        this.counterVector[resourceID] = counter;
    }

    /**
     * @param resourceID l'ID de la ressource concernee
     * @return le jeton assoicie a la ressource.
     */
    private Token getToken(int resourceID) {
        return this.arrayToken[resourceID];
    }

    /**
     * <p>Met a jour le jeton associe a la ressource en se referenceant sur le jeton reference, entre en parametre.</p>
     *
     * @param resourceID l'ID de la ressource concernee
     * @param tokenRef   le jeton reference sur lequel va se base le jeton local pour se mettre a jour
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

    public boolean hasFinishAllCS() {
        return this.iteListSetRequestCS >= this.nb_cs;
    }

    // Public enum.

    public enum State {
        NOTHING, // On ne fait rien.
        WAIT_S, // On attend les compteurs.
        WAIT_CS, // on attend les jetons.
        IN_CS // On est en section critique.
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


        // Constructors.

        /**
         * <p>Initialise par default tous les set de receptions a vide.</p>
         * <p>Cependant, regarde si certains jeton de ressources demandees ne sont pas deja present sur le noeud. Si c'est le cas,
         * met a jours les set de reception et aussi le vecteur de compteur. (Cela signifie que la fonction {@link AlgoJL#requestCS(Set)} verifie seulement si oui ou non le jeton est present,
         * si le jeton est deja present, aucun traitement est a faire, le constructeur de {@link RequestingCS} le fait automatiquement.</p>
         *
         * @param resourceSet le set de ressource requises pour entrer en CS.
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
         * @param counterMessage le message de compteur que l'on vient de recevoir
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
         * @param tokenMessage le message de jeton que l'on vient de recevoir
         */
        void receiveToken(TokenMessage tokenMessage) {
            int resourceID = tokenMessage.getResourceID();
            Token token = tokenMessage.getToken();

            boolean res = this.tokenReceived.add(tokenMessage.getResourceID());
            if (res) {
                this.parent.updateToken(resourceID, token);
                this.parent.setNodeLink(resourceID, null);

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
            Token tokenSend = (Token) this.parent.arrayToken[resourceID].clone();
            this.parent.arrayToken[resourceID].setHere(false);
            this.parent.arrayToken[resourceID].clearAllQueue();
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
