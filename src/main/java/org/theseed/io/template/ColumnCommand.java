/**
 *
 */
package org.theseed.io.template;

import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;
import org.theseed.io.FieldInputStream.Record;
import org.theseed.io.template.cols.FieldExpression;

/**
 * This is the most basic and common command.  It outputs the content of a field expression computed
 * on the current line.
 *
 * @author Bruce Parrello
 *
 */
public class ColumnCommand extends PrimitiveTemplateCommand {

    // FIELDS
    /** index of column to output */
    private FieldExpression field;
    /** default expected output size */
    private static int DEFAULT_COLUMN_SIZE = 20;

    /**
     * Construct the column command.
     *
     * @param template		master template
     * @param expression	field expression to output
     * @param inStream		source input stream
     *
     * @throws ParseFailureException
     */
    public ColumnCommand(LineTemplate template, String expression, FieldInputStream inStream) throws ParseFailureException {
        super(template);
        this.setEstimatedLength(DEFAULT_COLUMN_SIZE);
        // Compile the field expression.
        this.field = FieldExpression.compile(template, inStream, expression);
    }

    @Override
    protected String translate(Record line) {
        return this.field.get(line);
    }

    @Override
    protected String getName() {
        return this.field.getName();
    }

}
