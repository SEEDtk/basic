/**
 *
 */
package org.theseed.io.template.cols;

import java.io.IOException;
import java.util.regex.Pattern;

import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;
import org.theseed.io.template.LineTemplate;

import java.util.List;
import java.util.regex.Matcher;

/**
 * This is the base class for a field expression.  Field expressions are used in conditionals, and the
 * expression class must be able to parse the expression and evaluate it to TRUE or FALSE.
 *
 * The simplest field expression is simply a column name.  It is also, however, possible to have a function.
 * The function consists of the function name, a left parenthesis, a comma-delimited parameter list, and a
 * right parenthesis.
 *
 * @author Bruce Parrello
 *
 */
public abstract class FieldExpression {

    // FIELDS
    /** controlling line template */
    private LineTemplate template;
    /** match pattern for a function expression */
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("([a-z]\\w+)\\((.+)\\)");
    /** splitter for function parameters */
    private static final Pattern SPLIT_PATTERN = Pattern.compile("\\s*,\\s*");


    /**
     * Create a field expression from an expression string.
     *
     * @param template		master line template
     * @param inStream		relevant field input stream
     * @param exp			expression string
     *
     * @throws ParseFailureExcpression
     * @throws IOException
     */
    public static FieldExpression compile(LineTemplate template, FieldInputStream inStream, String expression)
            throws ParseFailureException {
        FieldExpression retVal;
        Matcher m = FUNCTION_PATTERN.matcher(expression);
        if (! m.matches()) {
            // Here we have a raw field name.
            retVal = new ReferenceFieldExpression(template, inStream, expression);
        } else {
            // Here we have a function invocation.  Note that functions aren't recursive yet: the parameters
            // are treated as strings for interpretation.
            String[] parms = SPLIT_PATTERN.split(m.group(2));
            // Process according to the function name.
            switch (m.group(1)) {
            case "include" :
                retVal = new IncludeFieldExpression(template, inStream, parms);
                break;
            case "sample" :
                retVal = new SampleFieldExpression(template, parms);
                break;
            default :
                throw new ParseFailureException("Invalid field-expression function \"" + m.group(1) + "\".");
            }
        }
        return retVal;
    }

    /**
     * Construct a new field expression.
     *
     * @param master	master line template
     */
    public FieldExpression(LineTemplate master) {
        this.template = master;
    }

    /**
     * @return TRUE if the expression is TRUE for the specified input line, else FALSE
     *
     * @param line		input line being evaluated
     */
    public abstract boolean eval(FieldInputStream.Record line);

    /**
     * @return the value of the field expression as a list for the specified input line
     *
     * @param line		input line being evaluated
     */
    public abstract List<String> getList(FieldInputStream.Record line);

    /**
     * @return the value of the field expression as a scalar string for the specified input line
     *
     * @param line		input line being evaluated
     */
    public abstract String get(FieldInputStream.Record line);

    /**
     * @return the master template
     */
    protected LineTemplate getTemplate() {
        return this.template;
    }

    /**
     * @return the name of this field expression
     */
    public abstract String getName();

}
