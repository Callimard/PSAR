package peersim;

import common.message.*;
import common.util.Token;
import common.util.Util;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;

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
     * <p>Nombre de cs que le noeud doit effectuer avant de s'arreter.</p>
     */
    private int nbCS;

    /**
     * <p>Permet de recuperer le la requete CS courante, doit etre incremente à chaque fin de CS.</p>
     */
    private int iteListSetRequestCS = 0;

    /**
     * <p>La liste des set de ressources de chaque demande de CS que le noeud doit effectuer.</p>
     */
    private List<Set<Integer>> listSetRequestCS;

    /**
     * <p>Decris l'etat du noeud.</p>
     * <p>Initialement à {@link State#NOTHING}</p>
     */
    private AlgoJL.State state = State.NOTHING;

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
     * <p>Represente les jetons de chaque ressource. Les ressources sont presente ou non sur le site. Pour le savoir il faut appeler la methode {@link Token#isHere()}.</p>
     */
    private Token[] arrayToken;

    /**
     * <p>Permet de gerer les receptions des compteurs et des ressources.</p>
     * <p>Null si aucune requete n'est en cours.</p>
     */
    private RequestingCS currentRequestingCS = null;

    /**
     * <p>ID de la requete courante.</p>
     */
    private int requestID = 0;

    /**
     * <p>La map des requete pendante, permet de verifier si une requete est obselete ou pas.</p>
     */
    private List<Request> listPendingRequest;

    // Constructors.

    public AlgoJL(String prefix) {
        this(Configuration.getPid(prefix + ".tr"), Configuration.getInt(prefix + ".nb_resource"),
                Configuration.lookupPid(prefix.split("\\.")[prefix.split("\\.").length - 1]),
                Configuration.getInt(prefix + ".nbCS"),
                Configuration.getInt(prefix + ".min_cs"),
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
    private AlgoJL(int transportPID, int nbResource, int myPid, int nbCS, int min_cs, int max_cs) {
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
        this.listSetRequestCS = new ArrayList<>();

        this.nbCS = nbCS;
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

        System.out.println("----------------------------------------------------------------------");

        for (int i = 0; i < this.listSetRequestCS.size(); i++) {
            System.out.println(this.listSetRequestCS.get(i));
        }

        System.out.println("----------------------------------------------------------------------");
    }

    // Methods.

    /**
     * <p>Doit etre lance quand on veut faire une demande de CS. Ne peut-etre lance qu'une fois, il faut ensuite passer en CS puis faire {@link AlgoJL#releaseCS()} pour pouvoir refaire requestCS.</p>
     *
     * @param resources le set de ressources requises pour entrer en CS.
     */
    public void requestCS(Set<Integer> resources) {
        System.out.println("ReqCS---------------------------------------------------------------------------------------");
        System.out.println("---------------------------------------------------------------------------------------");
    }

    /**
     * <p>Relache la CS. Est appelee lorsque le message {@link ReleaseMessage} est recu.</p>
     */
    public void releaseCS() {
        System.out.println("RelCS--------------------------------------------------------------------------------------");
        System.out.println("---------------------------------------------------------------------------------------");
    }

    private void receiveCounterRequest(CounterRequest counterRequest) {
        int resourceID = counterRequest.getResourceID();
        int requestID = counterRequest.getRequestID();
        Node sender = counterRequest.getSender();
        System.out.println("RcvREQ_C---------------------------------------------------------------------------------------");
        System.out.println("---------------------------------------------------------------------------------------");
    }

    private void receiveTokenRequest(TokenRequest tokenRequest) {
        int resourceID = tokenRequest.getResourceID();
        Node sender = tokenRequest.getSender();
        System.out.println("RcvREQ_T---------------------------------------------------------------------------------------");
        System.out.println("---------------------------------------------------------------------------------------");
    }

    private void receiveCounter(CounterMessage counterMessage) {
        System.out.println("RcvC---------------------------------------------------------------------------------------");
        System.out.println("---------------------------------------------------------------------------------------");
    }

    private void receiveToken(TokenMessage tokenMessage) {
        System.out.println("RcvT--------------------------------------------------------------------------------------- State = " + this.getState());
        System.out.println("---------------------------------------------------------------------------------------");
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

    @Override
    public Object clone() {
        return new AlgoJL(this.transportPID, this.nbResource, this.myPid, this.nbCS, this.MIN_CS, this.MAX_CS);
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

    /**
     * <p>Permet de faire pointer notre noeud sur le noeud link pour la ressource precisee en parametres.</p>
     * <p><strong>ATTENTION!</strong> Le jeton contenue dans {@link AlgoJL#arrayToken} et associe a la ressource n'est pas mis a jour avec cette methode.</p>
     *
     * @param resourceID l'ID de la ressource concernee
     * @param link       le nouveau noeud lien pour cette ressour (peut etre null)
     */
    public void setNodeLink(int resourceID, Node link) {
        this.dynamicTree[resourceID] = link;
    }

    /**
     * <p>Transforme ceux noeud en noeud initiale c'est a dire le noeud qui possède tous les jetons au debut.</p>
     * <p>Ne doit etre appele que sur un noeud logiquequement.</p>
     */
    public void setInitialNode() {
        for (int i = 0; i < this.arrayToken.length; i++) {
            this.arrayToken[i] = new Token(this, i);
        }
    }

    /**
     * <p>Methode a appeler lorsqu'on recoit un jeton, elle permet de stocker le jeton dans le tableau des jetons present sur le site.</p>
     * <p>Aucun traitement en rapport avec les CS n'est fait, seul l'ajout dans le tableau des jetons present sur le noeud est fait.</p>
     *
     * @param token - le jeton qui vient d'arriver sur le noeud.
     */
    public void tokenArrived(Token token) {
        if (this.arrayToken[token.getResourceID()] == null) {
            this.arrayToken[token.getResourceID()] = token;
        } else {
            System.out.println("N = " + this.node.getID() + " PB -> RECEPTION D'UN JETON DEJA PRESENT!!!!");
        }
    }

    /**
     * @param resourceID - la ressource pour laquelle on veut savoir si le jeton ets present ou pas.
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

    // Public enum.

    public enum State {
        NOTHING, // On ne fait rien.
        WAIT_S, // On attend les compteurs.
        WAIT_CS, // on attend les jetons.
        IN_CS // On est en section critique.
    }
}
