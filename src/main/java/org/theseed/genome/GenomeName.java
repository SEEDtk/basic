/**
 *
 */
package org.theseed.genome;

import org.apache.commons.lang3.StringUtils;
import org.theseed.magic.MagicObject;

/**
 * This object represents a genome name, and allows genome names to be compressed into magic words.
 *
 * @author Bruce Parrello
 *
 */
public class GenomeName extends MagicObject implements Comparable<GenomeName> {

    // FIELDS
    /** serialization identifier */
    private static final long serialVersionUID = 6822023632390655701L;

    /**
     * Construct a genome name from a name and a known ID.
     *
     * @param id	ID to use
     * @param name	name of the genome
     */
    public GenomeName(String id, String name) {
        super(id, name);
    }

    /**
     * Construct a blank, empty genome name.
     */
    public GenomeName() { }

    @Override
    protected String normalize(String name) {
        // The normalized genome name is simply in lower case.
        return StringUtils.lowerCase(name);
    }

    @Override
    public int compareTo(GenomeName o) {
        return super.compareTo(o);
    }


}
