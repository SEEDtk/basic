/**
 *
 */
package org.theseed.roles;

import java.util.regex.Pattern;

import org.apache.commons.lang3.RegExUtils;

/**
 * This class is a placeholder for static constants and methods used in biology.
 * @author Bruce Parrello
 *
 */
public class RoleUtilities {

    /** match pattern for LSU rRNA */
    public static final Pattern LSU_R_RNA = Pattern.compile("LSU\\s+rRNA|Large\\s+Subunit\\s+(?:Ribosomal\\s+)?r?RNA|lsuRNA|23S\\s+(?:r(?:ibosomal\\s+)?)?RNA", Pattern.CASE_INSENSITIVE);
    /** match pattern for SSU rRNA */
    public static final Pattern SSU_R_RNA = Pattern.compile("SSU\\s+rRNA|Small\\s+Subunit\\s+(?:Ribosomal\\s+)?r?RNA|ssuRNA|16S\\s+(?:r(?:ibosomal\\s+)?)?RNA", Pattern.CASE_INSENSITIVE);
    /** parsing pattern for removing function comments */
    public static final Pattern COMMENT_PATTERN = Pattern.compile("\\s*[#!].+");

    /**
     * @return a function with the comment removed
     *
     * @param function	function to check for comments
     */
    public static String commentFree(String function) {
        return RegExUtils.removeFirst(function, COMMENT_PATTERN);
    }

}
