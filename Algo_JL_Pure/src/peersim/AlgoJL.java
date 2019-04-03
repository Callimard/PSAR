package peersim;

import common.Token;
import peersim.config.Configuration;
import peersim.core.Node;
import peersim.edsim.EDProtocol;

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
     * <p>L'ID du noeud sur lequel l'instance du protocole est. (Initialisee grace au controler {@link InitJL} qui fait appel a la methode {@link AlgoJL#setNodeID(long)}</p>
     */
    private long nodeID = -1;

    /**
     * <p>False tant que la variable {@link AlgoJL#nodeID} n'a pas ete initialise avec @link AlgoJL#setNodeID(long)}. Si vrai, alors @link AlgoJL#setNodeID(long)} n'a plus aucun effet.</p>
     */
    private boolean nodeIDSet = false;

    /**
     * <p>Represente les jetons de chaque ressource. Les ressources sont presente ou non sur le site. Pour le savoir il faut appeler la methode {@link Token#isHere()}.</p>
     */
    private Token arrayToken[];

    /**
     * <p>Point sur le noeud à qui il faut s'addresser pour acceder à la ressource d'indice i.</p>
     * <p>Si resourceNodeLink[i] est null alors cela signifie que nous possedons la ressource i.</p>
     */
    private Node resourceNodeLink[];

    // Constructors.

    public AlgoJL(String prefix) {
        this.transport = Configuration.getPid(prefix + ".tr");
        this.nbResource = Configuration.getInt(prefix + "nb_resource");

        this.arrayToken = new Token[this.nbResource];
        for (int i = 0; i < this.arrayToken.length; i++) {
            this.arrayToken[i] = new Token(this, i);
        }

        this.resourceNodeLink = new Node[this.nbResource];

        System.out.println("Nb ressource = " + this.nbResource);
    }

    // Methods.

    @Override
    public void processEvent(Node node, int i, Object o) {
        // TODO Reception soit d'un objet de type CounterRequest, soit de type ResourceRequest.
    }

    @Override
    public Object clone() {
        try {
            AlgoJL o = (AlgoJL) super.clone();
            System.out.println("Je suis la!!!");

            o.arrayToken = new Token[this.arrayToken.length];
            o.resourceNodeLink = new Node[this.resourceNodeLink.length];

            return o;
        } catch (CloneNotSupportedException e) {
            // NEVER APPEND.
            return null;
        }
    }

    /**
     * <p>Est appele a l'initialisation pour le premiere noeud. Ce premier noeud possedera toutes les ressource au debut.</p>
     */
    public void setAllResourcesHere() {

        System.out.println("Je suis la dans le setAllResourceHere.");

        for (int i = 0; i < this.arrayToken.length; i++) {
            this.arrayToken[i].setHere(true);
        }
    }

    /**
     * <p>Permet de faire pointer notre noeud sur le noeud link pour la ressource precisee en parametres.</p>
     *
     * @param resource
     * @param link
     */
    public void setNodeLink(int resource, Node link) {
        this.resourceNodeLink[resource] = link;
    }

    // Getters and Setters.

    public int getNbResource() {
        return this.nbResource;
    }

    /**
     * @return la valeur du noeud sur lequel est l'instance de ce protocole. Si la valeur n'a pas ete initialise, retourne -1.
     */
    public long getNodeID() {
        return this.nodeID;
    }

    /**
     * <p>Met la valeur de {@link AlgoJL#nodeID} a nodeID. Si la valeur a deja ete initialise, aucun changement n'est fait.</p>
     *
     * @param nodeID
     */
    public void setNodeID(long nodeID) {
        if (!this.nodeIDSet) {
            this.nodeID = nodeID;
            this.nodeIDSet = true;
        }
    }
}
