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
 * This command generates a useful feature ID from a FIG ID and a functional assignment (product).
 * The parameters are a string field expression for the feature ID and an optional string field
 * expression for the assignment.  If the assignment is omitted, the feature ID must already have
 * a useful ID generated for it.
 *
 * @author Bruce Parrello
 *
 */
public class FidCommand extends PrimitiveTemplateCommand {

    // FIELDS
    /** feature ID */
    private FieldExpression featureId;
    /** feature function, or NULL if none */
    private FieldExpression function;

    /**
     * Compile a command to return a magic-word feature ID for the current genome.
     *
     * @param template	master line template
     * @param inStream	current input stream
     * @param parms		parameter string
     *
     * @throws ParseFailureException
     */
    public FidCommand(LineTemplate template, FieldInputStream inStream, String parms) throws ParseFailureException {
        super(template);
        // Parse the parameters.  The first is the feature ID.  The second, optional one is the function.
        String[] pieces = StringUtils.split(parms, ":");
        if (pieces.length >= 1) {
            this.featureId = FieldExpression.compile(template, inStream, pieces[0]);
            if (pieces.length == 2)
                this.function = FieldExpression.compile(template, inStream, pieces[1]);
            else if (pieces.length > 2)
                throw new ParseFailureException("Too many parameters for feature-ID command.");
            else
                this.function = null;
        } else
            throw new ParseFailureException("Feature-ID command requires parameters.");
    }

    @Override
    protected String translate(Record line) {
        String retVal;
        LineTemplate master = this.getMasterTemplate();
        String fid = this.featureId.get(line);
        try {
            if (this.function == null) {
                // Here we have a simple feature-ID lookup.
                retVal = master.getMagicFid(fid);
            } else {
                // Here we could be generating the feature ID for the first time.
                String fun = this.function.get(line);
                retVal = master.getMagicFid(fid, fun);
            }
        } catch (ParseFailureException e) {
            // Emit the error description instead of a result.
            retVal = "{ ERROR: " + e.getMessage() + "}";
        }
        return retVal;
    }

    @Override
    protected String getName() {
        return "fid";
    }

}
