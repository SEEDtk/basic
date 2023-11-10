/**
 *
 */
package org.theseed.io.template;

import java.io.IOException;

import org.theseed.io.FieldInputStream;
import org.theseed.io.FieldInputStream.Record;

/**
 * This is a simple command that displays strand codes.  The column containing the strand code will contain
 * either "+" or "-", and the output will be either "the plus (+) strand" or "the minus (-) strand".
 *
 * @author Bruce Parrello
 *
 */
public class StrandCommand extends PrimitiveTemplateCommand {

    // FIELDS
    /** column index for field to use */
    private int colIdx;

    /**
     * Create a strand command.
     *
     * @param lineTemplate	controlling template builder
     * @param inStream		input stream containing fields
     * @param colName		name of the column containing the strand code
     *
     * @throws IOException
     */
    public StrandCommand(LineTemplate lineTemplate, FieldInputStream inStream, String colName) throws IOException {
        super(lineTemplate);
        this.colIdx = inStream.findField(colName);
    }

    @Override
    protected String translate(Record line) {
        // Get the strand code.
        String strandCode = line.get(this.colIdx);
        String retVal;
        if (strandCode.contentEquals("+"))
            retVal = "the plus (+) strand";
        else if (strandCode.contentEquals("-"))
            retVal = "the minus (-) strand";
        else
            retVal = "an unknown strand";
        return retVal;
    }

    @Override
    protected String getName() {
        return "strand";
    }

}
