/**
 *
 */
package org.theseed.io.template;

import org.apache.commons.lang3.StringUtils;
import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;
import org.theseed.io.FieldInputStream.Record;

/**
 * This command is a single clause inside a group command.  It generates text only if the
 * associated column variable evaluates to TRUE.
 */
public class ClauseCommand extends BlockCommand {

    // FIELDS
    /** column index of condition field */
    private int colIdx;

    /**
     * Construct a group-command clause.
     *
     * @param template		controlling master template
     * @param inStream		source input file
     * @param parms			parameter string containing column name
     *
     * @throws ParseFailureException
     */
    public ClauseCommand(LineTemplate template, FieldInputStream inStream, String parms) throws ParseFailureException {
        super(template, "clause");
        if (StringUtils.isBlank(parms))
            throw new ParseFailureException("Column name is required for CLAUSE command.");
        this.colIdx = template.findField(parms, inStream);
    }

    /**
     * @return TRUE if the clause condition is satisfied for this input line
     *
     * @param line		source input record
     */
    public boolean isSatisfied(Record line) {
        return line.getFlag(this.colIdx);
    }

}
