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
import org.theseed.io.FieldInputStream;

/**
 * @author Bruce Parrello
 *
 */
class TestFidMapper {

    private static final Set<String> SPECIAL = Set.of("fig|83332.12.peg.2277", "fig|83332.12.rna.28",
            "fig|83332.12.peg.3669", "fig|83332.12.peg.1941", "fig|83332.12.rna.31");

    @Test
    void testFidMapper() throws IOException {
        FidMapper fidMapper = new FidMapper();
        // Set up a genome.
        fidMapper.setup("83332.12", "Mycobacterium tuberculosis H37Rv");
        // Insure we find the genome.
        String gWords = fidMapper.getGenomeIdWord("83332.12", "Mycobacterium tuberculosis H37Rv");
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
                    String fidWord = fidMapper.getMagicFid(fid, product);
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
            String actual = fidMapper.getMagicFid(fid, "unknown feature");
            assertThat(fid, actual, equalTo(expected));
        }
    }

}
