/**
 *
 */
package org.theseed.proteins;

import org.theseed.magic.MagicMap;

/**
 * This is the mapping class for a function map.  We currently don't save and restore function maps,
 * so there is very little to it.
 *
 * @author Bruce Parrello
 *
 */
public class FunctionMap extends MagicMap<Function> {

    /**
     * Construct an empty function map.
     */
    public FunctionMap() {
        super(new Function());
    }

    /**
     * Find the named function.  If it does not exist, a new function object will be created.
     *
     * @param funDesc	the function name
     *
     * @return	a Function object for the function
     */
    public Function findOrInsert(String funDesc) {
        Function retVal = this.getByName(funDesc);
        if (retVal == null) {
            // Create a function without an ID.
            retVal = new Function(funDesc);
            // Store it in the map to create the ID.
            this.put(retVal);
        }
        return retVal;
    }

}
