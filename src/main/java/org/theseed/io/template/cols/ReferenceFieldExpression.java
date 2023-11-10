/**
 *
 */
package org.theseed.io.template.cols;

import java.util.List;

import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;
import org.theseed.io.FieldInputStream.Record;
import org.theseed.io.template.LineTemplate;

/**
 * This is the primitive field expression, which simply accesses a column.
 *
 * @author Bruce Parrello
 *
 */
public class ReferenceFieldExpression extends FieldExpression {

    // FIELDS
    /** index of the key column */
    private int colIdx;
    /** name of the key column */
    private String colName;

    /**
     * Compile a column reference.
     *
     * @param template		controlling master template
     * @param inStream		field input stream
     * @param expression	expression containing field name
     */
    public ReferenceFieldExpression(LineTemplate template, FieldInputStream inStream, String expression)
            throws ParseFailureException {
        super(template);
        this.colName = expression;
        this.colIdx = template.findField(this.colName, inStream);
    }

    @Override
    public boolean eval(Record line) {
        return line.getFlag(this.colIdx);
    }

    @Override
    public List<String> getList(Record line) {
        return line.getList(this.colIdx);
    }

    @Override
    public String get(Record line) {
        return line.get(this.colIdx);
    }

    @Override
    public String getName() {
        return this.colName;
    }

}
