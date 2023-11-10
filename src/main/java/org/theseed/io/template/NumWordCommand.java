/**
 *
 */
package org.theseed.io.template;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;
import org.theseed.io.FieldInputStream.Record;
import org.theseed.io.template.cols.FieldExpression;

/**
 * This command is used to select between a singular word and a plural word given a list field expression.
 *
 * @author Bruce Parrello
 *
 */
public class NumWordCommand extends PrimitiveTemplateCommand {

    // FIELDS
    /** index of the column containing the list */
    private FieldExpression listExpression;
    /** singular form of word */
    private String singular;
    /** plural form of word */
    private String plural;
    /** separator string, or NULL if the field is a list */
    private String separator;

    /**
     * Construct a new number-word command.
     *
     * @param template	controlling master template
     * @param inStream	field input stream
     * @param parms		parameter string:  list field expression, singular word, plural word, optional delimiter
     *
     * @throws ParseFailureException
     */
    public NumWordCommand(LineTemplate template, FieldInputStream inStream, String parms) throws ParseFailureException {
        super(template);
        // Parse the parameters.
        String[] pieces = StringUtils.split(parms, ":");
        if (pieces.length < 3)
            throw new ParseFailureException("NumWord command requires at least three parameters.");
        else {
            // Compile the input expression.
            this.listExpression = FieldExpression.compile(template, inStream, pieces[0]);
            if (pieces.length < 4)
                this.separator = null;
            else if (pieces.length == 4)
                this.separator = pieces[3];
            else
                throw new ParseFailureException("Too many parameters on list command.");
            this.singular = pieces[1];
            this.plural = pieces[2];
        }
    }

    @Override
    protected String translate(Record line) {
        // Get the column data.
        List<String> pieces;
        if (this.separator == null)
            pieces = this.listExpression.getList(line);
        else {
            String value = this.listExpression.get(line);
            pieces = Arrays.asList(StringUtils.splitByWholeSeparator(value, this.separator));
        }
        String retVal;
        if (pieces.size() == 1)
            retVal = this.singular;
        else
            retVal = this.plural;
        return retVal;
    }

    @Override
    protected String getName() {
        return "numword";
    }

}
