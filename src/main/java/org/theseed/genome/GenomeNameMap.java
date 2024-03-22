/**
 *
 */
package org.theseed.genome;

import org.theseed.magic.MagicMap;

/**
 * This is a magic-word map that allows us to condense genome names into unique identifiers.
 *
 * @author Bruce Parrello
 *
 */
public class GenomeNameMap extends MagicMap<GenomeName> {

    /**
     * Construct a blank, empty genome name map.
     */
    public GenomeNameMap() {
        super(new GenomeName());
    }

    /**
     * Find the genome name object for the genome with the given name.
     * If none exists, one will be inserted.
     *
     * @param genomeName	name of the relevant genome
     */
    public GenomeName findOrInsert(String genomeName) {
        // Try to find it by name.
        GenomeName retVal = this.getByName(genomeName);
        if (retVal == null) {
            // Here we need to create the name.  We build a dummy name object and when we
            // put it in the map the ID will be filled in.
            retVal = new GenomeName(null, genomeName);
            this.put(retVal);
        }
        return retVal;
    }

}
