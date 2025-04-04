/**
 *
 */
package org.theseed.magic;

import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;
import org.theseed.basic.ParseFailureException;
import org.theseed.counters.CountMap;
import org.theseed.genome.GenomeName;
import org.theseed.genome.GenomeNameMap;
import org.theseed.proteins.Function;
import org.theseed.proteins.FunctionMap;

/**
 * This is a feature-mapper that stores magic IDs. Magic genome IDs are unique to each individual
 * genome, and the feature IDs are generated from the genome ID and the feature role, with an
 * optional sequence number.
 *
 * @author Bruce Parrello
 *
 */
public class MagicFidMapper extends FidMapper {

	// FIELDS
    /** functional assignment name map */
    private FunctionMap functionMap;
    /** count of number of times each function word was used */
    private CountMap<String> funCounters;
    /** count of number of times each genome name was used */
    private CountMap<String> nameCounters;
    /** genome name map */
    private GenomeNameMap genomeNameMap;

	/**
	 * Construct a magic-word feature mapper.
	 */
	public MagicFidMapper() {
		// Set up the function map that insures roles get the same magic words.
		this.functionMap = new FunctionMap();
		// Set up the magic-word map for genomes.
        this.genomeNameMap = new GenomeNameMap();
        // Clear the suffix counters.
		this.funCounters = new CountMap<String>();
		this.nameCounters = new CountMap<String>();
	}

	@Override
	protected void setupGenome(String genomeId, String genomeName) {
		// Erase the old function counts.
		this.funCounters.deleteAll();
	}

	@Override
	protected String createNewGenomeId(String genomeId, String genomeName) {
        // Here we have a new genome ID, so we have to generate.
        GenomeName gNameObject = this.genomeNameMap.findOrInsert(genomeName);
        // Get the genome ID.  We may need to suffix it.
        String retVal = gNameObject.getId();
        final int count = this.nameCounters.count(retVal);
        retVal = suffixCount(retVal, count);
        this.genomeNameMap.put(gNameObject);
        return retVal;
	}

	@Override
	protected String computeNewFeatureId(Matcher m, String function, String newGenomeId) throws ParseFailureException {
		String retVal = null;
        if (m.group(2).equals("peg")) {
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
                log.error("Zero-length function word found for function \"{}\" in feature {}.", pegFunction, m.group());
            funWord = FidMapper.suffixCount(funWord, count);
            // Now build the whole string.
            retVal = newGenomeId + funWord;
        } else {
            // Here we have a not-peg.  We put less work into these.
            retVal = newGenomeId + StringUtils.capitalize(m.group(2)) + m.group(3);
        }
        return retVal;
	}

}
