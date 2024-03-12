/**
 *
 */
package org.theseed.io.template.output;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.StringTokenizer;

import org.theseed.basic.ParseFailureException;

/**
 * This is a template writer that simply echoes the template string to an output file.
 */
public class TemplatePrintWriter implements ITemplateWriter, AutoCloseable {

    // FIELDS
    /** output print writer */
    private PrintWriter writer;
    /** word counter */
    private int wordCount;

    /**
     * Construct a template print writer for a specified output file.
     *
     * @param fileName	name of the output file
     *
     * @throws IOException
     */
    public TemplatePrintWriter(File fileName) throws IOException {
        this.writer = new PrintWriter(fileName);
        this.wordCount = 0;
    }

    @Override
    public void write(String fileName, String key, String outString) throws IOException {
        // Write the string.
        this.writer.println(outString);
        // Count the words.
        StringTokenizer tokens = new StringTokenizer(outString);
        this.wordCount += tokens.countTokens();
    }

    @Override
    public void close() {
        this.writer.close();
    }

    @Override
    public void readChoiceLists(File fileName, String... fields) throws ParseFailureException {
        // Only global templates (that use the TemplateHashWriter) can create choice lists.
        throw new ParseFailureException("Cannot create choice lists for file-output templates.");
    }

    @Override
    public int getWordCount() {
        return this.wordCount;
    }

}
