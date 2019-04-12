package peersim;

public class Util {

    /**
     * <p>Permet de generer un nombre aleatoir entre min et max.</p>
     *
     * @return un nombre aleatoir entre min et max.
     */
    public static int generateRandom(int min, int max) {
        return min + (int) (Math.random() * ((max - min) + 1));
    }

}
