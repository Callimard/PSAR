package common.util;

import common.message.CounterRequest;
import common.message.TokenRequest;
import peersim.AlgoJL;
import peersim.core.Node;

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
    private long counter = 1;

    /**
     * <p>La queue des requetes de compteur.</p>
     */
    private List<CounterRequest> queueCounterRequest = new LinkedList<>();

    /**
     * <p>La queue des requetes de resources. Si une requete est presente dans cette queue, c'est que le noeud qui a envoye la requete veut cette ressource.</p>
     * <p>Cette queue est triee dans l'ordre des requete prioritaire a moins prioritaire (Tout depend du vecteur de compteur de la requete).</p>
     */
    private List<TokenRequest> queueTokenRequest = new LinkedList<>();

    /**
     * <p>Sont initialement vide. Permet de savoir si une requete de compteur est obselete ou pas.</p>
     */
    private Map<Long, Integer> lastReqC = new HashMap<>();

    /**
     * <p>Sont initialement vide. Permet de savoir si une requete de jeton est obselete ou pas.</p>
     */
    private Map<Long, Integer> lastCS = new HashMap<>();

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
     */
    public void addTokenRequest(TokenRequest tokenRequest) {
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

    }

    /**
     * @return la prochaine requete de ressource et la retire de la queue, si la liste est vide, retourne null.
     */
    public TokenRequest nextTokenRequest() {
        if (!this.queueTokenRequest.isEmpty()) {
            return this.queueTokenRequest.remove(0);
        } else
            return null;
    }

    /**
     * @return la requete en tete de liste si cette derniere n'est pas vide, sinon null.
     */
    public TokenRequest seeHeadTokenRequestQueue() {
        if (!this.queueTokenRequest.isEmpty()) {
            return this.queueTokenRequest.get(0);
        } else
            return null;
    }

    /**
     * @return true si la queue des requete de jeton est vide, sinon false.
     */
    public boolean tokenRequestQueueEmpty() {
        return this.queueTokenRequest.isEmpty();
    }

    public boolean contains(TokenRequest tokenRequest) {
        return this.queueTokenRequest.contains(tokenRequest);
    }

    /**
     * @param nodeID le noeud pour lequel on veut voir la derniere requete de compteur traite.
     * @return le requeteID de la derniere requete de compteur traite par ce jeton pour ce noeud. Retourne null si le jeton n'a encore jamais traiter de requete de compteur pour ce noeud.
     */
    public int getLastReqC(long nodeID) {
        return this.lastReqC.get(nodeID) == null ? -1 : this.lastReqC.get(nodeID);
    }

    public void putLastReqC(long nodeID, int requestID) {
        this.lastReqC.put(nodeID, requestID);
    }

    /**
     * @param nodeID le noeud pour lequel on veut voir la derniere requete de jeton.
     * @return le requeteID de la derniere requete de jeton traite par ce jeton pour ce noeud. Retourne null si le jeton n'a encore jamais traiter de requete de compteur pour ce noeud.
     */
    public int getLastCS(long nodeID) {
        return this.lastCS.get(nodeID) == null ? -1 : this.lastCS.get(nodeID);
    }

    public void putLastCS(long nodeID, int requestID) {
        this.lastCS.put(nodeID, requestID);
    }

    @Override
    public Object clone() {
        Token clone = new Token(this.parent, this.resourceID);

        clone.counter = counter;

        clone.queueCounterRequest.addAll(this.queueCounterRequest);
        clone.queueTokenRequest.addAll(this.queueTokenRequest);
        clone.lastReqC.putAll(this.lastReqC);
        clone.lastCS.putAll(this.lastCS);

        return clone;
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
                System.err.println("ATTENTION!!! RESSOURCE QUEUE PAS VIDE ET ON VA UPDATE -> PAS LOGIQUE.");
            }
            this.queueTokenRequest.clear();
            for (TokenRequest tokenRequest : token.queueTokenRequest) {
                this.addTokenRequest(tokenRequest);
            }

            this.lastReqC.clear();
            this.lastReqC.putAll(token.lastReqC);
//            Set<Map.Entry<Node, Integer>> setReqC = token.lastReqC.entrySet();
//            for (Map.Entry<Node, Integer> entry : setReqC) {
//                this.lastReqC.put(entry.getKey(), entry.getValue());
//            }

            this.lastCS.clear();
            this.lastCS.putAll(token.lastCS);
//            Set<Map.Entry<Node, Integer>> setCS = token.lastCS.entrySet();
//            for (Map.Entry<Node, Integer> entry : setCS) {
//                this.lastCS.put(entry.getKey(), entry.getValue());
//            }
        } else {
            System.err.println("UPDATE DE TOKEN QUI NE GERE PAS LA MEME RESSOURCE!!!");
        }
    }

    /**
     * <p>Vide toutes les queues. Cette methode est appeler lorsque le token est envoye a un autre noeud. Le noeud est d'abord clone puis on le clear pour etre coherent.</p>
     * <p>Ne clear pas les map de lastReq et lastCS car elles sont toujours utile meme si on a plus le jeton pour voir les requete obselete.</p>
     */
    public void clearAllQueue() {
        this.queueCounterRequest.clear();
        this.queueTokenRequest.clear();
    }

    // Getters and Setters.

    public AlgoJL getParent() {
        return this.parent;
    }

    public int getResourceID() {
        return this.resourceID;
    }

    public boolean isHere() {
        if (this.isHere) {
            System.out.println("LINK = " + this.parent.getNodeLink(resourceID) + " R = " + resourceID);
            if (this.parent.getNodeLink(resourceID) != null)
                try {
                    throw new Exception();
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }
        return this.isHere;
    }

    public void setHere(boolean isHere) {
        this.isHere = isHere;
    }

}
