package common.util;

import java.util.*;

import common.message.LoanRequest;
import common.message.Request;
import common.message.TokenRequest;
import peersim.core.Node;

public class Token {

    // Variables.

    /**
     * <p>
     * Id de la ressource. Si un noeud veut la ressource, il utilisera cet
     * resourceID pour l'identifier.
     * </p>
     */
    private final int resourceID;

    /**
     * <p>
     * Compteur qui est incrémenté à chaque fois que l'on veut voir sa valeur.
     * </p>
     */
    private long counter = 1;

    /**
     * <p>
     * Sont initialement vide. Permet de savoir si une requete de compteur est
     * obselete ou pas.
     * </p>
     */
    private Map<Long, Integer> lastReqC = new HashMap<>();

    /**
     * <p>
     * Sont initialement vide. Permet de savoir si une requete de jeton est obselete
     * ou pas.
     * </p>
     */
    private Map<Long, Integer> lastCS = new HashMap<>();

    /**
     * <p>
     * La queue des requetes de resources. Si une requete est presente dans cette
     * queue, c'est que le noeud qui a envoye la requete veut cette ressource.
     * </p>
     * <p>
     * Cette queue est triee dans l'ordre des requete prioritaire a moins
     * prioritaire (Tout depend du vecteur de compteur de la requete).
     * </p>
     */
    private List<TokenRequest> queueTokenRequest = new LinkedList<>();

    /**
     * <p>
     * La liste triee des requete Loan. Même principe que
     * {@link Token#queueTokenRequest}.
     * </p>
     */
    private List<LoanRequest> queueLoanRequest = new LinkedList<>();

    /**
     * <p>
     * Le noeud qui nous a preter.
     * </p>
     */
    private Node lenderNode = null;

    // Constructors.

    public Token(int resourceID) {
        this.resourceID = resourceID;
    }

    // Methods.

    /**
     * <p>
     * Incremente la valeur de {@link Token#counter}. Retourne la valeur avant
     * l'incrementation.
     * </p>
     *
     * @return la valeur de {@link Token#counter} <strong>avant</strong>
     * l'incrementation.
     */
    public long incrementCounter() {
        long current = this.counter;
        this.counter++;

        return current;
    }

    /**
     * @return la valeur du compteur sans l'incrementer.
     */
    public long getCurrentCounterValue() {
        return this.counter;
    }

    /**
     * <p>
     * Ajoute la requete de ressource dans la queue. Apres l'ajout, la queue est
     * triee pour etre a jour et que la premiere valeur de la list (get(0)) soit la
     * requete la plus prioritaire.
     * </p>
     *
     * @param tokenRequest
     */
    public void addTokenRequest(TokenRequest tokenRequest) {

        System.out.println("ADD FOR " + tokenRequest.getSender().getID() + " tokenRequest = " + tokenRequest);

        this.queueTokenRequest.add(tokenRequest);

        this.queueTokenRequest.sort((TokenRequest o1, TokenRequest o2) -> {
            if ((o1.getMark() < o2.getMark())
                    || (o1.getMark() == o2.getMark() && o1.getSender().getID() < o2.getSender().getID())) {
                return -1;
            } else {
                return 1;
            }
        });

    }

    /**
     * @return la prochaine requete de ressource et la retire de la queue, si la
     * liste est vide, retourne null.
     */
    public TokenRequest nextTokenRequest() {
        if (!this.queueTokenRequest.isEmpty()) {
            TokenRequest tokenRequest = this.queueTokenRequest.remove(0);

            System.out.println("DEQUE FOR " + tokenRequest.getSender().getID() + " tokenRequest = " + tokenRequest);

            return tokenRequest;
        } else
            return null;
    }

    /**
     * @return la requete en tete de liste si cette derniere n'est pas vide, sinon
     * null.
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
     * <p>Retire la requete de jeton associe au noeud nevoyeur et la retourne.</p>
     *
     * @param sender
     * @return la requete de jeton associe au noeud envoyeur, si le noeud envoyeur n'a pas de requete de jeton dans le jeton, retourne null.
     */
    public TokenRequest removeTokenRequestFor(Node sender) {
        for (int i = 0; i < this.queueTokenRequest.size(); i++) {
            TokenRequest tokenRequest = this.queueTokenRequest.get(i);
            if (tokenRequest.getSender().getID() == sender.getID()) {
                return this.queueTokenRequest.remove(i);
            }
        }

        return null;
    }

