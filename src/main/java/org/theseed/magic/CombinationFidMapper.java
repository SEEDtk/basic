/**
 *
 */
package org.theseed.magic;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import org.theseed.basic.ParseFailureException;
import org.theseed.counters.CountMap;

/**
 * This feature ID mapper is used to manage the genome combination process. It takes as input a map
 * from original genome IDs to new genome IDs.
 *
 * The combination process handles a genome that has been split into multiple genomes, a common
 * problem with viruses. The old genome data is combined into the new genome, and the features
 * are renumbered.
 *
 * For each new genome we keep a record of the last feature number used for each feature type. This
 * is managed using a nested utility class-- GenomeTracker.
 *
 * @author Bruce Parrello
 *
 */
public class CombinationFidMapper extends FidMapper {

	// FIELDS
	/** genome ID to tracker map */
	private Map<String, GenomeTracker> trackerMap;
	/** genome tracker for the current genome */
	private GenomeTracker currTracker;

	/**
	 * This class contains the target genome for a combination process and tracks the
	 * the number of features it has of each type.
	 */
	protected static class GenomeTracker {

		/** target genome ID */
		private String targetGenomeId;
		/** map of feature types to counts */
		private CountMap<String> typeCounts;

		/**
		 * Create a genome tracker for a genome.
		 *
		 * @param newId		target genome ID
		 */
		protected GenomeTracker(String newId) {
			this.targetGenomeId = newId;
			this.typeCounts = new CountMap<String>();
		}

		/**
		 * Generate a new feature ID.
		 *
		 * @param type		feature type
		 */
		protected String computeNewFid(String type) {
			int newNum = this.typeCounts.count(type);
			String retVal = "fig|" + targetGenomeId + "." + type + "." + Integer.toString(newNum);
			return retVal;
		}

		/**
		 * @return the target genome ID
		 */
		protected String getTargetGenomeId() {
			return this.targetGenomeId;
		}

	}

	/**
	 * Create a combination mapper from a genome ID map.
	 *
	 * @param gMap	map from old genome IDs to new genome IDs
	 */
	public CombinationFidMapper(Map<String, String> gMap) {
		// First we build the tracker map from the genome map. The same tracker is associated with the
		// target genome ID and the source genome ID. This is important, because we want all sources with
		// the same target to point to the same tracker.
		this.trackerMap = new HashMap<String, GenomeTracker>(gMap.size() * 2);
		for (var gEntry : gMap.entrySet()) {
			String sourceId = gEntry.getKey();
			String targetId = gEntry.getValue();
			GenomeTracker targetTracker = this.trackerMap.computeIfAbsent(targetId, x -> new GenomeTracker(x));
			this.trackerMap.put(sourceId, targetTracker);
		}
		// Clear the current-tracker cache.
		this.currTracker = null;
	}

	@Override
	protected void setupGenome(String genomeId, String genomeName) {
		// Here we are starting a new genome. We retrieve the new genome's tracker.
		// Note that if the genome ID is not found, we map it to itself.
		this.currTracker = this.trackerMap.computeIfAbsent(genomeId, x -> new GenomeTracker(x));
		log.info("Mapping genome {} into {}.", genomeId, this.currTracker.targetGenomeId);
	}

	@Override
	protected String createNewGenomeId(String genomeId, String genomeName) {
		// The new genome ID is always the mapped genome ID.
		GenomeTracker tracker = this.trackerMap.get(genomeId);
		String retVal = (tracker == null ? genomeId : tracker.getTargetGenomeId());
		return retVal;
	}

	@Override
	protected String computeNewFeatureId(Matcher m, String function, String newGenomeId) throws ParseFailureException {
		// Get the match group for the feature type.
		String fType = m.group(2);
		// Compute the new feature ID using the genome tracker.
		String retVal = this.currTracker.computeNewFid(fType);
		log.debug("Feature mapping of {} to {}.", m.group(), retVal);
		return retVal;
	}

}
