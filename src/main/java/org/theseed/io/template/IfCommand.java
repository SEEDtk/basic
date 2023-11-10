/**
 *
 */
package org.theseed.io.template;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;
import org.theseed.io.FieldInputStream.Record;
import org.theseed.io.template.cols.FieldExpression;

/**
 * This is the basic IF command.  The command has as its sole parameter a field expression.
 * It executes its THEN-block when the expression evaluates to TRUE and executes the ELSE
 * block when the column evaluates to FALSE.
 */
public class IfCommand extends TemplateCommand {

    // FIELDS
    /** condition expressions */
    private FieldExpression[] fields;
    /** then-clause */
    private TemplateCommand thenClause;
    /** else-clause */
    private TemplateCommand elseClause;

    /**
     * Construct the IF-block.
     *
     * @param template	controlling master template
     * @param inStream	source input stream
     * @param parms		parameter, consisting of a column name
     *
     * @throws ParseFailureException
     */
    public IfCommand(LineTemplate template, FieldInputStream inStream, String parms) throws ParseFailureException {
        super(template);
        // Get the indices of all the condition columns.
        String[] cols = StringUtils.split(parms, ':');
        this.fields = new FieldExpression[cols.length];
        for (int i = 0; i < this.fields.length; i++)
            this.fields[i] = FieldExpression.compile(template, inStream, cols[i]);
        // Both clauses are initialized to null.  The first subcommand is THEN, the second is ELSE, and
        // any others are an error.
        this.thenClause = null;
        this.elseClause = null;
    }

    @Override
    protected void addCommand(TemplateCommand command) throws ParseFailureException {
        // Note that the estimated length will be the larger of the two estimates.
        if (this.thenClause == null) {
            this.thenClause = command;
            this.setEstimatedLength(command.getEstimatedLength());
        } else if (this.elseClause == null) {
            this.elseClause = command;
            this.mergeEstimatedLength(command);
        } else
            throw new ParseFailureException("Too many clauses for IF.");
    }

    @Override
    protected String translate(Record line) {
        String retVal = "";
        // Evaluate the condition.
        boolean flag = Arrays.stream(this.fields).allMatch(x -> x.eval(line));
        // Execute the appropriate clause if it exists.
        if (flag && this.thenClause != null)
            retVal = this.thenClause.translate(line);
        else if (! flag && this.elseClause != null)
            retVal = this.elseClause.translate(line);
        return retVal;
    }

    @Override
    protected String getName() {
        return "if";
    }

}
