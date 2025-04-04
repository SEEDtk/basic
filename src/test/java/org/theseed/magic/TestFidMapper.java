/**
 *
 */
package org.theseed.magic;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;
import org.theseed.io.LineReader;

/**
 * @author Bruce Parrello
 *
 */
class TestFidMapper {

    private static final Set<String> SPECIAL = Set.of("fig|83332.12.peg.2277", "fig|83332.12.rna.28",
            "fig|83332.12.peg.3669", "fig|83332.12.peg.1941", "fig|83332.12.rna.31");

    @Test
    void testMagicMapper() throws IOException, ParseFailureException {
        FidMapper fidMapper = new MagicFidMapper();
        // Set up a genome.
        fidMapper.setup("83332.12", "Mycobacterium tuberculosis H37Rv");
        // Insure we find the genome.
        String gWords = fidMapper.getNewGenomeId("83332.12", "Mycobacterium tuberculosis H37Rv");
        assertThat(gWords, equalTo("MycoTubeH37r"));
        // Now try this with all the features.  Our main goal is to insure each feature has a unique ID.
        Set<String> found = new HashSet<String>();
        Map<String, String> saved = new HashMap<String, String>();
        // This will store the magic words for a few known features.
        File testFile = new File("data", "all_feature.json");
        try (FieldInputStream inStream = FieldInputStream.create(testFile)) {
            int fidIdx = inStream.findField("patric_id");
            int prodIdx = inStream.findField("product");
            for (var record : inStream) {
                String fid = record.get(fidIdx);
                // Note we have to skip the foreign features.
                if (! StringUtils.isBlank(fid)) {
                    String product = record.get(prodIdx);
                    String fidWord = fidMapper.getNewFid(fid, product);
                    boolean isNew = found.add(fidWord);
                    assertThat(fid + " (" + fidWord + ")", isNew, equalTo(true));
                    if (SPECIAL.contains(fid))
                        saved.put(fid, fidWord);
                }
            }
        }
        // Verify that the features remain the same.
        for (var savedEntry : saved.entrySet()) {
            String fid = savedEntry.getKey();
            String expected = savedEntry.getValue();
            String actual = fidMapper.getNewFid(fid, "unknown feature");
            assertThat(fid, actual, equalTo(expected));
        }
    }

	// In our test genome mapping, we have three genomes mapping to 1.1 and two to 2.1. Genomes that
	// map to themselves do not have to be included.
	private static final Map<String, String> GMAP = Map.of("1.2", "1.1", "1.3", "1.1", "2.2", "2.1");

	@Test
	void testComboMapper() throws IOException, ParseFailureException {
		FidMapper fidMapper = new CombinationFidMapper(GMAP);
		try (LineReader inStream = new LineReader(new File("data", "comboMapper.txt"))) {
			String currGenomeId = null;
			Map<String, String> checkMap = new HashMap<String, String>(50);
			for (var line : inStream) {
				// Each record is three columns. If the first column has a genome ID, it's a new
				// genome. Otherwise it's a feature mapping.
				String[] cols = StringUtils.split(line, '\t');
				if (! StringUtils.isBlank(cols[0])) {
					// Here we have a new genome. Second column is the genome name.
					String genomeId = cols[0];
					String genomeName = cols[1];
					// If we have a previous genome, check its mappings.
					this.checkGenomeFids(fidMapper, currGenomeId, checkMap);
					checkMap.clear();
					// Now set up the new genome.
					currGenomeId = GMAP.getOrDefault(genomeId, genomeId);
					fidMapper.setup(genomeId, genomeName);
					assertThat(genomeId, fidMapper.getNewGenomeId(genomeId), equalTo(currGenomeId));
				} else {
					// Here we have a feature. Second column is the expected feature ID. Third is the role.
					String fid = cols[1];
					String newFid = cols[2];
					String role = cols[3];
					String computed = fidMapper.getNewFid(fid, role);
					assertThat(fid, computed, equalTo(newFid));
					checkMap.put(fid, computed);
				}
			}
			// Check the feature map for the last genome.
			this.checkGenomeFids(fidMapper, currGenomeId, checkMap);
			// Verify the genome mappings.
			for (var gEntry : GMAP.entrySet()) {
				String gId = gEntry.getKey();
				assertThat(gId, fidMapper.getNewGenomeId(gId), equalTo(gEntry.getValue()));
			}
		}
	}

	/**
	 * Verify that the feature mappings have been saved correctly.
	 *
	 * @param fidMapper		feature-mapper being tested
	 * @param currGenomeId	ID of the mapped genome (if NULL, the feature-mapper is empty)
	 * @param checkMap		saved feature mappings for verification
	 */
	protected void checkGenomeFids(FidMapper fidMapper, String currGenomeId, Map<String, String> checkMap) {
		if (currGenomeId != null) {
			for (var checkEntry : checkMap.entrySet()) {
				String fid = checkEntry.getKey();
				assertThat(fid, fidMapper.getNewFid(fid), equalTo(checkEntry.getValue()));
			}
		}
	}

}
