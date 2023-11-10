/**
 *
 */
package org.theseed.io.template;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream.Record;

/**
 * The group command is used to construct a sentence containing a list of phrases, some
 * of which may not materialize.  The phrases are comma-spliced together with a conjunction
 * at the end (standard phrase-conjuncting).  We only generate the prefix, the phrases
 * which correspond to fields that evaluate TRUE, and a period at the end.  If none of
 * the fields are TRUE, we generate a specified null clause.
 *
 * The parameters are the conjunction (default "and") and the null clause (default empty string).
 * If the conjunction is specified as "nl", then a different approach will be used and the
 * clauses will be output as separate lines.
 */
public class GroupCommand extends TemplateCommand {

    // FIELDS
    /** conjunction */
    private String conjunction;
    /** prefix block */
    private TemplateCommand prefix;
    /** clause blocks */
    private List<ClauseCommand> clauses;
    /** suffix */
    private String nullClause;

    /**
     * Construct a group command.
     *
     * @param template	controlling master template
     * @param parms		parameter string, containing the conjunction
     */
    public GroupCommand(LineTemplate template, String parms) {
        super(template);
        String[] pieces = StringUtils.split(parms, ':');
        if (pieces == null || pieces.length < 1) {
            this.conjunction = "and";
            this.nullClause = "";
        } else {
            if (pieces[0].contentEquals("nl"))
                this.conjunction = null;
            else
                this.conjunction = pieces[0];
            if (pieces.length < 2)
                this.nullClause = "";
            else
                this.nullClause = pieces[1];
        }
        // Denote we have no prefix and no clauses.
        this.prefix = null;
        this.clauses = new ArrayList<ClauseCommand>();
    }

    @Override
    protected void addCommand(TemplateCommand command) throws ParseFailureException {
        // The first command is the prefix.  The others are added to the list.
        if (this.prefix == null)
            this.prefix = command;
        else
            this.clauses.add((ClauseCommand) command);
        this.addEstimatedLength(command);
    }

    @Override
    protected String translate(Record line) {
        // Run through the clauses, translating the valid ones.
        List<String> phrases = new ArrayList<String>(this.clauses.size());
        for (ClauseCommand clause : this.clauses) {
            if (clause.isSatisfied(line))
                phrases.add(clause.translate(line));
        }
        String retVal;
        if (phrases.size() == 0)
            retVal = this.nullClause;
        else {
            String startup = prefix.translate(line);
            if (this.conjunction == null) {
                // Here we have new-line mode.
                if (! StringUtils.isBlank(startup))
                    retVal = startup + "\n";
                else
                    retVal = "";
                retVal += StringUtils.join(phrases, "\n");
            } else
                retVal = startup + " " + LineTemplate.conjunct(this.conjunction, phrases)
                    + ".";
        }
        return retVal;
    }

    @Override
    protected String getName() {
        return "group";
    }

}
