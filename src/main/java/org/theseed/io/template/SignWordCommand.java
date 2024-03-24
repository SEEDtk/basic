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
 * This command takes four parameters.  The first parameter is a field expression that is converted to a floating-point
 * number.  Depending on the sign of the parameter, the second (negative), third (zero), or fourth (positive) literal
 * string is emitted.
 *
 * @author Bruce Parrello
 *
 */
public class SignWordCommand extends PrimitiveTemplateCommand {

    // FIELDS
    /** input field expression */
    private FieldExpression floatExpression;
    /** array of choices (negative, zero, positive) */
    private String[] choices;

    /**
     * Construct a sign-based selection command.
     *
     * @param template	controlling master template
     * @param inStream	data input stream
     * @param parms		parameter string
     * @throws ParseFailureException
     */
    public SignWordCommand(LineTemplate template, FieldInputStream inStream, String parms) throws ParseFailureException {
        super(template);
        // Get the four parameters.
        String[] pieces = StringUtils.split(parms, ':');
        if (pieces.length != 4)
            throw new ParseFailureException("Exactly four parameters required for a signWord command.");
        // Compile the conditional expression.
        this.floatExpression = FieldExpression.compile(template, inStream, pieces[0]);
        // Save the choice strings.
        this.choices = new String[] { pieces[1], pieces[2], pieces[3] };
    }

    @Override
    protected String translate(Record line) {
        // Get the condition value.
        String condValue = this.floatExpression.get(line);
        double floatValue;
        // We will fold anything invalid into 0.
        try {
            floatValue = Double.valueOf(condValue);
        } catch (NullPointerException | NumberFormatException e) {
            floatValue = Double.NaN;
        }
        if (Double.isNaN(floatValue)) floatValue = 0.0;
        // Check the sign.
        String retVal;
        if (floatValue < 0.0)
            retVal = this.choices[0];
        else if (floatValue == 0.0)
            retVal = this.choices[1];
        else
            retVal = this.choices[2];
        return retVal;
    }

    @Override
    protected String getName() {
        return "signWord";
    }

}
