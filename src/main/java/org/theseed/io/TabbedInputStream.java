/**
 *
 */
package org.theseed.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

/**
 * This is the field-input stream handler for a standard tab-delimited file with headers.  The fields
 * are all described in the first (header) line, and fields not present cause an IOException.
 *
 * @author Bruce Parrello
 *
 */
public class TabbedInputStream extends FieldInputStream {

    // FIELDS
    /** number of input columns */
    private int cols;

    public TabbedInputStream(File inputFile) throws IOException {
        super(inputFile);
        if (! this.hasNextLine())
            throw new IOException("Input file " + inputFile + " is empty.");
        this.initialize();
    }

    public TabbedInputStream(InputStream inputStream) throws IOException {
        super(inputStream);
        if (! this.hasNextLine())
            throw new IOException("Tab-delimited input stream is empty.");
        this.initialize();
    }

    /**
     * Read the header line and initialize the field-name list for this file.
     *
     * @throws IOException
     */
    private void initialize() throws IOException {
        // Get the field-name list.
        String header = this.nextLine();
        String[] fields = StringUtils.splitPreserveAllTokens(header, '\t');
        this.cols = fields.length;
        // Add the field names in order.
        Arrays.stream(fields).forEach(x -> this.addFieldName(x));
    }

    @Override
    public Record next() {
        String line = this.nextLine();
        String[] fields = StringUtils.splitPreserveAllTokens(line, '\t');
        Record retVal = this.new Record(fields);
        // Insure the number of fields is sufficient.
        for (int i = fields.length; i < this.cols; i++)
            retVal.addField();
        return retVal;
    }

    @Override
    public int findField(String fieldName) throws IOException {
        int retVal = this.findColumn(fieldName);
        if (retVal < 0)
            throw new IOException("No column named \"" + fieldName + "\" found in input stream.");
        return retVal;
    }

}
