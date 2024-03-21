/**
 *
 */
package org.theseed.magic;

import java.io.Serializable;

import murmur3.MurmurHash3;
import murmur3.MurmurHash3.LongPair;

/**
 * This represents an object that can be inserted into a magic ID table.
 *
 * @author Bruce Parrello
 *
 */
public abstract class MagicObject implements Serializable {


    /**
     * ID for serialization
     */
    private static final long serialVersionUID = 2963558746842724469L;

    /** default seed */
    private static final int SEED = 1842724469;

    /** regex for common punctuation */
    static private final String PUNCTUATION = ",\\.;:";

    // FIELDS
    /** internal copy of the ID */
    private String id;
    /** internal copy of the name */
    private String name;
    /** internal copy of the checksum */
    private LongPair checksum;

    /**
     * Create a magic object with a name and and ID.
     */
    public MagicObject(String id, String name) {
        this.id = id;
        this.name = name;
        this.setChecksum();
    }

    /**
     * Compute the checksum for this magic object.
     */
    private void setChecksum() {
        this.checksum = this.getChecksum(this.name);
    }

    /**
     * Compute the checksum for a specified name.  This method CANNOT modify the object.
     *
     * @param name		name to checksum
     *
     * @return the desired checksum
     */
    public LongPair getChecksum(String name) {
        String normalized = this.normalize(name);
        return this.checksumOf(normalized);
    }

    /**
     * @return the checksum of a normalized name string
     *
     * @param normalized	normalized name string of interest
     */
    protected LongPair checksumOf(String normalized) {
        LongPair retVal = new LongPair();
        final byte[] workBuffer = new byte[19];
        MurmurHash3.murmurhash3_x64_128(normalized, 0, normalized.length(), SEED, workBuffer, retVal);
        return retVal;
    }

    /**
     * Create a blank magic object.
     */
    public MagicObject() {
        this.id = null;
        this.name = null;
        this.checksum = new LongPair();
    }

    /** Two objects with the same name will have the same checksum.  So, if checksums
     * are equal, the objects are equal. Otherwise, we compare names.  NULL compares
     * last.
     */
    public int compareTo(MagicObject o) {
        int retVal;
        if (this.name == o.name) {
            retVal = 0;
        } else if (this.name == null) {
            // Null compares high.
            retVal = -1;
        } else if (o.name == null) {
            retVal = 1;
        } else if (this.checksum.equals(o.checksum)) {
            retVal = 0;
        } else {
            retVal = this.name.compareTo(o.name);
        }
        return retVal;
    }

    /**
     * @return the normalized version of the object name
     */
    protected String normalize() {
        String retVal = this.getName();
        retVal = this.normalize(retVal);
        return retVal;
    }

    /**
     * This is the normalization engine.  It CANNOT modify the object.
     *
     * @param name	name to normalize
     *
     * @return the normalized version of a name
     */
    protected abstract String normalize(String name);

    @Override
    public String toString() {
        return (this.name == null ? "<empty>" : this.name);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((checksum == null) ? 0 : checksum.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MagicObject other = (MagicObject) obj;
        if (checksum == null) {
            if (other.checksum != null)
                return false;
        } else if (!checksum.equals(other.checksum))
            return false;
        return true;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Update the name.  This will set the checksum.  This can cause
     * the magic ID to go out of sync with the name unless the object
     * is blank.
     *
     * @param name	the name to set
     */
    protected void setName(String name) {
        this.name = name;
        this.setChecksum();
    }

    /**
     * Update the ID.  This should only be done by MagicMap.
     *
     * @param id	the ID to set
     */
    /* package private */ void setId(String id) {
        this.id = id;
    }

    /**
     * @return the id
     */
    public String getId() {
        return this.id;
    }

    /**
     * @return the checksum
     */
    public LongPair getChecksum() {
        return this.checksum;
    }

    /**
     * Join two text strings.  If there is no punctuation at the start of
     * the second string, a space is inserted.
     *
     * @param string1	first string to use
     * @param string2	string to join to it
     *
     * @return the joined string
     */
    public static String join_text(String string1, String string2) {
        StringBuilder retVal = new StringBuilder(string1.length() +
                string2.length() + 1);
        retVal.append(string1);
        if (string2.length() > 0) {
            char starter = string1.charAt(0);
            if (PUNCTUATION.indexOf(starter) < 0) {
                retVal.append(' ');
            }
            retVal.append(string2);
        }
        return retVal.toString();
    }


}
