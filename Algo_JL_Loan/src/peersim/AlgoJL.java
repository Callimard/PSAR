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

import java.util.*;

public class AlgoJL implements EDProtocol {

    // Constants.

    private final int MIN_CS;
    private final int MAX_CS;

    // Variables.

    /**
     * <p>
     * Le protocol de transportPID.
     * </p>
     */
    private final int transportPID;

    /**
     * <p>
     * Nombre de ressource disponnible dans le systeme.
     * </p>
     */
    private final int nbResource;

    /**
     * <p>
     * Le PID du protocole AlgoJL.
     * </p>
     */
    private final int myPid;

    /**
     * <p>
     * Le noeud sur lequel l'instance du protocole est. (Initialisee grace au
     * controler {@link InitJL} qui fait appel a la methode
     * {@link AlgoJL#setNode(Node)}
     * </p>
     */
    private Node node;

    /**
     * <p>
     * Nombre de cs que le noeud doit effectuer avant de s'arreter.
     * </p>
     */
    private int nbCS;

    /**
     * <p>Le nombre maximal de ressource que l'on a le droit de demander.</p>
     */
    private int nbMaxResourceAsked;

    /**
     * <p>
     * Permet de recuperer le la requete CS courante, doit etre incremente à chaque
     * fin de CS.
     * </p>
     */
    private int iteListSetRequestCS = 0;

    /**
     * <p>
     * La liste des set de ressources de chaque demande de CS que le noeud doit
     * effectuer.
     * </p>
     */
    private List<Set<Integer>> listSetRequestCS;

    /**
     * <p>
     * Decris l'etat du noeud.
     * </p>
     * <p>
     * Initialement à {@link State#NOTHING}
     * </p>
     */
    private AlgoJL.State state = State.NOTHING;

    /**
     * <p>
     * Point sur le noeud à qui il faut s'addresser pour acceder à la ressource
     * d'indice i.
     * </p>
     * <p>
     * Si dynamicTree[i] est null alors cela signifie que nous possedons la
     * ressource i.
     * </p>
     */
    private Node[] dynamicTree;

    /**
     * <p>
     * Vecteur de compteur. Tous initialement a 0.
     * </p>
     */
    private long[] counterVector;

    /**
     * <p>
     * Represente les jetons de chaque ressource. Les ressources sont presente ou
     * non sur le site. Pour le savoir il faut appeler la methode
     * {@link AlgoJL#hasToken(int)}.
     * </p>
     */
    private Token[] arrayToken;

    /**
     * <p>
     * Permet de gerer les receptions des compteurs et des ressources.
     * </p>
     * <p>
     * Null si aucune requete n'est en cours.
     * </p>
     */
    private RequestingCS currentRequestingCS = null;

    /**
     * <p>
     * ID de la requete courante.
     * </p>
     */
    private int requestID = 0;

    /**
     * <p>
     * La map des requete pendante, permet de verifier si une requete est obselete
     * ou pas.
     * </p>
     */
    private List<Request> listPendingRequest;

    /**
     * <p>
     * Set de ressource pretee.
     * </p>
     */
    private Set<Integer> lentResources = new TreeSet<>();

    /**
     * <p>
     * Au moins une demande de loan a ete faite.
     * </p>
     */
    private boolean loanAsked = false;

    // Constructors.

    public AlgoJL(String prefix) {
        this(Configuration.getPid(prefix + ".tr"), Configuration.getInt(prefix + ".nb_resource"),
                Configuration.lookupPid(prefix.split("\\.")[prefix.split("\\.").length - 1]),
                Configuration.getInt(prefix + ".nbCS"), Configuration.getInt(prefix + ".nb_max_r_asked"), Configuration.getInt(prefix + ".min_cs"),
                Configuration.getInt(prefix + ".max_cs"));
    }

