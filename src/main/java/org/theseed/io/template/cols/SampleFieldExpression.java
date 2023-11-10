/**
 *
 */
package org.theseed.io.template.cols;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream.Record;
import org.theseed.io.template.LineTemplate;

/**
 * This function is used to get a random list of names of a specified choice type.  The parameters are the
 * choice type name and the number of results desired.  The results will not have any repeats.
 *
 * @author Bruce Parrello
 *
 */
public class SampleFieldExpression extends FieldExpression {

    // FIELDS
    /** choice type code */
    private Set<String> choices;
    /** number of choices to use */
    private int count;

    /**
     * Parse the parameters and initialize the sampling expression.
     *
     * @param template	controlling master template
     * @param parms		array of parameters (choice type, count)
     *
     * @throws ParseFailureException
     */
    public SampleFieldExpression(LineTemplate template, String[] parms) throws ParseFailureException {
        super(template);
        if (parms.length  != 2)
            throw new ParseFailureException("\"sample\" function requires exactly two parameters.");
        this.choices = template.getChoices(parms[0]);
        if (this.choices == null)
            throw new ParseFailureException("Invalid choice type \"" + parms[0] + "\" in sample function.");
        try {
            this.count = Integer.valueOf(parms[1]);
        } catch (NumberFormatException e) {
            throw new ParseFailureException("Invalid sample count \"" + parms[1] + "\".");
        }
        if (this.count <= 0)
            throw new ParseFailureException("Sample count of " + parms[1] + " is invalid:  1 or greater required.");
    }

    @Override
    public boolean eval(Record line) {
        // We evaluate to TRUE if we know there will be at least one output list item.
        return this.choices.size() > 0 && this.count > 0;
    }

    @Override
    public List<String> getList(Record line) {
        // Get the choices into a list and shuffle them.
        var list = new ArrayList<String>(this.choices);
        int n = this.count;
        if (n > list.size()) n = list.size();
        this.getTemplate().shuffle(list, n);
        List<String> retVal = list.subList(0, n);
        return retVal;
    }

    @Override
    public String get(Record line) {
        return StringUtils.join(this.getList(line), ", ");
    }

    @Override
    public String getName() {
        return "sample";
    }

}
