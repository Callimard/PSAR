package peersim;

import common.message.*;
import common.util.Token;
import common.util.Util;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;
import peersim.transport.Transport;

import javax.annotation.Resource;
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
    private RequestingCS currentRequestingCS = null;

    /**
     * <p>La map des requete pendante, permet de verifier si une requete est obselete ou pas.</p>
     */
    private List<Request> listPendingRequest;

    private int nb_cs;

    /**
     * <p>La liste des set de ressources de chaque demande de CS que le noeud doit effectuer.</p>
     */
    private List<Set<Integer>> listSetRequestCS;

    /**
     * <p>Permet de recuperer le la requete CS courante, doit etre incremente à chaque fin de CS.</p>
     */
    private int iteListSetRequestCS = 0;

    // Constructors.

    public AlgoJL(String prefix) {
        this(Configuration.getPid(prefix + ".tr"), Configuration.getInt(prefix + ".nb_resource"),
                Configuration.lookupPid(prefix.split("\\.")[prefix.split("\\.").length - 1]),
                Configuration.getInt(prefix + ".nb_cs"),
                Configuration.getInt(prefix + ".min_cs"),
                Configuration.getInt(prefix + ".max_cs"));
    }

    /**
     * @param transportPID
     * @param nbResource
     * @param myPid
     * @param nb_cs
     * @param min_cs
     * @param max_cs
     */
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

        this.listPendingRequest = new LinkedList<>();
        this.listSetRequestCS = new ArrayList<>();

        this.nb_cs = nb_cs;
        // On cree les set de resources de chaque Requete CS.
        for (int i = 0; i < this.nb_cs; i++) {
            Set<Integer> setResources = new TreeSet<Integer>();

            int nbRes = Util.generateRandom(1, this.nbResource);

            int j = 0;

            while (j < nbRes) {
                int generate = CommonState.r.nextInt(this.nbResource);

                if (setResources.add(generate)) {
                    j++;
                }
            }

            this.listSetRequestCS.add(setResources);
        }

        System.out.println("----------------------------------------------------------------------");

        for (int i = 0; i < this.listSetRequestCS.size(); i++) {
            System.out.println(this.listSetRequestCS.get(i));
        }

        System.out.println("----------------------------------------------------------------------");
    }

    // Methods.

    @Override
    public Object clone() {
        /*AlgoJL algoJL = null;

        try {
            algoJL = (AlgoJL) super.clone();

            algoJL.arrayToken = new Token[algoJL.nbResource];
            for (int i = 0; i < algoJL.arrayToken.length; i++) {
                algoJL.arrayToken[i] = new Token(this, i);
            }

            algoJL.dynamicTree = new Node[algoJL.nbResource];

            algoJL.counterVector = new long[algoJL.nbResource];
            for (int i = 0; i < algoJL.counterVector.length; i++) {
                algoJL.counterVector[i] = 0;
            }

            algoJL.nb_cs = this.nb_cs;
            algoJL.listSetRequestCS = new ArrayList<>();
            // On cree les set de resources de chaque Requete CS.
            for (int i = 0; i < algoJL.nb_cs; i++) {
                Set<Integer> setResources = new TreeSet<Integer>();

                int nbRes = Util.generateRandom(1, algoJL.nbResource);

                int j = 0;
                while (j < nbRes) {
                    int generate = (int) ((Math.random() * (algoJL.nbResource)));

                    if (setResources.add(generate))
                        j++;
                }

                algoJL.listSetRequestCS.add(setResources);
            }

        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        return algoJL;*/

        return new AlgoJL(this.transportPID, this.nbResource, this.myPid, this.nb_cs, this.MIN_CS, this.MAX_CS);
    }


    /**
     * <p>Doit etre lance quand on veut faire une demande de CS. Ne peut-etre lance qu'une fois, il faut ensuite passer en CS puis faire {@link AlgoJL#releaseCS()} pour pouvoir refaire requestCS.</p>
     *
     * @param resources le set de ressources requises pour entrer en CS.
     */
    public void requestCS(Set<Integer> resources) {
        System.out.println("ReqCS---------------------------------------------------------------------------------------");

        if (this.currentRequestingCS == null && this.state == State.NOTHING) {
            this.currentRequestingCS = new RequestingCS(resources, this);

            this.requestID++;

            this.setState(State.WAIT_S);

            System.out.println("N = " + this.node.getID() + " Req_CS = " + this.currentRequestingCS.getResourceSet());

            System.out.println("N = " + this.node.getID() + " ALL COUNTER");

            System.out.print("[");
            for (int i = 0; i < this.counterVector.length; i++) {
                System.out.print(" " + this.counterVector[i] + " ");
            }
            System.out.println("]");

            if (this.currentRequestingCS.allCounterAreReceived()) {
                System.out.println("N = " + this.node.getID() + " ALL COUNTER RECEIVED");
                this.receivedAllCounter();
            } else {
                for (int resourceID : resources) {
                    if (!this.getToken(resourceID).isHere()) { // Si le jeton n'est pas present sur notre noeud.
                        CounterRequest counterRequest = new CounterRequest(resourceID, this.requestID, this.node, this.dynamicTree[resourceID]);

                        System.out.println("N = " + this.node.getID() + " SEND REQ_C / R  = " + resourceID + ":");
                        this.sendMessage(counterRequest);
                    } // Sinon le constructeur de RequestingCS a deja mis a jour les compteurs pour les jetons deja present sur le noeud.
                }
            }
        } else {
            if (this.currentRequestingCS != null)
                System.out.println("ATTENTION!!! DEMANDE DE CS ALORS QU'IL Y EN A UNE DEJA EN COURS.");

            if (this.state != State.NOTHING)
                System.out.println("ATTENTION!!! DEMANDE DE CS ALORS QUE L'ETAT N'EST PAS NOTHING.");
        }

        System.out.println("---------------------------------------------------------------------------------------");
    }

    /**
     * <p>Relache la CS. Est appelee lorsque le message {@link ReleaseMessage} est recu.</p>
     */
    public void releaseCS() {
        System.out.println("RelCS--------------------------------------------------------------------------------------");

        this.setState(State.NOTHING);

        System.out.println("N = " + this.node.getID() + " currentRequestingCS = " + this.currentRequestingCS);

        Set<Integer> resourceRequired = this.currentRequestingCS.getResourceSet();

        System.out.println("N = " + this.node.getID() + " Release_CS / R_required = " + resourceRequired);

        for (int resourceID : resourceRequired) {
            Token token = this.arrayToken[resourceID];

            token.putLastCS(this.node.getID(), this.requestID);

            if (!token.tokenRequestQueueEmpty()) {
                TokenRequest headTokenRequest = token.nextTokenRequest();

                Token tokenSend = this.currentRequestingCS.sendToken(resourceID, headTokenRequest.getSender());

                TokenMessage tokenM = new TokenMessage(tokenSend, headTokenRequest.getResourceID(), this.node, headTokenRequest.getSender());

                System.out.println("N = " + this.node.getID() + " SEND T / R = " + resourceID);

                this.sendMessage(tokenM);
            }
        }

        this.iteListSetRequestCS++;

        System.out.println("N = " + this.node.getID() + " Release CS / R = " + this.currentRequestingCS.getResourceSet());

        this.currentRequestingCS = null;

        if (this.iteListSetRequestCS >= this.nb_cs) {
            System.out.println("N = " + this.node.getID() + " FIN DU NOEUD!!!!!!!!!!");
        } else {
            System.out.println("N = " + this.node.getID() + " JE SUIS ICI!!");

            int delay = Util.generateRandom(this.MIN_CS, this.MAX_CS);
            EDSimulator.add(delay, new BeginMessage(-1, null, null), this.node, this.myPid);
        }

        System.out.println("---------------------------------------------------------------------------------------");
    }

    private void receiveCounterRequest(CounterRequest counterRequest) {
        int resourceID = counterRequest.getResourceID();
        int requestID = counterRequest.getRequestID();
        Node sender = counterRequest.getSender();

        System.out.println("RcvREQ_C---------------------------------------------------------------------------------------");

        System.out.println("N = " + this.node.getID() + " R = " + resourceID + " FROM " + sender.getID() + " FOR " + counterRequest.getReceiver().getID());

        if (this.listPendingRequest.contains(counterRequest) || this.isObsoletedRequest(counterRequest) || counterRequest.isVisitedNode(this.node)) {
            System.out.println("Contains = " + this.listPendingRequest.contains(counterRequest));
            System.out.println("IsObselete = " + this.isObsoletedRequest(counterRequest));
            System.out.println("Visited Node = " + counterRequest.isVisitedNode(this.node));

            System.out.println("REQUETE COUNTER OBSELETE!!! Req = " + counterRequest);
            System.out.println("---------------------------------------------------------------------------------------");
            return;
        }

        if (this.getToken(resourceID).isHere()) {
            CounterMessage counterMessage = new CounterMessage(this.getToken(resourceID).incrementCounter(), resourceID, this.node, sender);

            this.arrayToken[resourceID].putLastReqC(sender.getID(), requestID);

            System.out.println("N = " + this.node.getID() + " SEND C / R = " + resourceID + ":");

            this.sendMessage(counterMessage);
        } else { // Si on a pas le jeton, on transmet.
            if (this.dynamicTree[resourceID] != this.node) {
                System.out.println("N = " + this.node.getID() + " ROUT / Counter R = " + resourceID + ":");

                /*CounterRequest cR = new CounterRequest(resourceID, requestID, sender, this.dynamicTree[resourceID]);
                cR.addAllVisitedNode(counterRequest.getVisitedNode());
                this.listPendingRequest.add(cR);
                this.sendMessage(cR);
                 */

                counterRequest.setReceiver(this.dynamicTree[resourceID]);
                counterRequest.addVisitedNode(this.node);

                this.listPendingRequest.add(counterRequest);
                this.sendMessage(counterRequest);
            } else {
                System.out.println("N = " + this.node.getID() + " GROS PB ON ENVOIE A SOIT MEME POUR UN ROUTAGE!!");
            }
        }

        System.out.println("---------------------------------------------------------------------------------------");
    }

    private void receiveTokenRequest(TokenRequest tokenRequest) {
        int resourceID = tokenRequest.getResourceID();
        Node sender = tokenRequest.getSender();

        System.out.println("RcvREQ_T---------------------------------------------------------------------------------------");

        System.out.println("N = " + this.node.getID() + " R = " + resourceID + " FROM " + sender.getID() + " FOR " + tokenRequest.getReceiver().getID());

        if (this.listPendingRequest.contains(tokenRequest) || this.isObsoletedRequest(tokenRequest) || tokenRequest.isVisitedNode(this.node)) {

            System.out.println("Contains = " + this.listPendingRequest.contains(tokenRequest));
            System.out.println("IsObselete = " + this.isObsoletedRequest(tokenRequest));
            System.out.println("Visited Node = " + tokenRequest.isVisitedNode(this.node));

            System.out.println("REQUETE TOKEN OBSELETE!!! Req = " + tokenRequest);
            System.out.println("---------------------------------------------------------------------------------------");
            return;
        }

        if (this.getToken(resourceID).isHere()) {
            if (this.state == State.WAIT_S || (this.currentRequestingCS != null && !this.currentRequestingCS.isTokenNeeded(resourceID)) || this.state == State.NOTHING) { // Si c'est une ressource dont on a pas besoin ou qu'on attend encore tout les compteurs.
                System.out.println("N = " + this.node.getID() + " SEND T / R = " + resourceID);
                this.sendToken(resourceID, sender);
            } else {
                if (!this.arrayToken[resourceID].contains(tokenRequest)) {
                    if (this.state == State.WAIT_CS && this.compareRequest(tokenRequest, this.currentRequestingCS.getMyRequestMark())) { // Si la requete recue est plus prioritaire.
                        TokenRequest myTokenRequest = new TokenRequest(this.currentRequestingCS.getMyRequestMark(), resourceID, this.requestID, this.node, this.node);

                        this.arrayToken[resourceID].addTokenRequest(myTokenRequest);

                        System.out.println("N = " + this.node.getID() + " SEND T / R = " + resourceID + " :");

                        this.sendToken(resourceID, sender);
                    } else {
                        System.out.println("N = " + this.node.getID() + " ADD REQ_T " + tokenRequest + " -> Token R = " + resourceID);
                        this.arrayToken[resourceID].addTokenRequest(tokenRequest);
                    }
                } else {
                    System.out.println("REQ_T -> " + tokenRequest + " ALREADY CONTAINS!!!");
                }
            }

        } else { // Si on a pas le jeton, on transmet.
            if (this.dynamicTree[tokenRequest.getResourceID()] != this.node) {
                System.out.println("N = " + this.node.getID() + " ROUT / Token R = " + resourceID + ":");

               /*TokenRequest tR = new TokenRequest(tokenRequest.getMark(), tokenRequest.getResourceID(), tokenRequest.getRequestID(), tokenRequest.getSender(), this.dynamicTree[tokenRequest.getResourceID()]);
                tR.addAllVisitedNode(tokenRequest.getVisitedNode());
                this.listPendingRequest.add(tR);
                this.sendMessage(tR);*/

                tokenRequest.setReceiver(this.dynamicTree[resourceID]);
                tokenRequest.addVisitedNode(this.node);

                this.listPendingRequest.add(tokenRequest);
                this.sendMessage(tokenRequest);
            } else {
                System.out.println("N = " + this.node.getID() + " GROS PB ON ENVOIE A SOIT MEME POUR UN ROUTAGE!!");
            }
        }

        System.out.println("---------------------------------------------------------------------------------------");
    }

    private void sendToken(int resourceID, Node receiver) {
        Token tokenSend = null;
        if (this.currentRequestingCS != null) {
            tokenSend = this.currentRequestingCS.sendToken(resourceID, receiver);
        } else {
            tokenSend = (Token) this.arrayToken[resourceID].clone();
            this.arrayToken[resourceID].setHere(false);
            this.arrayToken[resourceID].clearAllQueue();
            this.setNodeLink(resourceID, receiver);
        }
        TokenMessage tokenMessage = new TokenMessage(tokenSend, resourceID, this.node, receiver);

        this.setNodeLink(resourceID, receiver);

        this.sendMessage(tokenMessage);
    }

    private void receiveCounter(CounterMessage counterMessage) {
        System.out.println("RcvC---------------------------------------------------------------------------------------");

        System.out.println("N = " + this.node.getID() + " R = " + counterMessage.getResourceID() + " FROM " + counterMessage.getSender().getID() + " FOR " + counterMessage.getReceiver().getID());

        System.out.println("Counter = " + counterMessage.getCounter());

        if (this.currentRequestingCS == null) {
            System.out.println("N = " + this.node.getID() + " RECEPTION COUNTER R = " + counterMessage.getResourceID() + " ALORS QU'ON A PAS DEMANDER DE CS!!!!");
            return;
        }

        this.currentRequestingCS.receiveCounter(counterMessage);

        if (this.currentRequestingCS.allCounterAreReceived()) {
            this.receivedAllCounter();
        }

        System.out.println("---------------------------------------------------------------------------------------");
    }

    private void receiveToken(TokenMessage tokenMessage) {
        System.out.println("RcvT---------------------------------------------------------------------------------------");

        System.out.println("N = " + this.node.getID() + " R = " + tokenMessage.getResourceID() + " FROM " + tokenMessage.getSender().getID() + " FOR " + tokenMessage.getReceiver().getID());

        if (this.currentRequestingCS == null) {
            System.out.println("N = " + this.node.getID() + " RECEPTION TOKEN R = " + tokenMessage.getResourceID() + " ALORS QU'ON A PAS DEMANDE DE CS.");
        }

        if (this.currentRequestingCS != null) {
            System.out.println("N = " + this.node.getID() + " R = " + tokenMessage.getResourceID() + " RECEIVE TOKEN.");
            this.currentRequestingCS.receiveToken(tokenMessage);
        }

        System.out.println("N = " + this.node.getID() + " REQ_PENDING = " + this.listPendingRequest);

        for (Request request : this.listPendingRequest) {
            if (request.getResourceID() == tokenMessage.getResourceID()) {
                if (this.isObsoletedRequest(request)) {
                    System.out.println("REQUETE OBSELETE!!! Req = " + request);
                    continue;
                }

                if (request instanceof CounterRequest) {
                    this.arrayToken[request.getResourceID()].putLastReqC(request.getSender().getID(), request.getRequestID());
                    CounterMessage counterMessage = new CounterMessage(this.arrayToken[request.getResourceID()].incrementCounter(), request.getResourceID(), this.node, request.getSender());

                    System.out.println("N = " + this.node.getID() + " SEND C / R = " + counterMessage.getResourceID());

                    this.sendMessage(counterMessage);
                } else if (request instanceof TokenRequest) {
                    if (!this.arrayToken[request.getResourceID()].contains((TokenRequest) request)) {

                        System.out.println("N = " + this.node.getID() + " ADD REQ_T = " + request + " IN R = " + request.getResourceID());

                        this.arrayToken[request.getResourceID()].addTokenRequest((TokenRequest) request);
                    }
                }  // else LoanRequest.
            }
        }

        if (this.currentRequestingCS != null) {
            if (this.currentRequestingCS.allTokenAreReceived()) {
                this.setInCS();
            }
        }

        if (this.state == State.WAIT_S && this.currentRequestingCS != null && this.currentRequestingCS.allCounterAreReceived()) {
            System.out.println("N = " + this.node.getID() + " ALL COUNTER RECEIVED");
            this.receivedAllCounter();
        }

        /*Set<Token> ownedToken = this.getOwnedToken();*/
        Token[] ownedToken = this.getOwnedToken();

        System.out.print("N = " + this.node.getID() + " Token OWNED = ");
        System.out.print("[");
        for (Token value : ownedToken) {
            System.out.print(" " + value.getResourceID() + " ");
        }
        System.out.println("]");

        for (Token token : ownedToken) {
            if (!token.tokenRequestQueueEmpty()) {
                System.out.println("N = " + this.node.getID() + " R_OWNED = " + token.getResourceID() + " QUEUE_NOT EMPTY");
                TokenRequest headTokenRequest = token.seeHeadTokenRequestQueue();

                if (this.state == State.WAIT_S) {
                    headTokenRequest = token.nextTokenRequest();

                    System.out.println("N = " + this.node.getID() + " SEND FROM R  = " + token.getResourceID() + " PEND_REQ_T = " + headTokenRequest);

                    this.sendToken(headTokenRequest.getResourceID(), headTokenRequest.getSender());
                } else if (this.state == State.WAIT_CS) {
                    if (this.compareRequest(headTokenRequest, this.currentRequestingCS.getMyRequestMark())) {
                        headTokenRequest = token.nextTokenRequest();

                        System.out.println("N = " + this.node.getID() + " SEND T / R = " + token.getResourceID());

                        this.sendToken(headTokenRequest.getResourceID(), headTokenRequest.getSender());
                    }
                }
            }
        }


        System.out.println("---------------------------------------------------------------------------------------");
    }

    private void setInCS() {
        this.setState(State.IN_CS);

        // Genère un evenement qui lancera le relachement de la CS.

        System.out.println("N = " + this.node.getID() + " IN_CS / R = " + this.currentRequestingCS.getResourceSet());

        int delay = Util.generateRandom(this.MIN_CS, this.MAX_CS);
        EDSimulator.add(delay, new ReleaseMessage(-1, null, null), this.node, this.myPid);
    }

    /**
     * <p>Envoie un message. (Toutes les infos comme a qui doit etre envoye le message sont dans le message).</p>
     *
     * @param message le message a envoyer
     */
    public void sendMessage(Message message) {
        Transport tr = (Transport) this.node.getProtocol(this.transportPID);

        System.out.println("SEND " + message);

        message.addVisitedNode(this.node);

        tr.send(message.getSender(), message.getReceiver(), message, this.myPid);
    }

    /**
     * <p>Retourne la moyenne du vecteur de compteurs.</p>
     *
     * @return la valeur de la note des requete de jetons courant en se basant sur le vecteur de compteur.
     * @see AlgoJL#counterVector
     */
    public double computeMark() {
        double sum = 0.0;

        for (long counter : this.counterVector) {
            sum += (double) counter;
        }

        return sum / ((double) this.counterVector.length);
    }

    /**
     * <p>Effectue le traitement a faire lorsqu'on a recu tous les coutner.</p>
     */
    public void receivedAllCounter() {
        System.out.println("N = " + this.node.getID() + " " + this.getState() + "->  WAIT_CS");
        this.setState(State.WAIT_CS);

        double mark = this.computeMark();
        this.currentRequestingCS.setMyRequestMark(mark);

        System.out.println("N = " + this.node.getID() + " Current_Mark = " + this.currentRequestingCS.getMyRequestMark() + " mark = " + mark);

        if (this.currentRequestingCS.allTokenAreReceived()) {
            this.setInCS();
        } else {
            for (int resourceID : this.currentRequestingCS.getResourceSet()) {
                if (!this.arrayToken[resourceID].isHere()) {
                    TokenRequest tokenRequest = new TokenRequest(mark, resourceID, this.requestID, this.node, this.dynamicTree[resourceID]);

                    System.out.println("N = " + this.node.getID() + " SEND REQ_T / R = " + resourceID + ":");

                    this.sendMessage(tokenRequest);
                }
            }
        }
    }

    /**
     * @param request la requete a verifier
     * @return true si la requete est obselete sinon false.
     */
    public boolean isObsoletedRequest(Request request) {
        if (request instanceof CounterRequest) {
            return this.arrayToken[request.getResourceID()].getLastReqC(request.getSender().getID()) >= request.getRequestID();
        } else if (request instanceof TokenRequest) {
            return this.arrayToken[request.getResourceID()].getLastCS(request.getSender().getID()) >= request.getRequestID();
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
    public boolean compareRequest(TokenRequest tokenRequestReceived, double myRequestMark) {
        return tokenRequestReceived.getMark() < myRequestMark || ((tokenRequestReceived.getMark() == myRequestMark) && tokenRequestReceived.getSender().getID() < this.getNode().getID());
    }

    /**
     * @return un set de tous les jetons present sur le noeud.
     */
    public Token[] getOwnedToken() {
        /*Set<Token> setToken = new TreeSet<>();

        for (Token token : this.arrayToken) {
            if (token.isHere())
                setToken.add(token);
        }*/

        int count = 0;

        for (Token token : this.arrayToken) {
            if (token.isHere())
                count++;
        }

        Token[] tokenArray = new Token[count];

        for (int i = 0, j = 0; i < this.arrayToken.length; i++) {
            if (this.arrayToken[i].isHere()) {
                tokenArray[j] = this.arrayToken[i];
                j++;
            }
        }

        return tokenArray;
    }

    @Override
    public void processEvent(Node node, int i, Object o) {
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
    public void setCounter(int resourceID, long counter) {
        this.counterVector[resourceID] = counter;
    }

    /**
     * @param resourceID l'ID de la ressource concernee
     * @return le jeton assoicie a la ressource.
     */
    public Token getToken(int resourceID) {
        return this.arrayToken[resourceID];
    }

    /**
     * <p>Met a jour le jeton associe a la ressource en se referenceant sur le jeton reference, entre en parametre.</p>
     *
     * @param resourceID l'ID de la ressource concernee
     * @param tokenRef   le jeton reference sur lequel va se base le jeton local pour se mettre a jour
     */
    public void updateToken(int resourceID, Token tokenRef) {
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
            // IMPORTANT
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

    public Token[] getArrayToken() {
        return this.arrayToken;
    }

    public Node getNodeLink(int resourceID) {
        return this.dynamicTree[resourceID];
    }

    // Public enum.

    public enum State {
        NOTHING, // On ne fait rien.
        WAIT_S, // On attend les compteurs.
        WAIT_CS, // on attend les jetons.
        IN_CS // On est en section critique.
    }

    // Private class.
}
