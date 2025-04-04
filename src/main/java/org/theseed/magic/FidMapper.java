/**
 *
 */
package org.theseed.magic;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.ParseFailureException;

/**
 * This is the control structure for mapping feature IDs.  A new feature ID is composed of a new genome ID
 * plus new identifying information for the feature.
 *
 * The mapper contains a master genome map that converts genome IDs to new genome IDs.
 *
 * In addition, there is a master function map that converts functional assignments to magic words as well.
 * Finally, for the current genome we maintain a cache that maps FIG feature IDs to magic word IDs.
 *
 * @author Bruce Parrello
 *
 */
public abstract class FidMapper {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(FidMapper.class);
    /** genome ID map */
    private Map<String, String> genomeIdMap;
    /** feature ID map */
    private Map<String, String> featureIdMap;
    /** current genome old ID */
    private String currGenomeId;
    /** current genome new ID */
    private String currGenomeNewId;
    /** feature ID parser */
    protected static final Pattern FID_PATTERN = Pattern.compile("fig\\|(\\d+\\.\\d+)\\.([^.]+)\\.(\\d+)");

    /**
     * Create a new, blank FID mapper.
     */
    public FidMapper() {
        this.genomeIdMap = new HashMap<String, String>();
        this.featureIdMap = new HashMap<String, String>();
        this.currGenomeId = "";
        this.currGenomeNewId = null;
    }

    /**
     * Set up this map for a new genome.
     *
     * @param genomeId		ID of the new genome
     * @param genomeName	name of the new genome
     */
    public void setup(String genomeId, String genomeName) {
        // Set up the subclass structures.
    	this.setupGenome(genomeId, genomeName);
        // Erase the old features.
        this.featureIdMap.clear();
        // Get a new ID for the genome.
        String currGenomeNewId = this.getNewGenomeId(genomeId, genomeName);
        // Save the magic word ID.
        this.currGenomeId = genomeId;
        this.currGenomeNewId = currGenomeNewId;
    }

    /**
     * Initialize for a new genome.
     *
	 * @param genomeId		ID of the genome
	 * @param genomeName	name of the genome
	 */
	protected abstract void setupGenome(String genomeId, String genomeName);

	/**
	 * Return the (possibly unknown) new genome ID for a genome.
	 *
	 * @param genomeId		old genome ID
	 * @param genomeName	genome name
	 *
	 * @return a new genome ID which may or may not have been generated
	 */
	protected String getNewGenomeId(String genomeId, String genomeName) {
		String retVal = this.genomeIdMap.get(genomeId);
		if (retVal == null) {
			// Here we have a new genomeID, so we have to generate.
			retVal = this.createNewGenomeId(genomeId, genomeName);
			this.genomeIdMap.put(genomeId, retVal);
		}
		return retVal;
	}

    /**
     * Return a brand-new genome ID for a genome.
     *
	 * @param genomeId		old genome ID
	 * @param genomeName	genome name
	 *
	 * @return new genome ID
	 */
	protected abstract String createNewGenomeId(String genomeId, String genomeName);

	/**
     * Compute the new ID for a feature.
     *
     * @param fid			FIG ID of the feature
     * @param function		functional assignment of the feature
     * @param newGenomeId	new ID for the genome
     *
     * @throws ParseFailureException
     */
    public String getNewFid(String fid, String function) throws ParseFailureException {
    	// Check for a cached result.
    	String retVal = this.featureIdMap.get(fid);
    	if (retVal == null) {
    		// Here we have to generate the new feature ID.
    		retVal = this.createNewFeatureId(fid, function, this.currGenomeNewId);
            // Save the result in the feature ID map.
            this.featureIdMap.put(fid, retVal);
    	}
    	return retVal;

    }

	/**
	 * @return TRUE if the specified genome is the current one
	 *
	 * @param genomeId	genome ID to check
	 */
	protected boolean isGenome(String genomeId) {
		return this.currGenomeId.contentEquals(genomeId);
	}

    /**
     * Construct a new feature ID for the specified feature.
     *
	 * @param fid			old feature ID
	 * @param function		functional assignment
	 * @param newGenomeId	new ID of the host genome
	 *
	 * @return the new feature's ID
	 *
     * @throws ParseFailureException
	 */
	protected String createNewFeatureId(String fid, String function, String newGenomeId) throws ParseFailureException {
		String retVal = null;
        // Here we have to compute the feature's magic ID.  Parse the ID string.
        Matcher m = FidMapper.FID_PATTERN.matcher(fid);
        if (! m.matches())
            throw new IllegalArgumentException("Invalid feature ID \"" + fid + "\".");
        else {
            // Group 1 is the genome ID.  Insure it's the current genome.
            if (! this.isGenome(m.group(1)))
                throw new ParseFailureException("Feature \"" + fid + "\" is not from the current genome.");
            else
            	retVal = this.computeNewFeatureId(m, function, newGenomeId);
        }
        return retVal;
	}

	/**
	 * Compute the new feature ID for a feature. This method guarantees that we are in the
	 * current genome and has already parsed the old feature ID.
	 *
	 * @param m				matcher for the old feature ID
	 * @param function		functional assignment
	 * @param newGenomeId	new ID of the host genome
	 *
	 * @return the new feature's ID
	 *
     * @throws ParseFailureException
	 */
	protected abstract String computeNewFeatureId(Matcher m, String function, String newGenomeId) throws ParseFailureException;

	/**
     * This retrieves an existing feature identifier given only the old feature ID.  If the
     * feature does not have a new ID yet, it returns NULL;
     *
     * @param fid	feature ID to translate
     *
     * @return the new identifier for the feature
     */
    public String getNewFid(String fid)  {
        return this.featureIdMap.get(fid);
    }

    /**
     * Suffix a count to a magic word.  We need to insert an "n" if the word ends
     * with a digit.
     *
     * @param word		magic word to suffix
     * @param count		ordinal occurrence number of this word instance
     *
     * @return the suffixed word
     */
    protected static String suffixCount(String word, final int count) {
        final int last = word.length() - 1;
        if (Character.isDigit(word.charAt(last)))
            word += "n" + String.valueOf(count);
        else if (count > 1)
            word += String.valueOf(count);
        return word;
    }

    /**
     * Return the new genome ID for a given genome.
     *
     * @param oldId		original genome ID
     *
     * @return the new ID, or NULL if it is not known
     */
    public String getNewGenomeId(String oldId) {
        return this.genomeIdMap.get(oldId);
    }

}
