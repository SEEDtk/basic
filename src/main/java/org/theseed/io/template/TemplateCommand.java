/**
 *
 */
package org.theseed.io.template;

import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;

/**
 * This is the base class for template commands.  It handles the process of tracking the length
 * estimate and exposes the methods needed by the template processor.
 *
 * @author Bruce Parrello
 *
 */
public abstract class TemplateCommand {

    // FIELDS
    /** estimated length of the command output */
    private int estimatedLength;
    /** controlling template */
    private LineTemplate masterTemplate;

    /**
     * Compile a new template command.
     *
     * @param template	controlling command template
     */
    public TemplateCommand(LineTemplate template) {
        this.estimatedLength = 0;
        this.masterTemplate = template;
    }

    /**
     * @return the estimated output length for this block
     */
    protected int getEstimatedLength() {
        return this.estimatedLength;
    }

    /**
     * Specify the estimated length for this block.
     *
     * @param newLength		proposed new estimated length
     */
    protected void setEstimatedLength(int newLength) {
        this.estimatedLength = newLength;
    }

    /**
     * Add the estimated length of the specified command to this one's.
     *
     * @param command		sub-command whose length should be added
     */
    protected void addEstimatedLength(TemplateCommand command) {
        this.estimatedLength += command.getEstimatedLength();
    }

    /**
     * Merge the estimated length of the specified command with this one's.
     * The larger length is kept.
     *
     * @param command		sub-command whose length should be merged
     */
    protected void mergeEstimatedLength(TemplateCommand command) {
        int newLength = command.getEstimatedLength();
        if (newLength > this.estimatedLength)
            this.estimatedLength = newLength;
    }

    /**
     * Add a new sub-command.
     *
     * @param command	sub-command to add
     *
     * @throws ParseFailureException
     */
    protected abstract void addCommand(TemplateCommand command) throws ParseFailureException;

    /**
     * Translate this command to output text.
     *
     * @param line	current input line
     *
     * @return the translated text of the command
     */
    protected abstract String translate(FieldInputStream.Record line);

    /**
     * @return the name of this command
     */
    protected abstract String getName();

    /**
     * @return the master controlling template
     */
    protected LineTemplate getMasterTemplate() {
        return this.masterTemplate;
    }
}
