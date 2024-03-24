/**
 *
 */
package org.theseed.io.template.cols;

import java.util.List;

import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;
import org.theseed.io.template.LineTemplate;

/**
 * This field expression takes two parameters:  The first is a column name and the second is text.  The expression will return
 * TRUE if the column contains the text (using an exact equality match) and FALSE otherwise.  In list or string context, it returns
 * 1 instead of TRUE and an empty string otherwise.
 *
 * @author Bruce Parrello
 *
 */
public class EqualTestFieldExpression extends FieldExpression {

    // FIELDS
    /** input column to test */
    private int colIdx;
    /** value to test against the input column */
    private String testValue;
    /** singleton "1" list */
    private static final List<String> TRUE_LIST = List.of("1");
    /** empty list */
    private static final List<String> EMPTY_LIST = List.of();

    /**
     * Construct an equality-test field expression.
     *
     * @param master	master line template
     * @param inStream	current input stream
     * @param parms		array of parameters (should be 2)
     *
     * @throws ParseFailureException
     */
    public EqualTestFieldExpression(LineTemplate master, FieldInputStream inStream, String[] parms) throws ParseFailureException {
        super(master);
        if (parms.length != 2)
            throw new ParseFailureException("EQ function must have exactly two parameters.");
        // Get the column index for the first parameter.
        this.colIdx = master.findField(parms[0], inStream);
        // Get the test value.
        this.testValue = parms[1];
    }

    @Override
    public boolean eval(FieldInputStream.Record line) {
        String value = line.get(this.colIdx);
        return (testValue.contentEquals(value));
    }

    @Override
    public List<String> getList(FieldInputStream.Record line) {
        return (this.eval(line) ? TRUE_LIST : EMPTY_LIST);
    }

    @Override
    public String get(FieldInputStream.Record line) {
        return (this.eval(line) ? "1" : "");
    }

    @Override
    public String getName() {
        return "eq";
    }

}
