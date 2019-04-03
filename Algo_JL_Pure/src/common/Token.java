package common;

import peersim.AlgoJL;

import java.util.*;

public class Token {

    // Variables.

    /**
     * <p>Noeud parent. (Ici on represente le noeud par son protocole AlgoJL).</p>
     */
    private final AlgoJL parent;

    /**
     * <p>Id de la ressource. Si un noeud veut la ressource, il utilisera cet id pour l'identifier.</p>
     */
    private final int id;

    /**
     * <p>Si cette variable est egale a true, alors le jeton est present sur le parent.</p>
     */
    private boolean isHere;

    /**
     * <p>Compteur qui est incrémenté à chaque fois que l'on veut voir sa valeur.</p>
     */
    private long counter = 0;

    /**
     * <p>La queue des requetes de compteur.</p>
     */
    private Queue<CounterRequest> queueCounterRequest = new LinkedList<>();

    /**
     * <p>La queue des requetes de resources. Si une requete est presente dans cette queue, c'est que le noeud qui a envoye la requete veut cette ressource.</p>
     * <p>Cette queue est triee dans l'ordre des requete prioritaire a moins prioritaire (Tout depend du vecteur de compteur de la requete).</p>
     */
    private List<ResourceRequest> queueResourceRequest = new LinkedList<>();

    // Constructors.

    public Token(AlgoJL parent, int id) {
        this.parent = parent;

        this.id = id;
        this.isHere = false;
    }

    // Methods.

    /**
     * <p>Incremente la valeur de {@link Token#counter}. Retourne la valeur avant l'incrementation.</p>
     *
     * @return la valeur de {@link Token#counter} <strong>avant</strong> l'incrementation.
     */
    public long incrementCounter() {
        long current = this.counter;
        this.counter++;

        return current;
    }

    /**
     * <p>Ajoute la requete de compteur dans la queue.</p>
     *
     * @param counterRequest
     * @return true si la requete a ete ajoute.
     */
    public boolean addCounterRequest(CounterRequest counterRequest) {
        return this.queueCounterRequest.offer(counterRequest);
    }

    /**
     * @return la prochaine requete de compteur et la retire de la queue, si la liste est vide, retourne null.
     */
    public CounterRequest nextCounterRequest() {
        return this.queueCounterRequest.poll();
    }

    /**
     * <p>Ajoute la requete de ressource dans la queue. Apres l'ajout, la queue est triee pour etre a jour et que la premiere valeur de la list (get(0))
     * soit la requete la plus prioritaire.</p>
     *
     * @param resourceRequest
     * @return true
     */
    public boolean addResourceRequest(ResourceRequest resourceRequest) {
        this.queueResourceRequest.add(resourceRequest);

        this.queueResourceRequest.sort((ResourceRequest o1, ResourceRequest o2) -> {
            if ((o1.getMark() < o2.getMark()) || (o1.getMark() == o2.getMark() && o1.getNodeID() < o2.getNodeID())) {
                return -1;
            } else {
                return 1;
            }

            /*if (o1.getMark() < o2.getMark()) {
                return -1;
            } else if (o1.getMark() > o2.getMark()) {
                return 1;
            } else {
                if (o1.getNodeID() < o2.getNodeID()) {
                    return -1;
                } else {
                    return 1;
                }

            }*/
        });

        return true;
    }

    /**
     * @return la prochaine requete de ressource et la retire de la queue, si la liste est vide, retourne null.
     */
    public ResourceRequest nextResourceRequest() {
        if (!this.queueResourceRequest.isEmpty()) {
            return this.queueResourceRequest.get(0);
        } else
            return null;
    }

    // Getters and Setters.

    public AlgoJL getParent() {
        return this.parent;
    }

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
