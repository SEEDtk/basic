/**
 *
 */
package org.theseed.io.template;

import org.theseed.basic.ParseFailureException;

/**
 * A primitive template command has no sub-commands.  We throw an error if someone
 * tries to add a sub-command.
 *
 * @author Bruce Parrello
 *
 */
public abstract class PrimitiveTemplateCommand extends TemplateCommand {

    public PrimitiveTemplateCommand(LineTemplate template) {
        super(template);
    }

    @Override
    protected void addCommand(TemplateCommand command) throws ParseFailureException {
        throw new ParseFailureException("Invalid attempt to add a subcommand to a primitive command.");

    }

}
