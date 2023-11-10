/**
 *
 */
package org.theseed.io.template;

import org.theseed.io.FieldInputStream;

/**
 * A literal command is very simple, and outputs its internal text unchanged.
 *
 * @author Bruce Parrello
 *
 */
public class LiteralCommand extends PrimitiveTemplateCommand {

    // FIELDS
    /** content of the literal */
    private String text;

    /**
     * Compile a literal command.
     *
     * @param template	controlling template
     * @param content 	literal string to output
     */
    public LiteralCommand(LineTemplate template, String content) {
        super(template);
        this.text = content;
        this.setEstimatedLength(content.length());
    }

    @Override
    protected String translate(FieldInputStream.Record line) {
        return this.text;
    }

    @Override
    protected String getName() {
        return "literal";
    }

}
