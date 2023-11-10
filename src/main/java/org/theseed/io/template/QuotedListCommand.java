/**
 *
 */
package org.theseed.io.template;

import java.util.List;
import java.util.stream.Collectors;

import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;

/**
 * This is a variant of the list command that quotes all the list elements.
 *
 * @author Bruce Parrello
 *
 */
public class QuotedListCommand extends ListCommand {

    public QuotedListCommand(LineTemplate template, FieldInputStream inStream, String parms)
            throws ParseFailureException {
        super(template, inStream, parms);
    }

    @Override
    protected String assemble(String conjunct, List<String> pieces) {
        // Quote the strings.
        List<String> quoted = pieces.stream().map(x -> "\"" + x + "\"").collect(Collectors.toList());
        return LineTemplate.conjunct(conjunct, quoted);
    }

    @Override
    protected String getName() {
        return "qlist";
    }

}