    /**
     * <p>
     * Ajoute la requete de pret dans la queue. Apres l'ajout, la queue est triee
     * pour etre a jour et que la premiere valeur de la list (get(0)) soit la
     * requete la plus prioritaire.
     * </p>
     *
     * @param loanRequest
     */
    public void addLoanRequest(LoanRequest loanRequest) {

        System.out.println("ADD FOR " + loanRequest.getSender().getID() + " loanRequest = " + loanRequest);

        this.queueLoanRequest.add(loanRequest);

        this.queueLoanRequest.sort((LoanRequest o1, LoanRequest o2) -> {
            if ((o1.getMark() < o2.getMark())
                    || (o1.getMark() == o2.getMark() && o1.getSender().getID() < o2.getSender().getID())) {
                return -1;
            } else {
                return 1;
            }
        });

    }

    /**
     * @return la prochaine requete de pret et la retire de la queue, si la liste
     * est vide, retourne null.
     */
    public LoanRequest nextLoanRequest() {
        if (!this.queueLoanRequest.isEmpty()) {
            LoanRequest loanRequest = this.queueLoanRequest.remove(0);

            System.out.println("DEQUE FOR " + loanRequest.getSender().getID() + " loanRequest = " + loanRequest);

            return loanRequest;
        } else
            return null;
    }

    /**
     * @return la requete en tete de liste si cette derniere n'est pas vide, sinon
     * null.
     */
    public LoanRequest seeHeadLoanRequestQueue() {
        if (!this.queueLoanRequest.isEmpty()) {
            return this.queueLoanRequest.get(0);
        } else
            return null;
    }

    public void clearLoanRequestQueue() {
        this.queueLoanRequest.clear();
    }

    /**
     * @return true si la queue des requete de jeton est vide, sinon false.
     */
    public boolean loanRequestQueueEmpty() {
        return this.queueLoanRequest.isEmpty();
    }

    public Set<LoanRequest> copyLoanRequestQueue() {
        return new TreeSet<>(this.queueLoanRequest);
    }

    public boolean contains(LoanRequest loanRequest) {
        return this.queueLoanRequest.contains(loanRequest);
    }

    /**
     * @param nodeID le noeud pour lequel on veut voir la derniere requete de
     *               compteur traite.
     * @return le requeteID de la derniere requete de compteur traite par ce jeton
     * pour ce noeud. Retourne null si le jeton n'a encore jamais traiter de
     * requete de compteur pour ce noeud.
     */
    public int getLastReqC(long nodeID) {
        return this.lastReqC.get(nodeID) == null ? -1 : this.lastReqC.get(nodeID);
    }

    public void putLastReqC(long nodeID, int requestID) {
        if (this.lastReqC.get(nodeID) != null) {
            if (this.lastReqC.get(nodeID) < requestID)
                this.lastReqC.put(nodeID, requestID);
        } else {
            this.lastReqC.put(nodeID, requestID);
        }
    }

    /**
     * @param nodeID le noeud pour lequel on veut voir la derniere requete de jeton.
     * @return le requeteID de la derniere requete de jeton traite par ce jeton pour
     * ce noeud. Retourne null si le jeton n'a encore jamais traiter de
     * requete de compteur pour ce noeud.
     */
    public int getLastCS(long nodeID) {
        return this.lastCS.get(nodeID) == null ? -1 : this.lastCS.get(nodeID);
    }

    public void putLastCS(long nodeID, int requestID) {
        if (this.lastCS.get(nodeID) != null) {
            if (this.lastCS.get(nodeID) < requestID)
                this.lastCS.put(nodeID, requestID);
        } else {
            this.lastCS.put(nodeID, requestID);
        }
    }

    // Getters and Setters.

    public int getResourceID() {
        return this.resourceID;
    }

    public Node getLenderNode() {
        return lenderNode;
    }

    public void setLenderNode(Node lenderNode) {
        /*assert (this.lenderNode != null && lenderNode == null) || (this.lenderNode == null
                && lenderNode != null) : "(this.lenderNode != null && lenderNode == null) = "
                + (this.lenderNode != null && lenderNode == null)
                + " (this.lenderNode == null && lenderNode != null) = "
                + (this.lenderNode == null && lenderNode != null);*/

        this.lenderNode = lenderNode;
    }
}
