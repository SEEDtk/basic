/**
 *
 */
package org.theseed.reports;

import java.util.Comparator;

/**
 * This comparator performs a natural sort of strings that contain digits.  The string is broken into string parts and
 * digit parts, and each digit part is converted to an integer for comparison purposes.
 *
 * @author Bruce Parrello
 *
 */
public class NaturalSort implements Comparator<String> {

    private int[] n = new int[2];

    @Override
    public int compare(String o1, String o2) {
        int retVal = 0;
        // These are the current position in each string.
        int i1 = 0;
        int i2 = 0;
        while (retVal == 0 && i1 < o1.length() && i2 < o2.length()) {
            if (Character.isDigit(o1.charAt(i1)) && Character.isDigit(o2.charAt(i2))) {
                i1 = this.consume(o1, 0, i1);
                i2 = this.consume(o2, 1, i2);
                retVal = n[0] - n[1];
            } else {
                retVal = Character.toLowerCase(o1.charAt(i1)) - Character.toLowerCase(o2.charAt(i2));
                if (retVal == 0)
                    retVal = o2.charAt(i2) - o1.charAt(i1);
                i1++;
                i2++;
            }
        }
        if (retVal == 0) {
            if (i1 < o1.length())
                retVal = o1.charAt(i1);
            else if (i2 < o2.length())
                retVal = -o2.charAt(i2);
        }
        return retVal;
    }

    /**
     * Accumulate the number at the current position in the specified string.
     * The current character must be a digit.
     *
     * @param o		string to separate
     * @param idx	array index for storing result
     * @param i		current position in the string
     */
    private int consume(String o, int idx, int i) {
        int retVal = i + 1;
        int accum = (o.charAt(i) - '0');
        while (retVal < o.length() && Character.isDigit(o.charAt(retVal))) {
            accum = accum * 10 + (o.charAt(retVal) - '0');
            retVal++;
        }
        this.n[idx] = accum;
        return retVal;
    }

}
