/**
 *
 */
package org.theseed.io.template;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;
import org.theseed.io.FieldInputStream.Record;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * The choices command is used to generate reasonable responses for multiple-choice questions.  The choices will
 * be taken randomly from a named choice list and emitted with labels A, B, C, and so forth.  The command takes
 * three parameters (separated by colons, as usual).  First is the choice set name, second is the correct answer,
 * and third is the number of answers to emit.  The correct answer can be specified either as a field name or as
 * a literal enclosed in double quotes.
 * @author Bruce Parrello
 *
 */
public class ChoiceCommand extends PrimitiveTemplateCommand {

    // FIELDS
    /** command to retrieve the correct answer */
    private TemplateCommand answerCommand;
    /** number of answers desired */
    private int numAnswers;
    /** name of source choice list */
    private String choiceListName;
    /** pattern for matching a literal answer spec */
    private static final Pattern LITERAL_ANSWER = Pattern.compile("\"(.+)\"");

    /**
     * Construct the choices command.
     *
     * @param template	controlling master template
     * @param inStream	input field stream
     * @param parms		parameter string
     *
     * @throws ParseFailureException
     */
    public ChoiceCommand(LineTemplate template, FieldInputStream inStream, String parms) throws ParseFailureException {
        super(template);
        String[] pieces = StringUtils.split(parms, ':');
        if (pieces.length != 3)
            throw new ParseFailureException("All three parameters to the CHOICES command are required.");
        // Save the answer count.
        try {
            this.numAnswers = Integer.valueOf(pieces[2]);
        } catch (NumberFormatException e) {
            throw new ParseFailureException("Invalid answer count \"" + pieces[2] + "\" in CHOICES command.");
        }
        // Validate the parms.
        if (this.numAnswers < 2)
            throw new ParseFailureException("Requested number of answers must be at least 2 in CHOICES command.");
        if (this.numAnswers > 10)
            throw new ParseFailureException("Requested number of answers cannot be more than 10 in CHOICES command.");
        this.choiceListName = pieces[0];
        if (! this.getMasterTemplate().hasChoiceList(pieces[0]))
            throw new ParseFailureException("Undefined choice list \"" + pieces[0] + "\" specified in CHOICES command.");
        // Now we process the correct answer.
        String answer = pieces[1];
        Matcher m = LITERAL_ANSWER.matcher(answer);
        if (m.matches()) {
            // Here we have a literal answer.
            this.answerCommand = new LiteralCommand(template, m.group(1));
        } else {
            // Here the answer is the content of a field.
            this.answerCommand = new ColumnCommand(template, answer, inStream);
        }
    }

    @Override
    protected String translate(Record line) {
        // Compute the answer.
        String answer = this.answerCommand.translate(line);
        // Get the list of choices.
        List<String> choices;
        try {
            choices = this.getMasterTemplate().getChoices(this.choiceListName, answer, this.numAnswers);
        } catch (ParseFailureException e) {
            // This should never happen, because we checked at compile time.
            throw new RuntimeException("Choice list error: " + e.getMessage());
        }
        // Now output the answers.
        List<String> answers = new ArrayList<String>(choices.size());
        char label = 'A';
        for (String choice : choices) {
            answers.add(Character.toString(label) + ") " + choice);
            label++;
        }
        String retVal = LineTemplate.conjunct("or", answers);
        return retVal;
    }

    @Override
    protected String getName() {
        return "choices";
    }

}
