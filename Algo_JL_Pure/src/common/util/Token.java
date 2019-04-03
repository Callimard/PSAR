package common.util;

import common.message.CounterRequest;
import common.message.TokenRequest;
import peersim.AlgoJL;

import java.util.*;

public class Token {

    // Variables.

    /**
     * <p>Noeud parent. (Ici on represente le noeud par son protocole AlgoJL).</p>
     */
    private final AlgoJL parent;

    /**
     * <p>Id de la ressource. Si un noeud veut la ressource, il utilisera cet resourceID pour l'identifier.</p>
     */
    private final int resourceID;

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
    private List<CounterRequest> queueCounterRequest = new LinkedList<>();

    /**
     * <p>La queue des requetes de resources. Si une requete est presente dans cette queue, c'est que le noeud qui a envoye la requete veut cette ressource.</p>
     * <p>Cette queue est triee dans l'ordre des requete prioritaire a moins prioritaire (Tout depend du vecteur de compteur de la requete).</p>
     */
    private List<TokenRequest> queueTokenRequest = new LinkedList<>();

    // Constructors.

    public Token(AlgoJL parent, int resourceID) {
        this.parent = parent;

        this.resourceID = resourceID;
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

    public long getCurrentCounterValue() {
        return this.counter;
    }

    /**
     * <p>Ajoute la requete de compteur dans la queue.</p>
     *
     * @param counterRequest
     * @return true si la requete a ete ajoute.
     */
    public boolean addCounterRequest(CounterRequest counterRequest) {
        return this.queueCounterRequest.add(counterRequest);
    }

    /**
     * @return la prochaine requete de compteur et la retire de la queue, si la liste est vide, retourne null.
     */
    public CounterRequest nextCounterRequest() {
        return this.queueCounterRequest.remove(0);
    }

    /**
     * <p>Ajoute la requete de ressource dans la queue. Apres l'ajout, la queue est triee pour etre a jour et que la premiere valeur de la list (get(0))
     * soit la requete la plus prioritaire.</p>
     *
     * @param tokenRequest
     * @return true
     */
    public boolean addResourceRequest(TokenRequest tokenRequest) {
        this.queueTokenRequest.add(tokenRequest);

        this.queueTokenRequest.sort((TokenRequest o1, TokenRequest o2) -> {
            if ((o1.getMark() < o2.getMark()) || (o1.getMark() == o2.getMark() && o1.getSender().getID() < o2.getSender().getID())) {
                return -1;
            } else {
                return 1;
            }

            /*if (o1.getMark() < o2.getMark()) {
                return -1;
            } else if (o1.getMark() > o2.getMark()) {
                return 1;
            } else {
                if (o1.getNode() < o2.getNode()) {
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
    public TokenRequest nextResourceRequest() {
        if (!this.queueTokenRequest.isEmpty()) {
            return this.queueTokenRequest.remove(0);
        } else
            return null;
    }

    /**
     * <p>Met a jour le token local pour qu'il corresponde au token entre en parametres.</p>
     * <p>Le token entre en parametre doit absolument gerer la meme ressource que l'instance qui appel la methode.</p>
     * <p>Les liens de l'arbre dynamique ne sont pas gere dans cette methode. Il faut voir du coté de la class {@link AlgoJL}</p>
     *
     * @param token
     */
    public void updateToken(Token token) {
        if (token.getResourceID() == this.getResourceID()) {
            this.counter = token.counter;
            this.isHere = true;

            if (!this.queueCounterRequest.isEmpty()) {
                System.err.println("ATTENTION!!! COUNTER QUEUE PAS VIDE ET ON VA UPDATE -> PAS LOGIQUE.");
            }
            this.queueCounterRequest.clear();
            for (CounterRequest counterRequest : token.queueCounterRequest) {
                this.addCounterRequest(counterRequest);
            }

            if (!this.queueTokenRequest.isEmpty()) {
                System.err.println("ATTENTION!!! RESOURCE QUEUE PAS VIDE ET ON VA UPDATE -> PAS LOGIQUE.");
            }
            this.queueTokenRequest.clear();
            for (TokenRequest tokenRequest : token.queueTokenRequest) {
                this.addResourceRequest(tokenRequest);
            }
        } else {
            System.err.println("UPDATE DE TOKEN QUI NE GERE PAS LA MEME RESSOURCE!!!");
        }
    }

    // Getters and Setters.

    public AlgoJL getParent() {
        return this.parent;
    }

    public int getResourceID() {
        return this.resourceID;
    }

    public boolean isHere() {
        return this.isHere;
    }

    public void setHere(boolean isHere) {
        this.isHere = isHere;
    }

}
