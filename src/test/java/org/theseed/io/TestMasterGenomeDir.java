/**
 *
 */
package org.theseed.io;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

/**
 * @author Bruce Parrello
 *
 */
class TestMasterGenomeDir {

    /** expected directory names */
    private static final Set<String> EXPECTED = Set.of("11159.6", "11159.7", "11159.8", "11159.9", "11168.4");

    @Test
    void testMasterDir() {
        MasterGenomeDir testDir = new MasterGenomeDir(new File("data"));
        assertThat(testDir.size(), equalTo(EXPECTED.size()));
        // Test iteration.
        for (File dir : testDir) {
            assertThat(dir.toString(), dir.isDirectory(), equalTo(true));
            String name = dir.getName();
            assertThat(name, in(EXPECTED));
        }
        // Test streams.
        Set<String> actual = testDir.stream().map(x-> x.getName()).collect(Collectors.toSet());
        assertThat(actual, equalTo(EXPECTED));

    }

}
