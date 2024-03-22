/**
 *
 */
package org.theseed.io.template;

import org.apache.commons.lang3.StringUtils;
import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;
import org.theseed.io.FieldInputStream.Record;
import org.theseed.io.template.cols.FieldExpression;

/**
 * This command emits no text, but stores the genome ID and name in the feature ID mapper for the
 * next group of features.
 *
 * @author Bruce Parrello
 *
 */
public class GStoreCommand extends PrimitiveTemplateCommand {

    // FIELDS
    /** genome ID field expression */
    private FieldExpression genomeId;
    /** genome name field expression */
    private FieldExpression genomeName;

    /**
     * Compile a command to store the genome identifier word in the feature ID mapper.
     *
     * @param template		controlling line template
     * @param inStream		input data stream
     * @param parms			parameter string (2 required parameters)
     *
     * @throws ParseFailureException
     */
    public GStoreCommand(LineTemplate template, FieldInputStream inStream, String parms) throws ParseFailureException {
        super(template);
        // Parse the parameter string.
        String[] pieces = StringUtils.split(parms, ":");
        if (pieces.length != 2)
            throw new ParseFailureException("GStore requires two parameters.");
        // Save the parameters.
        this.genomeId = FieldExpression.compile(template, inStream, pieces[0]);
        this.genomeName = FieldExpression.compile(template, inStream, pieces[1]);
    }

    @Override
    protected String translate(Record line) {
        // We return an empty string, but we must update the FID mapper.
        LineTemplate master = this.getMasterTemplate();
        // Get the genome ID and name.
        String gID = this.genomeId.get(line);
        String gName = this.genomeName.get(line);
        master.storeMapperGenome(gID, gName);
        return "";
    }

    @Override
    protected String getName() {
        return "gStore";
    }

}
