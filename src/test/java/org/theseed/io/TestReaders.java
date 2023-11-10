/**
 *
 */
package org.theseed.io;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * @author Bruce Parrello
 *
 */
class TestReaders {

    /**
     * test the line reader
     *
     * @throws IOException
     */
    @Test
    public void testLineReader() throws IOException {
        // Start with an empty file.
        File emptyFile = new File("data", "empty.fa");
        try (LineReader reader = new LineReader(emptyFile)) {
            assertThat(reader.hasNext(), equalTo(false));
        }
        // Try a regular file as a stream.
        InputStream inStream = new FileInputStream(new File("data", "lines.txt"));
        try (LineReader reader = new LineReader(inStream)) {
            assertThat(reader.hasNext(), equalTo(true));
            List<String> lines = new ArrayList<String>(5);
            for (String line : reader)
                lines.add(line);
            assertThat(lines.size(), equalTo(3));
            assertThat(lines.get(0), equalTo("line 1"));
            assertThat(lines.get(1), equalTo("line 2"));
            assertThat(lines.get(2), equalTo("line 3"));
        }
        File badFile = new File("data", "nosuchfile.bad");
        try (LineReader reader = new LineReader(badFile)) {
            assertThat("Opened an invalid file.", false, equalTo(true));
        } catch (IOException e) {
            assertThat(true, equalTo(true));
        }
    }

}
