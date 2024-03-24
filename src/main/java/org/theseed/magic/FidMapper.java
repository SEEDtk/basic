/**
 *
 */
package org.theseed.magic;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.ParseFailureException;
import org.theseed.counters.CountMap;
import org.theseed.genome.GenomeName;
import org.theseed.genome.GenomeNameMap;
import org.theseed.proteins.Function;
import org.theseed.proteins.FunctionMap;

/**
 * This is the control structure for mapping feature IDs.  A feature ID consists of a magic word generated
 * from the genome name, an optional genome index number, a feature type, a magic word generated from the
 * assigned function, and a feature sequence number.
 *
 * The mapper contains a master genome map that converts genome IDs to genome name words.  The genome name
 * is augmented by the genome ID to insure every genome has a unique magic-word ID.
 *
 * In addition, there is a master function map that converts functional assignments to magic words as well.
 * Finally, for the current genome we maintain a cache that maps FIG feature IDs to magic word IDs.
 *
 * @author Bruce Parrello
 *
 */
public class FidMapper {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(FidMapper.class);
    /** functional assignment name map */
    private FunctionMap functionMap;
    /** genome name map */
    private GenomeNameMap genomeNameMap;
    /** genome ID map */
    private Map<String, String> genomeIdMap;
    /** feature ID map */
    private Map<String, String> featureIdMap;
    /** current genome ID */
    private String currGenomeId;
    /** current genome magic word ID */
    private String currGenomeWord;
    /** count of number of times each function word was used */
    private CountMap<String> funCounters;
    /** count of number of times each genome name was used */
    private CountMap<String> nameCounters;
    /** feature ID parser */
    private static final Pattern FID_PATTERN = Pattern.compile("fig\\|(\\d+\\.\\d+)\\.(\\w+)\\.(\\d+)");

    /**
     * Create a new, blank FID mapper.
     */
    public FidMapper() {
        this.functionMap = new FunctionMap();
        this.genomeIdMap = new HashMap<String, String>();
        this.genomeNameMap = new GenomeNameMap();
        this.featureIdMap = new HashMap<String, String>();
        this.funCounters = new CountMap<String>();
        this.nameCounters = new CountMap<String>();
        this.currGenomeId = "";
        this.currGenomeWord = null;
    }

    /**
     * Set up this map for a new genome.
     *
     * @param genomeId		ID of the new genome
     * @param genomeName	name of the new genome
     */
    public void setup(String genomeId, String genomeName) {
        // Erase the old features and the function counts.
        this.funCounters.deleteAll();
        this.featureIdMap.clear();
        // Get a magic name for the genome.
        String genomeWord = this.getGenomeIdWord(genomeId, genomeName);
        // Save the magic word ID.
        this.currGenomeId = genomeId;
        this.currGenomeWord = genomeWord;
    }

    /**
     * Get the magic name for a specified genome.
     *
     * @param genomeId		ID of the new genome
     * @param genomeName	name of the new genome
     */
    public String getGenomeIdWord(String genomeId, String genomeName) {
        String retVal = this.genomeIdMap.get(genomeId);
        if (retVal == null) {
            // Here we have a new genome ID, so we have to generate.
            GenomeName gNameObject = this.genomeNameMap.findOrInsert(genomeName);
            // Get the genome ID.  We may need to suffix it.
            retVal = gNameObject.getId();
            final int count = this.nameCounters.count(retVal);
            retVal = suffixCount(retVal, count);
            this.genomeIdMap.put(genomeId, retVal);
        }
        return retVal;
    }

    /**
     * Compute the magic ID for a feature.
     *
     * @param fid			FIG ID of the feature
     * @param function		functional assignment of the feature
     *
     * @throws ParseFailureException
     */
    public String getMagicFid(String fid, String function) throws ParseFailureException {
        // Check for a cached result.
        String retVal = this.featureIdMap.get(fid);
        if (retVal == null) {
            // Here we have to compute the feature's magic ID.  Parse the ID string.
            Matcher m = FID_PATTERN.matcher(fid);
            if (! m.matches())
                throw new IllegalArgumentException("Invalid feature ID \"" + fid + "\".");
            else {
                // Group 1 is the genome ID.  Insure it's the current genome.
                if (! m.group(1).equals(this.currGenomeId))
                    throw new ParseFailureException("Feature \"" + fid + "\" is not from the current genome.");
                else if (m.group(2).equals("peg")) {
                    // Insure we have a function.
                    String pegFunction = (StringUtils.isBlank(function) ? "hypothetical protein" : function);
                    // Get the function ID word.
                    Function funObj = this.functionMap.findOrInsert(pegFunction);
                    // Now we need to suffix the function ID word.  If the word ends in a digit, we add
                    // "n" plus the count.  If the word ends in a letter and the count is 1, we don't append.
                    // otherwise we append the count.
                    String funWord = funObj.getId();
                    final int count = this.funCounters.count(funWord);
                    if (funWord.length() < 1)
                        log.error("Zero-length function word found for function \"{}\" in feature {}.", pegFunction, fid);
                    funWord = suffixCount(funWord, count);
                    // Now build the whole string.
                    retVal = this.currGenomeWord + funWord;
                } else {
                    // Here we have a not-peg.  We put less work into these.
                    retVal = this.currGenomeWord + StringUtils.capitalize(m.group(2)) + m.group(3);
                }
                // Save the result in the feature ID map.
                this.featureIdMap.put(fid, retVal);
            }
        }
        return retVal;
    }

    /**
     * This retrieves an existing feature identifier given only the feature ID.  If the
     * feature does not have a magic word ID yet, it returns NULL;
     *
     * @param fid	feature ID to translate
     *
     * @return the magic word identifier for the feature
     */
    public String getMagicFid(String fid)  {
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
    private static String suffixCount(String word, final int count) {
        final int last = word.length() - 1;
        if (Character.isDigit(word.charAt(last)))
            word += "n" + String.valueOf(count);
        else if (count > 1)
            word += String.valueOf(count);
        return word;
    }

    /**
     * Return the genome magic-word ID for a given genome.
     *
     * @param oldId		original genome ID
     *
     * @return the magic word ID, or NULL if it is not known
     */
    public String getMagicGenomeId(String oldId) {
        return this.genomeIdMap.get(oldId);
    }

    /**
     * Store a mapping from a feature alias to its magic word ID.
     *
     * @param featureId		feature ID alias
     * @param fidWord		corresponding magic word ID.
     */
    public void saveFidAlias(String featureId, String fidWord) {
        this.featureIdMap.put(featureId, fidWord);
    }

}
