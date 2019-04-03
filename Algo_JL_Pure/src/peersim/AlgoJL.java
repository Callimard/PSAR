package peersim;

import peersim.config.Configuration;
import peersim.core.Node;
import peersim.edsim.EDProtocol;

public class AlgoJL implements EDProtocol {

    // Variables.

    /**
     * <p>Le protocol de transport.</p>
     */
    private final int transport;

    private final int nbResource;

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
            this.arrayToken[i] = new Token(i);
        }

        this.resourceNodeLink = new Node[this.nbResource];

        System.out.println("Nb ressource = " + this.nbResource);
    }

    // Methods.

    @Override
    public void processEvent(Node node, int i, Object o) {

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

    public void setAllResourcesHere() {

        System.out.println("Je suis la dans le setAllResourceHere.");

        for (int i = 0; i < this.arrayToken.length; i++) {
            this.arrayToken[i].setHere(true);
        }
    }

    public void setNodeLink(int resource, Node link) {
        this.resourceNodeLink[resource] = link;
    }

    // Getters and Setters.

    public int getNbResource() {
        return this.nbResource;
    }

    // Private class.

    private class Token {

        // Variables.

        private int id;
        private boolean isHere;

        // Constructors.

        public Token(int id) {
            this.id = id;
            this.isHere = false;
        }

        // Methods.

        // Getters and Setters.

        public int getId() {
            return this.id;
        }

        public boolean isHere() {
            return this.isHere;
        }

        public void setHere(boolean isHere) {
            this.isHere = isHere;
        }

    }
}