    /**
     * @param transportPID
     * @param nbResource
     * @param myPid
     * @param nbCS
     * @param min_cs
     * @param max_cs
     */
    private AlgoJL(int transportPID, int nbResource, int myPid, int nbCS, int nbMaxResourceAsked, int min_cs, int max_cs) {
        this.MIN_CS = min_cs;
        this.MAX_CS = max_cs;

        this.myPid = myPid;
        this.transportPID = transportPID;
        this.nbResource = nbResource;

        this.arrayToken = new Token[this.nbResource];

        this.dynamicTree = new Node[this.nbResource];

        System.out.println("Nb ressource = " + this.nbResource);

        this.counterVector = new long[this.nbResource];
        for (int i = 0; i < this.counterVector.length; i++) {
            this.counterVector[i] = 0;
        }

        this.listPendingRequest = new LinkedList<>();

        this.nbCS = nbCS;
        if (!this.isInfinite()) {
            this.listSetRequestCS = new ArrayList<>();
            // On cree les set de resources de chaque Requete CS.
            for (int i = 0; i < this.nbCS; i++) {
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
        }

        this.nbMaxResourceAsked = nbMaxResourceAsked;

        if (!this.isInfinite()) {
            System.out.println("----------------------------------------------------------------------");
            for (int i = 0; i < this.listSetRequestCS.size(); i++) {
                System.out.println(this.listSetRequestCS.get(i));
            }
            System.out.println("----------------------------------------------------------------------");
        }

        BigObserver.BIG_OBSERVER.setNbMaxResourceAsked(this.nbMaxResourceAsked);
    }

    // Methods.

    /**
     * <p>
     * Doit etre lance quand on veut faire une demande de CS. Ne peut-etre lance
     * qu'une fois, il faut ensuite passer en CS puis faire
     * {@link AlgoJL#releaseCS()} pour pouvoir refaire requestCS.
     * </p>
     *
     * @param resources le set de ressources requises pour entrer en CS.
     */
    public void requestCS(Set<Integer> resources) {
        /*System.out.println("N = " + this.node.getID()
                + " ReqCS---------------------------------------------------------------------------------------");*/

        /* BigObserver.BIG_OBSERVER.displayArrayToken(); */

        /*System.out.println("SetResource = " + resources);*/

        if (this.currentRequestingCS == null && this.state == State.NOTHING) {
            this.currentRequestingCS = new RequestingCS(resources, this);
            this.requestID++;

            this.setState(State.WAIT_S);

            if (!this.currentRequestingCS.allTokenAreReceived()) {
                List<Message> buff = new ArrayList<>();
                for (int resourceID : this.currentRequestingCS.getResourceRequiredSet()) {
                    if (!this.hasToken(resourceID)) {
                        Message counterRequest = new CounterRequest(resourceID, this.requestID, this.node,
                                this.dynamicTree[resourceID]);
                        buff.add(counterRequest);
                    } else {
                        /*if (this.dynamicTree[resourceID] != null)*/
                            /*System.out.println(
                                    "N = " + this.node.getID() + " PB -> ON A LE NOEUD MAIS DYNAMIC TREE PAS NULL -> "
                                            + this.dynamicTree[resourceID]);*/
                    }
                }

                this.sendBuff(buff, false);
            } else {
                /*System.out.println("N = " + this.node.getID() + " JETON DEJA TOUS POSSEDES!!");*/
                this.setInCS();
            }
        } else {
            /*if (this.currentRequestingCS != null)*/
                /*System.out.println("N = " + this.node.getID()
                        + "PB -> ATTENTION!!! DEMANDE DE CS ALORS QU'IL Y EN A UNE DEJA EN COURS.");*/

            /*if (this.state != State.NOTHING)*/
                /*System.out.println("N = " + this.node.getID()
                        + "PB -> ATTENTION!!! DEMANDE DE CS ALORS QUE L'ETAT N'EST PAS NOTHING.");*/
        }

        /* BigObserver.BIG_OBSERVER.displayArrayToken(); */

        /*System.out.println("---------------------------------------------------------------------------------------");*/
    }

    /**
     * <p>
     * Relache la CS. Est appelee lorsque le message {@link ReleaseMessage} est
     * recu.
     * </p>
     */
    public void releaseCS() {
        /*System.out.println("N = " + this.node.getID()
                + " RelCS--------------------------------------------------------------------------------------");*/

        /* BigObserver.BIG_OBSERVER.displayArrayToken(); */

        this.setState(State.NOTHING);
        this.loanAsked = false;

        BigObserver.BIG_OBSERVER.releaseCS(this.node.getID());

        /*System.out.println("N = " + this.node.getID() + " currentRequestingCS = " + this.currentRequestingCS);*/
        Set<Integer> resourceRequired = this.currentRequestingCS.getResourceRequiredSet();
        /*System.out.println("N = " + this.node.getID() + " R_CS / R_required = " + resourceRequired);*/

        // Contient que des TokenMessage que l'on cree pour envoyer les jetons.
        List<Message> buff = new ArrayList<>();
        for (int resourceID : resourceRequired) {
            Token token = this.arrayToken[resourceID];
            token.putLastCS(this.node.getID(), this.requestID);

            Node lender = token.getLenderNode();

            if (!token.tokenRequestQueueEmpty() && lender == null) {
                /*System.out.println("N = " + this.node.getID() + " SEND T / R = " + resourceID);*/
                buff.add(this.sendToken(resourceID, token.nextTokenRequest().getSender()));
            } else if (lender != null) {
                token.setLenderNode(null);
                TokenRequest tokenRequest = token.removeTokenRequestFor(lender);
                assert tokenRequest != null;
                buff.add(this.sendToken(token.getResourceID(), lender));
            }
        }

        this.currentRequestingCS = null;

        this.sendBuff(buff, false);

        if (!this.isInfinite()) {
            this.iteListSetRequestCS++;

            if (this.iteListSetRequestCS >= this.nbCS) {
                /*System.out.println("N = " + this.node.getID() + " FIN DU NOEUD!!!!!!!!!!");*/
            } else {
                int delay = Util.generateRandom(this.MIN_CS, this.MAX_CS);
                EDSimulator.add(delay, new BeginMessage(-1, null, null), this.node, this.myPid);
            }
        } else {
            int delay = Util.generateRandom(this.MIN_CS, this.MAX_CS);
            EDSimulator.add(delay, new BeginMessage(-1, null, null), this.node, this.myPid);
        }

        /* BigObserver.BIG_OBSERVER.displayArrayToken(); */

        /*System.out.println("---------------------------------------------------------------------------------------");*/
    }

    private void receiveCounterRequest(CounterRequest counterRequest) {
        int resourceID = counterRequest.getResourceID();
        int requestID = counterRequest.getRequestID();
        Node sender = counterRequest.getSender();

        List<Message> buff = new ArrayList<>();
        List<Message> buffTrue = new ArrayList<>();

        /*System.out.println("N = " + this.node.getID()
                + " RcvREQ_C--------------------------------------------------------------------------------------- Sender = "
                + sender.getID() + " State = " + this.getState());*/

        /* BigObserver.BIG_OBSERVER.displayArrayToken(); */

        /*System.out.println("N = " + this.node.getID() + " RECV " + counterRequest);*/

        if (this.listPendingRequest.contains(counterRequest) || counterRequest.isVisitedNode(this.node)
                || (this.hasToken(resourceID)
                && this.arrayToken[resourceID].getLastReqC(counterRequest.getSender().getID()) >= requestID)) {
            /*System.out.println("Contains = " + this.listPendingRequest.contains(counterRequest));
            System.out.println("Visited Node = " + counterRequest.isVisitedNode(this.node));
            System.out.println(
                    "(this.hasToken(resourceID) && this.arrayToken[resourceID].getLastReqC(counterRequest.getSender().getID()) >= requestID) = "
                            + (this.hasToken(resourceID) && this.arrayToken[resourceID]
                            .getLastReqC(counterRequest.getSender().getID()) >= requestID));
            System.out.println("REQUETE COUNTER OBSELETE!!! Req = " + counterRequest);
            System.out
                    .println("---------------------------------------------------------------------------------------");*/
            return;
        }

        assert ((sender.getID() != this.node.getID())
                || ((sender.getID() == this.node.getID()) && !counterRequest.isVisitedNode(this.node))) : "Sender = "
                + sender.getID() + " N = " + this.getNode().getID() + " VisitedNode = "
                + counterRequest.getVisitedNode() + " Message envoye a nous meme.";

        if (this.hasToken(resourceID)) {
            if (this.getState() == State.NOTHING
                    || (this.currentRequestingCS != null && !this.currentRequestingCS.isTokenNeeded(resourceID))) { // Si on a pas besoin de ce token.
                /*System.out.println("N = " + this.node.getID() + " SEND T / R = " + resourceID);*/
                buff.add(this.sendToken(resourceID, sender));
            } else {
               /* System.out.println("N = " + this.node.getID() + " SEND C / R = " + resourceID);*/
                this.arrayToken[resourceID].putLastReqC(sender.getID(), requestID);
                buff.add(new CounterMessage(this.arrayToken[resourceID].incrementCounter(), resourceID, this.node,
                        sender));
            }
        } else {
            /*System.out.println("N = " + this.node.getID() + " ROUT / Counter R = " + resourceID + " dynamicTree["
                    + resourceID + "] = " + this.dynamicTree[resourceID].getID());*/
            this.listPendingRequest.add(counterRequest);
            counterRequest.setReceiver(this.dynamicTree[resourceID]);
            buffTrue.add(counterRequest);
        }

        this.sendBuff(buff, false);
        this.sendBuff(buffTrue, true);

        /* BigObserver.BIG_OBSERVER.displayArrayToken(); */

        /*System.out.println("---------------------------------------------------------------------------------------");*/
    }

    private void receiveTokenRequest(TokenRequest tokenRequest) {
        int resourceID = tokenRequest.getResourceID();
        int requestID = tokenRequest.getRequestID();
        Node sender = tokenRequest.getSender();

        List<Message> buff = new ArrayList<>();
        List<Message> buffTrue = new ArrayList<>();

        /*System.out.println("N = " + this.node.getID()
                + " RcvREQ_T--------------------------------------------------------------------------------------- Sender = "
                + sender.getID() + " State = " + this.getState());*/

        /* BigObserver.BIG_OBSERVER.displayArrayToken(); */

        /*System.out.println("N = " + this.node.getID() + " RECV " + tokenRequest);*/

        if (this.listPendingRequest.contains(tokenRequest) || tokenRequest.isVisitedNode(this.node)
                || (this.hasToken(resourceID)
                && this.arrayToken[resourceID].getLastCS(tokenRequest.getSender().getID()) >= requestID)) {
            /*System.out.println("Contains = " + this.listPendingRequest.contains(tokenRequest));
            System.out.println("Visited Node = " + tokenRequest.isVisitedNode(this.node));
            System.out.println(
                    "(this.hasToken(resourceID) && this.arrayToken[resourceID].getLastReqC(counterRequest.getSender().getID()) >= requestID) = "
                            + (this.hasToken(resourceID) && this.arrayToken[resourceID]
                            .getLastReqC(tokenRequest.getSender().getID()) >= requestID));
            System.out.println("REQUETE TOKEN OBSELETE!!! Req = " + tokenRequest);
            System.out
                    .println("---------------------------------------------------------------------------------------");*/
            return;
        }

        assert ((sender.getID() != this.node.getID())
                || ((sender.getID() == this.node.getID()) && !tokenRequest.isVisitedNode(this.node))) : "Sender = "
                + sender.getID() + " N = " + this.getNode().getID() + " VisitedNode = "
                + tokenRequest.getVisitedNode() + " Message envoye a nous meme.";

        if (this.hasToken(resourceID)) {
            if (this.getState() == State.WAIT_S || this.getState() == State.NOTHING
                    || (this.currentRequestingCS != null && !this.currentRequestingCS.isTokenNeeded(resourceID))) {
                /*System.out.println("N = " + this.node.getID() + " SEND T / R = " + resourceID);*/
                buff.add(this.sendToken(resourceID, sender));
            } else {
                if (!this.arrayToken[resourceID].contains(tokenRequest)) {
                    if (this.getState() == State.WAIT_CS
                            && (this.compareRequest(tokenRequest, this.currentRequestingCS.getMyRequestMark()))) {
                        /*System.out.println("N = " + this.node.getID() + " SEND T / R = " + resourceID);*/
                        this.arrayToken[resourceID]
                                .addTokenRequest(this.currentRequestingCS.getTokenRequestSend(resourceID));
                        buff.add(this.sendToken(resourceID, sender));
                    } else {
                        if (sender.getID() != this.node.getID()) {
                            /*System.out.println("N = " + this.node.getID() + " ICI state -> " + this.getState());*/
                            this.arrayToken[resourceID].addTokenRequest(tokenRequest);
                        }
                    }
                }
            }
        } else {
            /*System.out.println("N = " + this.node.getID() + " ROUT / Token R = " + resourceID + " dynamicTree["
                    + resourceID + "] = " + this.dynamicTree[resourceID].getID());*/
            this.listPendingRequest.add(tokenRequest);
            tokenRequest.setReceiver(this.dynamicTree[resourceID]);
            buffTrue.add(tokenRequest);
        }

        this.sendBuff(buff, false);
        this.sendBuff(buffTrue, true);

        /* BigObserver.BIG_OBSERVER.displayArrayToken(); */

        /*System.out.println("---------------------------------------------------------------------------------------");*/
    }

    private void receiveLoanRequest(LoanRequest loanRequest) {
        int resourceID = loanRequest.getResourceID();
        int requestID = loanRequest.getRequestID();
        Node sender = loanRequest.getSender();

        List<Message> buff = new ArrayList<>();
        List<Message> buffTrue = new ArrayList<>();

        /*System.out.println("N = " + this.node.getID() + " REQUETE LOAN -> " + loanRequest + " resourceMissing -> " + loanRequest.getMissingResource());*/

        /*System.out.println("N = " + this.node.getID()
                + " RcvREQ_L--------------------------------------------------------------------------------------- Sender = "
                + sender.getID() + " State = " + this.getState());*/

        if (this.listPendingRequest.contains(loanRequest) || loanRequest.isVisitedNode(this.node) || this.isObsoletedRequest(loanRequest)) {
            /*System.out.println("Contains = " + this.listPendingRequest.contains(loanRequest));
            System.out.println("Visited Node = " + loanRequest.isVisitedNode(this.node));
            System.out.println("isObsoletedRequest = " + this.isObsoletedRequest(loanRequest));
            System.out.println("REQUETE TOKEN OBSELETE!!! Req = " + loanRequest);
            System.out
                    .println("---------------------------------------------------------------------------------------");*/
            return;
        }

        assert ((sender.getID() != this.node.getID())
                || ((sender.getID() == this.node.getID()) && !loanRequest.isVisitedNode(this.node))) : "Sender = "
                + sender.getID() + " N = " + this.getNode().getID() + " VisitedNode = "
                + loanRequest.getVisitedNode() + " Message envoye a nous meme.";

        if (this.hasToken(resourceID)) {
//        	System.out.println("Je suis la");
            this.processRequestLoan(loanRequest, buff);
        } else {
//            System.out.println("N = " + this.node.getID() + " ROUT / Loan R = " + resourceID + " dynamicTree["
//                    + resourceID + "] = " + this.dynamicTree[resourceID].getID());
        	
            this.listPendingRequest.add(loanRequest);
            loanRequest.setReceiver(this.dynamicTree[resourceID]);
            buffTrue.add(loanRequest);
        }

        this.sendBuff(buff, false);
        this.sendBuff(buffTrue, true);

        /*System.out.println("---------------------------------------------------------------------------------------");*/
    }

    private void receiveCounter(CounterMessage counterMessage) {
        int resourceID = counterMessage.getResourceID();
        Node sender = counterMessage.getSender();

        /*System.out.println("N = " + this.node.getID()
                + " RcvC--------------------------------------------------------------------------------------- Sender = "
                + sender.getID() + " State = " + this.getState());
        System.out.println("R = " + resourceID + " counter = " + counterMessage.getCounter());*/

        /* BigObserver.BIG_OBSERVER.displayArrayToken(); */

        assert ((sender.getID() != this.node.getID())
                || ((sender.getID() == this.node.getID()) && !counterMessage.isVisitedNode(this.node))) : "Sender = "
                + sender.getID() + " N = " + this.getNode().getID() + " VisitedNode = "
                + counterMessage.getVisitedNode() + " Message envoye a nous meme.";

        this.currentRequestingCS.receiveCounter(counterMessage);

        if (this.currentRequestingCS.allCounterAreReceived()) {
            this.processCounterNeededEmpty();
        }

        /* BigObserver.BIG_OBSERVER.displayArrayToken(); */

        /*System.out.println("---------------------------------------------------------------------------------------");*/
    }

    private void receiveToken(TokenMessage tokenMessage) {
        Node sender = tokenMessage.getSender();

        List<Message> buff = new ArrayList<>();

        /*System.out.println("N = " + this.node.getID()
                + " RcvT--------------------------------------------------------------------------------------- Sender = "
                + sender.getID() + " State = " + this.getState());

        System.out.println("N = " + this.node.getID() + " RecvT = " + tokenMessage.getToken().getResourceID());
*/
        /* BigObserver.BIG_OBSERVER.displayArrayToken(); */

        assert ((sender.getID() != this.node.getID())
                || ((sender.getID() == this.node.getID()) && !tokenMessage.isVisitedNode(this.node))) : "Sender = "
                + sender.getID() + " N = " + this.getNode().getID() + " VisitedNode = "
                + tokenMessage.getVisitedNode() + " Message envoye a nous meme.";

        this.processUpdate(tokenMessage, buff);

        if (this.currentRequestingCS != null && this.currentRequestingCS.allTokenAreReceived()) {

            for (int resource : this.currentRequestingCS.getResourceRequiredSet()) {
                if (this.arrayToken[resource].getLenderNode() != null) {
                    BigObserver.BIG_OBSERVER.loanSuccess(this.node.getID());
                    break;
                }
            }

            this.setInCS();
        } else {

            List<Integer> listOwned = this.getListOwnedToken();
            for (int resource : listOwned) {
                if (this.arrayToken[resource].getLenderNode() != null) {
                    buff.add(this.sendToken(resource, this.arrayToken[resource].getLenderNode()));
                    this.loanAsked = false;
                }
            }

            if (this.getState() == State.WAIT_S && this.currentRequestingCS.allCounterAreReceived()) {
                this.processCounterNeededEmpty();
            }

            Token[] owned = this.getOwnedToken();
            for (Token token : owned) {
                if (!token.tokenRequestQueueEmpty()) {
                    TokenRequest headRequest = token.seeHeadTokenRequestQueue();

                    if (this.getState() == State.WAIT_S) {
                        TokenRequest tmp = headRequest;
                        headRequest = token.nextTokenRequest();
                        assert (tmp == headRequest);

                        /*System.out.println("N = " + this.node.getID() + " SEND T / R = " + headRequest.getResourceID());*/
                        buff.add(this.sendToken(headRequest.getResourceID(), headRequest.getSender()));
                    } else if (this.getState() == State.WAIT_CS) {
                        TokenRequest myTokenRequest = this.currentRequestingCS
                                .getTokenRequestSend(headRequest.getResourceID());

                        if (this.compareRequest(headRequest, this.currentRequestingCS.getMyRequestMark())) {
                            TokenRequest tmp = headRequest;
                            headRequest = token.nextTokenRequest();
                            assert (tmp == headRequest);

                            this.arrayToken[headRequest.getResourceID()].addTokenRequest(myTokenRequest);
                            buff.add(this.sendToken(headRequest.getResourceID(), headRequest.getSender()));
                        }
                    } else {
                        /*System.out.println(
                                "N = " + this.node.getID() + "PB -> TOTALEMENT IMPOSSIBLE!!! state = " + this.state);*/
                    }
                }
            }

            listOwned = this.getListOwnedToken();
            for (int resource : listOwned) {
                if (!this.arrayToken[resource].loanRequestQueueEmpty()) {
                    List<LoanRequest> copyLoanRequest = this.arrayToken[resource].copyLoanRequestQueue();
                    this.arrayToken[resource].clearLoanRequestQueue();
                    for (LoanRequest loanRequest : copyLoanRequest) {
                        if (this.hasToken(loanRequest.getResourceID()))
                            this.processRequestLoan(loanRequest, buff);
                    }
                }
            }
        }

        if (/*this.currentRequestingCS.getMissingResource().size() == this.givenThreshold &&*/ this.state == State.WAIT_CS && !this.loanAsked) {
            this.loanAsked = true;
            for (int resourceMissing : this.currentRequestingCS.getMissingResource()) {
                buff.add(new LoanRequest(this.currentRequestingCS.getMyRequestMark(), this.currentRequestingCS.getMissingResource(), resourceMissing, this.requestID, this.node, this.dynamicTree[resourceMissing]));
            }
        }

        this.sendBuff(buff, false);

        /* BigObserver.BIG_OBSERVER.displayArrayToken(); */

        /*System.out.println("---------------------------------------------------------------------------------------");*/
    }

    private void setInCS() {
        /* BigObserver.BIG_OBSERVER.displayArrayToken(); */

        this.setState(State.IN_CS);
        /*System.out.println(
                "N = " + this.node.getID() + " SET_IN_CS / R = " + this.currentRequestingCS.getResourceRequiredSet());*/

        BigObserver.BIG_OBSERVER.setInCS(this.node.getID(), this.currentRequestingCS.getResourceRequiredSet());

        // Genère un evenement qui lancera le relachement de la CS.
        int delay = Util.generateRandom(this.MIN_CS, this.MAX_CS);
        EDSimulator.add(delay, new ReleaseMessage(-1, null, null), this.node, this.myPid);
    }

    private void sendBuff(List<Message> buff, boolean addInVisitedNode) {
        for (Message message : buff) {
            this.sendMessage(message, addInVisitedNode);
        }
    }

    public Message sendToken(int resourceID, Node receiver) {
        Message tokenMessage = new TokenMessage(this.arrayToken[resourceID], resourceID, this.node, receiver);

        if (this.currentRequestingCS != null) {
            this.currentRequestingCS.removeTokenReceived(resourceID);
        }

        this.setNodeLink(resourceID, receiver);
        this.arrayToken[resourceID] = null;

        /* BigObserver.BIG_OBSERVER.displayArrayToken(); */

        return tokenMessage;
    }

    /**
     * <p>
     * Envoie un message. (Toutes les infos comme a qui doit etre envoye le message
     * sont dans le message).
     * </p>
     *
     * @param message le message a envoyer
     */
    public void sendMessage(Message message, boolean addInVisitedNode) {
        Transport tr = (Transport) this.node.getProtocol(this.transportPID);

        /*System.out.println("N = " + this.node.getID() + " SEND " + message);*/

        if (addInVisitedNode)
            message.addVisitedNode(this.node);

        BigObserver.BIG_OBSERVER.messageSend();
        
        tr.send(message.getSender(), message.getReceiver(), message, this.myPid);
    }

    /**
     * <p>
     * Retourne la moyenne du vecteur de compteurs.
     * </p>
     *
     * @return la valeur de la note des requete de jetons courant en se basant sur
     * le vecteur de compteur.
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
     * @param tokenRequestReceived la requete que l'on a recue et que l'on va
     *                             comparer a notre note
     * @param myRequestMark        la note de nos requete de jeton courantes.
     * @return true si la requete de jeton recue est plus prioritaire que la requete
     * que nous avons envoye pour la demande de CS courante, sinon false.
     */
    public boolean compareRequest(TokenRequest tokenRequestReceived, double myRequestMark) {

        return tokenRequestReceived.getMark() < myRequestMark || ((tokenRequestReceived.getMark() == myRequestMark)
                && tokenRequestReceived.getSender().getID() < this.getNode().getID());
    }

    /**
     * @param loanRequestReceived la requete que l'on a recue et que l'on va
     *                            comparer a notre note
     * @param myRequestMark       la note de nos requete de jeton courantes.
     * @return true si la requete de jeton recue est plus prioritaire que la requete
     * que nous avons envoye pour la demande de CS courante, sinon false.
     */
    public boolean compareRequest(LoanRequest loanRequestReceived, double myRequestMark) {

        return loanRequestReceived.getMark() < myRequestMark || ((loanRequestReceived.getMark() == myRequestMark)
                && loanRequestReceived.getSender().getID() < this.getNode().getID());
    }

    private void processUpdate(TokenMessage tokenMessage, List<Message> buff) {

        /*assert this.currentRequestingCS != null : "Sender = " + tokenMessage.getSender().getID() + " N = "
                + this.getNode().getID() + " Reception de T = " + tokenMessage.getResourceID() + " Sans demande de CS";*/

        if (this.currentRequestingCS != null) {
            this.currentRequestingCS.receiveToken(tokenMessage.getToken());
        } else {
            /*System.out.println("N = " + this.node.getID() + " Reception Token alors qu'on demande pas de CS.");*/
            this.tokenArrived(tokenMessage.getToken());
            this.setNodeLink(tokenMessage.getToken().getResourceID(), null);
        }

        // Remove si elle est dedans.
        this.lentResources.remove(tokenMessage.getResourceID());

        for (Request request : this.listPendingRequest) {
            if (request.getResourceID() == tokenMessage.getResourceID()) {
                if (this.isObsoletedRequest(request)) {
                    /*System.out.println("REQUETE OBSELETE!!! Req = " + request);*/
                    continue;
                }

                if (request instanceof CounterRequest) {
                    this.arrayToken[tokenMessage.getResourceID()].putLastReqC(request.getSender().getID(),
                            request.getRequestID());
                    /*System.out.println("N = " + this.node.getID() + " SEND C / R = " + tokenMessage.getResourceID());*/
                    buff.add(new CounterMessage(this.arrayToken[tokenMessage.getResourceID()].incrementCounter(),
                            tokenMessage.getResourceID(), this.node, request.getSender()));
                } else if (request instanceof TokenRequest) {
                    if (request.getSender().getID() != this.node.getID()) {
                        if (!this.arrayToken[tokenMessage.getResourceID()].contains((TokenRequest) request)) {
                            this.arrayToken[tokenMessage.getResourceID()].addTokenRequest((TokenRequest) request);
                        }
                    }
                } else {
                    if (!this.arrayToken[request.getResourceID()].contains((LoanRequest) request)) {
                        this.arrayToken[request.getResourceID()].addLoanRequest((LoanRequest) request);
                    }
                }
            }
        }

    }

    private void processCounterNeededEmpty() {
        assert (this.state == State.WAIT_S && this.currentRequestingCS.allCounterAreReceived());

        List<Message> buff = new ArrayList<>();

        /*System.out.println("N = " + this.node.getID() + " " + this.getState() + " -> WAIT_CS ALL COUNTER RECEIVED");*/
        this.setState(State.WAIT_CS);

        double mark = this.computeMark();
        this.currentRequestingCS.setMyRequestMark(mark);

        /*System.out.println("N = " + this.node.getID() + " Current_Mark = " + this.currentRequestingCS.getMyRequestMark()
                + " mark = " + mark);*/

        for (int resourceID : this.currentRequestingCS.getResourceRequiredSet()) {
            TokenRequest tokenRequest = new TokenRequest(mark, resourceID, this.requestID, this.node,
                    this.dynamicTree[resourceID]);
            this.currentRequestingCS.addTokenRequestSend(tokenRequest);

            if (!this.hasToken(resourceID)) {
                /*System.out.println("N = " + this.node.getID() + " SEND REQ_T / R = " + resourceID + ":");*/
                buff.add(tokenRequest);
            }
        }

        this.sendBuff(buff, false);
    }

    private void processRequestLoan(LoanRequest loanRequest, List<Message> buff) {
        int resourceID = loanRequest.getResourceID();
        Node sender = loanRequest.getSender();
        
        if (!this.isObsoletedRequest(loanRequest)) {
            if (this.canLend(loanRequest)) {
//            	System.out.println("DAMN I LENT");
                this.lentResources.addAll(loanRequest.getMissingResource());
                for (int resource : this.lentResources) {
                    this.arrayToken[resource].setLenderNode(this.getNode());
                    TokenRequest tokenRequest = this.arrayToken[resource].removeTokenRequestFor(sender);
                    if (tokenRequest == null) {
                       /* System.out.println("N = " + this.node.getID() + " TokenRequest null Sender = " + sender.getID() + " R = " + resource);*/
                    }

                   /* System.out.println("N = " + this.node.getID() + " SEND T / R = " + resource);*/
                    buff.add(this.sendToken(resource, sender));
                }
            } else {
//            	System.out.println("RAIiiiiiiii!!");
//            	System.out.println("this.getState() == State.NOTHING = " + (this.getState() == State.NOTHING));
//            	System.out.println("this.getState() == State.WAIT_S = " + (this.getState() == State.WAIT_S));
//            	System.out.println("(this.currentRequestingCS != null && !this.currentRequestingCS.isTokenNeeded(resourceID)) = " + (this.currentRequestingCS != null && !this.currentRequestingCS.isTokenNeeded(resourceID)));
                if (this.getState() == State.NOTHING || this.getState() == State.WAIT_S || (this.currentRequestingCS != null && !this.currentRequestingCS.isTokenNeeded(resourceID))) {
//                	System.out.println("Yeessss!!");
                	buff.add(this.sendToken(resourceID, sender));
                } else if (!this.arrayToken[resourceID].contains(loanRequest)) {
//                	System.out.println("Noooononononooon!!");
                	this.arrayToken[resourceID].addLoanRequest(loanRequest);
                }
            }
        } else {
//        	System.out.println("Ouououououuouou!!");
        }
    }

    /**
     * @param request la requete a verifier
     * @return true si la requete est obselete sinon false.
     */
    public boolean isObsoletedRequest(Request request) {
        if (request instanceof CounterRequest) {
            return this.arrayToken[request.getResourceID()].getLastReqC(request.getSender().getID()) >= request
                    .getRequestID();
        } else if (request instanceof TokenRequest) { // (request instanceof TokenRequest)
            return this.arrayToken[request.getResourceID()].getLastCS(request.getSender().getID()) >= request
                    .getRequestID();
        } else {
            return request.getSender().getID() == this.node.getID(); /*this.arrayToken[request.getResourceID()].contains((LoanRequest) request);*/ // Condition a verifier.
        }
    }

    /**
     * @return un set de tous les jetons present sur le noeud.
     */
    private Token[] getOwnedToken() {
        int count = 0;

        for (int i = 0; i < this.arrayToken.length; i++) {
            if (this.arrayToken[i] != null)
                count++;
        }

        Token[] tokenArray = new Token[count];

        for (int i = 0, j = 0; i < this.arrayToken.length; i++) {
            if (this.arrayToken[i] != null) {
                tokenArray[j] = this.arrayToken[i];
                j++;
            }
        }

        return tokenArray;
    }

    private List<Integer> getListOwnedToken() {
        List<Integer> owned = new LinkedList<>();
        for (int i = 0; i < this.arrayToken.length; i++) {
            if (this.arrayToken[i] != null)
                owned.add(i);
        }
        return owned;
    }

    /**
     * Fonction permettant de savoir si un noeud peut preter des ressources. Cela
     * n'est possible que lorsque le noeud possède toutes les ressources demandees,
     * qu'il n'en a aucune deja pretee, et qu'il n'est pas en section critique.
     *
     * @param loanRequest la requete contenant les ressources demandees
     * @return <code>true</code> si le noeud peut preter des ressources,
     * <code>false</code> sinon
     */
    public boolean canLend(LoanRequest loanRequest) {
        List<Integer> ownedResources = this.getListOwnedToken();
        Set<Integer> missingResources = loanRequest.getMissingResource();
        boolean lentResources = false;
        int index;

        /* boucle verifiant si une ressource a deja ete pretee */
        for (int i = 0; i < ownedResources.size(); i++) {
            index = ownedResources.get(i);
            if (arrayToken[index].getLenderNode() == null) {
                lentResources = true;
                break; // Plus aucun interet de chercher, on ne pretera pas.
            }
        }

        if (ownedResources.containsAll(missingResources) && !lentResources && this.lentResources.isEmpty()
                && this.getState() != State.IN_CS) {
            if (this.getState() == State.WAIT_CS) {
                return !this.loanAsked || this.compareRequest(loanRequest, this.currentRequestingCS.getMyRequestMark());
            } else {
                return true;
            }
        } else {
            return false;
        }
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
                Set<Integer> setResource = null;
                if (!this.isInfinite()) {
                    setResource = this.listSetRequestCS.get(this.iteListSetRequestCS);
                } else {
                    setResource = new TreeSet<>();
                    int nbRes = Util.generateRandom(1, this.nbMaxResourceAsked + 1);
                    int j = 0;
                    while (j < nbRes) {
                        int generate = CommonState.r.nextInt(this.nbResource);

                        if (setResource.add(generate)) {
                            j++;
                        }
                    }
                }
                this.requestCS(setResource);
            } else if (o instanceof LoanRequest) {
                this.receiveLoanRequest((LoanRequest) o);
            } else {
                throw new RuntimeException("Mauvais event");
            }
        } else {
            throw new RuntimeException("Mauvais ID");
        }
    }

    @Override
    public Object clone() {
        return new AlgoJL(this.transportPID, this.nbResource, this.myPid, this.nbCS, this.nbMaxResourceAsked, this.MIN_CS, this.MAX_CS);
    }

    /**
     * <p>
     * Met la valeur de {@link AlgoJL#node} a node. Si la valeur a deja ete
     * initialise, aucun changement n'est fait.
     * </p>
     *
     * @param node
     */
    public void setNode(Node node) {
        if (this.node == null) {
            // IMPORTANT
            this.node = node;

            BigObserver.BIG_OBSERVER.addAlgoJL(this);
        }
    }

    /**
     * <p>
     * Permet de faire pointer notre noeud sur le noeud link pour la ressource
     * precisee en parametres.
     * </p>
     * <p>
     * <strong>ATTENTION!</strong> Le jeton contenue dans {@link AlgoJL#arrayToken}
     * et associe a la ressource n'est pas mis a jour avec cette methode.
     * </p>
     *
     * @param resourceID l'ID de la ressource concernee
     * @param link       le nouveau noeud lien pour cette ressour (peut etre null)
     */
    public void setNodeLink(int resourceID, Node link) {
        assert (link != this.node) : "Link = " + link.getID() + " N = " + this.node.getID();

        this.dynamicTree[resourceID] = link;
    }

    /**
     * <p>
     * Transforme ceux noeud en noeud initiale c'est a dire le noeud qui possède
     * tous les jetons au debut.
     * </p>
     * <p>
     * Ne doit etre appele que sur un noeud logiquequement.
     * </p>
     */
    public void setInitialNode() {
        for (int i = 0; i < this.arrayToken.length; i++) {
            this.arrayToken[i] = new Token(i);
        }
    }

    /**
     * <p>
     * Methode a appeler lorsqu'on recoit un jeton, elle permet de stocker le jeton
     * dans le tableau des jetons present sur le site.
     * </p>
     * <p>
     * Aucun traitement en rapport avec les CS n'est fait, seul l'ajout dans le
     * tableau des jetons present sur le noeud est fait.
     * </p>
     *
     * @param token - le jeton qui vient d'arriver sur le noeud.
     */
    public void tokenArrived(Token token) {
        if (this.arrayToken[token.getResourceID()] == null) {
            this.arrayToken[token.getResourceID()] = token;
        } else {
            /*System.out.println("N = " + this.node.getID() + " PB -> RECEPTION D'UN JETON DEJA PRESENT!!!!");*/
        }
    }

    /**
     * @param resourceID - la ressource pour laquelle on veut savoir si le jeton ets
     *                   present ou pas.
     * @return true si le jeton est sur ce noeud sinon false.
     */
    public boolean hasToken(int resourceID) {
        return this.arrayToken[resourceID] != null;
    }

    // Getters and Setters.

    public Node getNode() {
        return this.node;
    }

    public int getNbResource() {
        return this.nbResource;
    }

    public void setCounter(int resourceID, long counter) {
        this.counterVector[resourceID] = counter;
    }

    public Token getToken(int resourceID) {
        return this.arrayToken[resourceID];
    }

    public Token[] getArrayToken() {
        return this.arrayToken;
    }

    public State getState() {
        return this.state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public int getRequestID() {
        return this.requestID;
    }

    public boolean isLoanAsked() {
        return loanAsked;
    }

    public void setLoanAsked(boolean loanAsked) {
        this.loanAsked = loanAsked;
    }

    public Set<Integer> getLentResources() {
        return lentResources;
    }

    public boolean isInfinite() {
        return this.nbCS <= 0;
    }

    // Public enum.

    public enum State {
        NOTHING, // On ne fait rien.
        WAIT_S, // On attend les compteurs.
        WAIT_CS, // on attend les jetons.
        IN_CS // On est en section critique.
    }
}
