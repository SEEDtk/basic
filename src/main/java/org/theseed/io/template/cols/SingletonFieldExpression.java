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
 * This function returns TRUE if the parameter is a single-element list and FALSE otherwise.  It is intended for use in IF-constructs
 * when it is necessary to produce different results depending on whether or not a list has multiple elements.
 *
 * In list mode the function returns a list containing only the first element.  In string mode the function returns the first element of the list.
 *
 * @author Bruce Parrello
 *
 */
public class SingletonFieldExpression extends FieldExpression {

    // FIELDS
    /** index of the column containing the list to check */
    private int colIdx;

    /**
     * Construct a singleton-list-test field expression.
     *
     * @param master	master line template
     * @param inStream	current input stream
     * @param parms		array of parameters (should be 2)
     *
     * @throws ParseFailureException
     */
    public SingletonFieldExpression(LineTemplate master, FieldInputStream inStream, String[] parms) throws ParseFailureException {
        super(master);
        // Insure we have exactly one parameter.
        if (parms.length != 1)
            throw new ParseFailureException("Singleton function must have exactly one parameter.");
        this.colIdx = master.findField(parms[0], inStream);
    }

    @Override
    public boolean eval(Record line) {
        List<String> vList = line.getList(this.colIdx);
        return (vList.size() == 1);
    }

    @Override
    public List<String> getList(Record line) {
        List<String> vList = line.getList(this.colIdx);
        List<String> retVal;
        if (vList.size() < 1)
            retVal = vList;
        else
            retVal = List.of(vList.get(0));
        return retVal;
    }

    @Override
    public String get(Record line) {
        List<String> vList = line.getList(this.colIdx);
        String retVal;
        if (vList.size() < 1)
            retVal = "";
        else
            retVal = vList.get(0);
        return retVal;
    }

    @Override
    public String getName() {
        return "singleton";
    }

}
