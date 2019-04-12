package common.util;

import peersim.core.CommonState;

public class Util {

    /**
     * <p>Permet de generer un nombre aleatoir entre min et max.</p>
     *
     * @return un nombre aleatoir entre min et max.
     */
    public static int generateRandom(int min, int max) {
        return min + (int) (CommonState.r.nextDouble() * ((max - min) + 1));
    }

}
