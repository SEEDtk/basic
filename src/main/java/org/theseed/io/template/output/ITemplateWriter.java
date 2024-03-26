/**
 *
 */
package org.theseed.io.template.output;

import java.io.File;
import java.io.IOException;

import org.theseed.basic.ParseFailureException;

/**
 * This interface must be supported by any object that handles template output.
 * It insures we can process a template output line given the file name, key,
 * and output string.
 */
public interface ITemplateWriter {

    /**
     * Output an expanded template string.
     *
     * @param fileName		input file base name
     * @param key			input line key value
     * @param outString		output expanded template string
     *
     * @throws IOException
     */
    public void write(String fileName, String key, String outString) throws IOException;

    /**
     * Insure all output is written and all I/O resources are freed.
     */
    public void close();

    /**
     * Read in a file to create choice lists.
     *
     * @param fileName	name of the input file
     * @param fields	array of field names to read the choices from
     *
     * @throws IOException
     * @throws ParseFailureException
     */
    public void readChoiceLists(File fileName, String... fields) throws IOException, ParseFailureException;

    /**
     * This uses a tokenizer to compute the number of tokens output.
     *
     * @return the number of tokens written
     */
    public long getTokenCount();

}
