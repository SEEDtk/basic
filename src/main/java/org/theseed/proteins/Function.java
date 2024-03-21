/**
 *
 */
package org.theseed.proteins;

import java.util.Comparator;
import java.util.regex.Pattern;

import org.apache.commons.lang3.RegExUtils;
import org.theseed.magic.MagicObject;
import org.theseed.reports.NaturalSort;
import org.theseed.roles.RoleUtilities;

/**
 * This class represents a full functional assignment, and is used when we want to classify proteins by function instead
 * of role.
 *
 * @author Bruce Parrello
 *
 */
public class Function extends MagicObject {

    // FIELDS
    /** serialization ID */
    private static final long serialVersionUID = -454453869754978161L;
    /** parsing pattern for removing EC numbers */
    private static final Pattern EC_PATTERN = Pattern.compile(Role.EC_REGEX);
    /** parsing pattern for removing TC numbers */
    private static final Pattern TC_PATTERN = Pattern.compile(Role.TC_REGEX);


    /**
     * Create a blank function object.
     */
    public Function() {
        super();
    }

    /**
     * Create a function object with a blank ID for a specific description.
     *
     * @param funDesc	function description
     */
    public Function(String funDesc) {
        super(null, funDesc);
    }

    /**
     * Create a function object with a specific ID and description.
     *
     * @param funId		function ID
     * @param funDesc	function description
     */
    public Function(String funId, String funDesc) {
        super(funId, funDesc);
    }

    @Override
    protected String normalize() {
        return this.normalize(this.getName());
    }

    /**
     * @return a normalized copy of the specified function string
     *
     * @param funDesc	function description to normalize
     */
    protected String normalize(String funDesc) {
        String retVal = RoleUtilities.commentFree(funDesc);
        // Remove all the EC and TC numbers.
        retVal = RegExUtils.replaceAll(retVal, EC_PATTERN, " ");
        retVal = RegExUtils.replaceAll(retVal, TC_PATTERN, " ");
        // Fix common spelling and punctuation errors.
        retVal = Role.fixSpelling(retVal);
        // Remove any leftover spaces or extra punctuation.
        retVal = RegExUtils.replaceAll(retVal, Role.EXTRA_SPACES, " ");
        // Return the normalized result.
        return retVal;
    }

    /**
     * @return TRUE if the specified function description matches this function
     *
     * @param funDesc	function description to check
     */
    public boolean matches(String funDesc) {
        String normalized = this.normalize(funDesc);
        return this.checksumOf(normalized).equals(this.getChecksum());
    }

    /**
	 * @return a function with the comment removed
	 *
	 * @param function	function to check for comments
	 */
	public static String commentFree(String function) {
		return RoleUtilities.commentFree(function);
	}

    /**
     * This is a comparator for sorting functions by name.
     */
    public static class ByName implements Comparator<Function> {

        private NaturalSort sorter;

        public ByName() {
            this.sorter = new NaturalSort();
        }

        @Override
        public int compare(Function o1, Function o2) {
            return sorter.compare(o1.getName(), o2.getName());
        }

    }

}
