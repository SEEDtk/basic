/**
 *
 */
package org.theseed.io.template.cols;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.theseed.basic.ParseFailureException;
import org.theseed.io.Attribute;
import org.theseed.io.FieldInputStream;
import org.theseed.io.FieldInputStream.Record;
import org.theseed.io.template.LineTemplate;

/**
 * This is a function-based field expression that returns the data found on the other side of a link
 * to the global cache.  The parameters are the source file name and the key column name.
 *
 * In a boolean context, the result is TRUE if the data exists.  In a list context, the result is all
 * the strings with the specified key from the specified master file.  In a string context, the result
 * is the first string in the list.
 *
 * @author Bruce Parrello
 *
 */
public class IncludeFieldExpression extends FieldExpression {

    // FIELDS
    /** index of the key column */
    private int colIdx;
    /** name of the source file */
    private String fileName;
    /** name of the key column */
    private String colName;

    /**
     * Construct a field-expression for retrieving strings from the global cache.
     *
     * @param template		master line template
     * @param inStream		field input stream containing the data
     * @param parms			array of parameter strings
     */
    public IncludeFieldExpression(LineTemplate template, FieldInputStream inStream, String[] parms)
            throws ParseFailureException {
        super(template);
        // Save the file name.
        this.fileName = parms[0];
        // Save the column name and compute the index.
        this.colName = parms[1];
        this.colIdx = template.findField(this.colName, inStream);
    }

    @Override
    public boolean eval(Record line) {
        List<String> values = this.getList(line);
        return values.stream().anyMatch(x -> ! x.isBlank());
    }

    @Override
    public List<String> getList(Record line) {
        String key = line.get(this.colIdx);
        return this.getTemplate().getGlobal(this.fileName, key);
    }

    @Override
    public String get(Record line) {
        List<String> values = this.getList(line);
        return StringUtils.join(values, Attribute.DELIM);
    }

    @Override
    public String getName() {
        return this.fileName + "[" + this.colName + "]";
    }

}
