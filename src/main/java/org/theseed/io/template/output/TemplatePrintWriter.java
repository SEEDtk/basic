/**
 *
 */
package org.theseed.io.template.output;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import org.theseed.basic.ParseFailureException;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;

/**
 * This is a template writer that simply echoes the template string to an output file.
 */
public class TemplatePrintWriter implements ITemplateWriter, AutoCloseable {

    // FIELDS
    /** output print writer */
    private PrintWriter writer;
    /** token counter */
    private int tokenCount;
    /** token encoder */
    private Encoding encoder;

    /**
     * Construct a template print writer for a specified output file.
     *
     * @param fileName	name of the output file
     *
     * @throws IOException
     */
    public TemplatePrintWriter(File fileName) throws IOException {
        this.writer = new PrintWriter(fileName);
        // Set up the token counter.
        this.tokenCount = 0;
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        this.encoder = registry.getEncoding(EncodingType.CL100K_BASE);
    }

    @Override
    public void write(String fileName, String key, String outString) throws IOException {
        // Write the string.
        this.writer.print(outString);
        // Write out an EOL if there is not already one in the string.
        if (outString.length() > 0 && ! outString.endsWith("\n"))
            this.writer.println();
        // Count the tokens.
        int tokens = this.encoder.countTokens(outString);
        this.tokenCount += tokens;
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
    public long getTokenCount() {
        return this.tokenCount;
    }

}
