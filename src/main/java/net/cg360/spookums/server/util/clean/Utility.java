package net.cg360.spookums.server.util.clean;

import java.util.Random;

/**
 * A few utility methods.
 * @author CG360
 */
public class Utility {

    public static final char[] UNIQUE_TOKEN_CHARACTERS = new char[]{'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z','A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z','0','1','2','3','4','5','6','7','8','9','$','&','*','^','%','(',')'};

    /**
     * Generates a random set of characters from the UNIQUE_TOKEN_CHARACTERS pool
     * @param minlength the minimum length of the generated string
     * @param variation the variation in length of the generated string
     * @return the generated string
     */
    public static String generateUniqueToken(int minlength, int variation){
        int length = minlength + (variation > 0 ? new Random().nextInt(variation) : 0);
        String fstr = "";
        for(int i = 0; i < length; i++){
            Random r = new Random();
            fstr = fstr.concat(String.valueOf(UNIQUE_TOKEN_CHARACTERS[r.nextInt(UNIQUE_TOKEN_CHARACTERS.length)]));
        }
        return fstr;
    }

    /**
     * Selects a random string out of an array.
     * @return the picked string
     */
    public static String pickRandomString(String[] strings){
        int index = new Random().nextInt(strings.length);
        return strings[index];
    }



}
