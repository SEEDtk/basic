/**
 *
 */
package org.theseed.io.template;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;
import org.theseed.io.template.output.TemplateHashWriter;
import org.theseed.magic.FidMapper;



/**
 * This object manages a template that can be used to translate a line from a tab-delimited file into
 * a text string.  The template string contains variables which are column names surrounded by double braces.
 * Each such variable is replaced by the corresponding column value in the current line.
 *
 * Special commands are handled by a dollar sign and a command name.  If the command requires a variable or
 * expression, that follows the command with a colon separator.  Conditionals are handled by the special
 * commands "if", "else", and "fi".  "if" takes one or more field expressions as its argument, and the body is only
 * output if all the columns evaluate to TRUE.  "else" produces output if the IF failed, and "fi" terminates
 * the conditional.
 *
 * There is a second kind of conditional called the group.  The group starts with the "group" command and
 * end with an "end" command.  It takes as input a conjunction, usually "and" or "or".  Each clause in the
 * group is associated with a column name.  The group is only generated if at least one column is nonblank (so
 * the group is treated as a conditional at runtime). Inside the group, the "clause" command generates text
 * if the specified column is nonblank.  (The column name should be specified as a parameter.) At the
 * end, the clauses are assembled and put into a long sentence with commas and the conjunction if necessary,
 * with a final period.  Sometimes the group will start in the middle of a sentence instead of being a sentence
 * unto itself.  If this happens, you can add a period as an additional parameter (colon-separated) to the
 * conjunction.
 *
 * Occasionally, you will want to use the group to output the clauses as individual lines.  If this is the
 * case, use "nl" as the conjunction.
 *
 * Besides conditionals, we have the following special commands
 *
 *  0			produces no output
 * 	list		takes as input a conjunction, a field expression, and a separator string.  The column is
 * 				split on the separator string, and then formed into a comma-separated list using the conjunction.
 * 				If the separator string is omitted, the column is retrieved as a list.
 *  numword		takes as input a field expression, a singular word, a plural word, and an optional separator
 *  			string.  The field expression is interpreted as a list.  If there is one item in the list, the
 *  			singular word is emitted; otherwise, the plural word is emitted.
 *  product		takes as input two column names separated by a colon.  The first column is presumed to be a gene
 *  			product, and is parsed into multiple English sentences accordingly.  THe second column should
 *  			contain the code for the feature type, which is included in the description and affects the
 *  			text.
 *  qlist		identical to the list command, except the list elements are quoted
 *  tab			emits a horizontal tab character
 *  strand		takes as input a column name containing a strand code and outputs a text translation
 *  nl			emits a line break
 *  choices		outputs randomly-selected answers to a multiple-choice question.  Takes as input a choice-set
 *  			name, the correct answer (as a column name or as a literal in quotes), and the number of answers desired.
 *  json		takes as input a type name, a tag name, and a field expression.  It outputs a JSON string.
 *  fid			generates a useful feature ID from a real feature ID
 *  gStore		store the genome ID and name mapping for future features
 *
 * The template string is parsed into a list of commands.  This command list can then be processed rapidly
 * to form the result string.
 *
 * The $fid command is exceptionally complicated.  At some point, the FidMapper must be created and attached
 * to the template.  Before any features are expressed, the genome must be set up in the FidMapper using the
 * $gid command.  The first time a feature is expressed, it must include the product and the FIG ID.  After
 * that, only the patric ID is needed.
 *
 * @author Bruce Parrello
 *
 */
public class LineTemplate {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(LineTemplate.class);
    /** compiled template */
    private TemplateCommand compiledTemplate;
    /** compile stack */
    private Deque<TemplateCommand> compileStack;
    /** global-data cache */
    private TemplateHashWriter globals;
    /** randomizer */
    private Random rand;
    /** feature ID mapper */
    private FidMapper fidMapper;
    /** search pattern for variables */
    protected static final Pattern VARIABLE = Pattern.compile("(.*?)\\{\\{(.+?)\\}\\}(.*)");
    /** search pattern for special commands */
    protected static final Pattern COMMAND = Pattern.compile("\\$(\\w+)(?::(.+))?");

    /**
     * Construct a line template for the specified tab-delimited file and the specified template string.
     *
     * @param inStream	tab-delimited file stream
     * @param template	template string
     * @param globals 	global-data structure
     * @param fidMap	feature ID mapper to use
     *
     * @throws IOException
     * @throws ParseFailureException
     */
    public LineTemplate(FieldInputStream inStream, String template, TemplateHashWriter globals,
            FidMapper fidMap)
            throws IOException, ParseFailureException {
        // Store the feature ID mapper.
        this.fidMapper = fidMap;
        // Set up the randomizer.
        this.rand = new Random();
        // Save the global-data cache.
        this.globals = globals;
        // Initialize the compile stack.
        this.compileStack = new ArrayDeque<TemplateCommand>();
        this.compileStack.push(new BlockCommand(this,"block"));
        final int len = template.length();
        log.info("Compiling {}-character template string.", len);
        // We parse the template into tokens.  There are literals, variables, and commands.  The unparsed section
        // of the string is stored as the residual.  We set up a try-block so we can output the neighborhood of the
        // error.
        String residual = template;
        int currentPos = 0;
        try {
            while(! residual.isEmpty()) {
                // Look for the next variable or command.
                Matcher m = VARIABLE.matcher(residual);
                if (! m.matches()) {
                    // Here the entire remainder of the template is a literal.
                    currentPos = template.length() - residual.length();
                    TemplateCommand residualCommand = new LiteralCommand(this, residual);
                    this.addToTop(residualCommand);
                    residual = "";
                } else {
                    // Here group 1 is the initial literal, group 2 is a variable or command, and group 3
                    // is the new residual.
                    String prefix = m.group(1);
                    String construct = m.group(2);
                    currentPos = template.length() - residual.length() + prefix.length();
                    // Process the prefix.
                    if (! prefix.isEmpty()) {
                        TemplateCommand prefixCommand = new LiteralCommand(this, prefix);
                        this.addToTop(prefixCommand);
                    }
                    // Is this a command or a variable reference?
                    if (construct.charAt(0) != '$') {
                        // Here we have a variable reference.
                        TemplateCommand varCommand = new ColumnCommand(this, construct, inStream);
                        this.addToTop(varCommand);
                    } else {
                        // Here we have a special command and we need to decode it.
                        Matcher m2 = COMMAND.matcher(construct);
                        if (! m2.matches())
                            throw new ParseFailureException("Invalid special command \"" + construct + "\".");
                        // Set up some local variables for use below.
                        TemplateCommand newCommand = null;
                        switch (m2.group(1)) {
                        case "0" :
                            // The null command does nothing, so it has no effect.
                            break;
                        case "strand" :
                            // This command translates a strand code (+ or -).
                            newCommand = new StrandCommand(this, inStream, m2.group(2));
                            this.addToTop(newCommand);
                            break;
                        case "product" :
                            // This command translates a gene product.
                            newCommand = new GeneProductCommand(this, inStream, m2.group(2));
                            this.addToTop(newCommand);
                            break;
                        case "numword" :
                            // This command uses a field containing a list to decide between a singular word
                            // and a plural word.
                            newCommand = new NumWordCommand(this, inStream, m2.group(2));
                            this.addToTop(newCommand);
                            break;
                        case "list" :
                            // This command turns a field containing a list into a comma-separated
                            // phrase.
                            newCommand = new ListCommand(this, inStream, m2.group(2));
                            this.addToTop(newCommand);
                            break;
                        case "qlist" :
                            // This command turns a field containing a list into a comma-separated
                            // phrase with quoted elements.
                            newCommand = new QuotedListCommand(this, inStream, m2.group(2));
                            this.addToTop(newCommand);
                            break;
                        case "if" :
                            // This command starts an if-block.
                            newCommand = new IfCommand(this, inStream, m2.group(2));
                            this.addAndPush(newCommand);
                            // Start a block to cover the THEN clause.
                            this.addAndPush(new BlockCommand(this, "if"));
                            break;
                        case "nl" :
                            // This command emits a new-line.
                            newCommand = new LiteralCommand(this, "\n");
                            this.addToTop(newCommand);
                            break;
                        case "else" :
                            // This command starts a block that executes when the IF is false.
                            // We first need to pop off a then-block.  This next method fails if
                            // the context is not IF.
                            this.popInContext("else", "if");
                            // Now create the ELSE and connect it to the IF.
                            newCommand = new BlockCommand(this, "else");
                            this.addAndPush(newCommand);
                            break;
                        case "fi" :
                            // This command terminates the scope of an IF-construct. We must
                            // insure we are in the scope of an if-construct.  Pop off the
                            // currently-active block command and verify we are in a valid
                            // context.
                            this.popInContext("fi", "if", "else");
                            // Pop off the IF itself.
                            this.pop();
                            break;
                        case "group" :
                            // The group command allows the template to create a conjuncted list of
                            // complex phrases.  The group consists of a prefix and a set of clauses.
                            // We need to construct the group command and then push on a block command
                            // for the prefix.
                            newCommand = new GroupCommand(this, m2.group(2));
                            this.addAndPush(newCommand);
                            this.addAndPush(new BlockCommand(this, "group"));
                            break;
                        case "clause" :
                            // The clause command indicates a conditional section of the group.
                            // Pop off the current block command and verify we are in a valid
                            // context.
                            this.popInContext("clause", "group", "clause");
                            // Create the clause command and add it to the group.
                            newCommand = new ClauseCommand(this, inStream, m2.group(2));
                            this.addAndPush(newCommand);
                            break;
                        case "end" :
                            // This command ends a group construct.  Pop off the currently-active
                            // block command and verify we are in a valid context.
                            this.popInContext("end", "group", "clause");
                            // Pop off the GROUP itself.
                            this.pop();
                            break;
                        case "tab" :
                            // This command emits a tab.
                            newCommand = new LiteralCommand(this, "\t");
                            this.addToTop(newCommand);
                            break;
                        case "choices" :
                            // This command presents multiple answer choices.
                            newCommand = new ChoiceCommand(this, inStream, m2.group(2));
                            this.addToTop(newCommand);
                            break;
                        case "json" :
                            // This command presents a JSON string.
                            newCommand = new JsonCommand(this, inStream, m2.group(2));
                            this.addToTop(newCommand);
                            break;
                        case "fid" :
                            // This command emits a useful feature ID.
                            newCommand = new FidCommand(this, inStream, m2.group(2));
                            break;
                        case "gStore" :
                            // This command store the genome ID and name.
                            newCommand = new GStoreCommand(this, inStream, m2.group(2));
                            break;
                        default :
                            throw new ParseFailureException("Unknown special command \"" + m2.group(1) + "\".");
                        }
                    }
                    // Update the residual.
                    residual = m.group(3);
                }
            }
            if (this.compileStack.size() > 1)
                throw new ParseFailureException("Unclosed " + this.peek().getName() + " command in template.");
            // Save the compiled template.
            this.compiledTemplate = this.pop();
        } catch (ParseFailureException e) {
            int start = currentPos - 20;
            if (start < 0) start = 0;
            int end = currentPos + 20;
            if (end > len) end = len;
            log.error("Parsing error encountered near \"{}\".", template.substring(start, end));
            log.error("Parser message: {}", e.getMessage());
            throw new ParseFailureException(e);
        }
    }

    private void popInContext(String newName, String... context) throws ParseFailureException {
        TemplateCommand top = this.pop();
        final String name = top.getName();
        if (! Arrays.stream(context).anyMatch(x -> x.contentEquals(name)))
            throw new ParseFailureException("\"" + newName + "\" command found outside of proper context.");
    }

    /**
     * Set the random seed to allow repeatable testing.
     *
     * @param newSeed	new random see
     */
    protected void setSeed(long newSeed) {
        this.rand = new Random(newSeed);
    }

    /**
     * Specify a feature ID mapper for the $fid command.
     *
     * @param fidMap	feature ID mapper to use for this template
     */
    public void setFidMapper(FidMapper fidMap) {
        this.fidMapper = fidMap;
    }

    /**
     * Compute the magic-word identifier for a feature when the function is known.
     *
     * @param fid	FIG feature ID
     * @param fun	functional assignment
     *
     * @return the magic word feature ID
     *
     * @throws ParseFailureException
     */
    public String getMagicFid(String fid, String fun) throws ParseFailureException {
        if (this.fidMapper == null)
            throw new ParseFailureException("No FidMapper assigned during request for identifier of \""
                    + fid + "\".");
        return this.fidMapper.getMagicFid(fid, fun);
    }

    /**
     * Compute the magic-word identifier for a feature when the function is not known.
     *
     * @param fid	FIG feature ID
     *
     * @return the magic word feature ID
     *
     * @throws ParseFailureException
     */
    public String getMagicFid(String fid) throws ParseFailureException {
        if (this.fidMapper == null)
            throw new ParseFailureException("No FidMapper assigned during request for identifier of \""
                    + fid + "\".");
        return this.fidMapper.getMagicFid(fid);
    }


    /**
     * Add a new subcommand to the top command on the compile stack.
     *
     * @param subCommand	subcommand to add
     *
     * @throws ParseFailureException
     */
    private void addToTop(TemplateCommand subCommand) throws ParseFailureException {
        TemplateCommand top = this.compileStack.peek();
        if (top == null)
            throw new ParseFailureException("Mismatched block construct.");
        top.addCommand(subCommand);
    }

    /**
     * Add a new subcommand to the top command of the compile stack and push it on
     * to start a new context.
     *
     * @param subCommand	subcommand to add and push
     */
    private void addAndPush(TemplateCommand subCommand) throws ParseFailureException {
        this.addToTop(subCommand);
        this.push(subCommand);
    }

    /**
     * This method applies the template to the current input line.
     *
     * @param line		input line to process
     *
     * @return the result of applying the template to the input line
     */
    public String apply(FieldInputStream.Record line) {
        // Create the string builder.
        return this.compiledTemplate.translate(line);
    }

    /**
     * Push a new command onto the compile stack.
     *
     * @param command	command to push
     */
    private void push(TemplateCommand command) {
        this.compileStack.push(command);

    }

    /**
     * Pop a command off the command stack.
     *
     * @return the command on top of the stack.
     */
    private TemplateCommand pop() {
        return this.compileStack.pop();
    }

    /**
     * @return the top command on the stack without popping it
     */
    protected TemplateCommand peek() {
        return this.compileStack.peek();
    }

    /**
     * @return the index for the specified column
     *
     * @param colName	name of the column desired
     * @param inStream	source input stream
     *
     * @throws ParseFailureException
     *
     */
    public int findField(String colName, FieldInputStream inStream) throws ParseFailureException {
        int retVal;
        try {
            retVal = inStream.findField(colName);
        } catch (IOException e) {
            // Convert a field-not-found to a parsing exception.
            throw new ParseFailureException("Could not find field \"" + colName + "\" in source input stream.");
        }
        return retVal;
    }

    /**
     * Form a list of phrases into an english-language list using a conjunction.
     *
     * @param conjunction	conjunction for the final phrase
     * @param phrases		list of phrases
     *
     * @return a string representation of the list
     */
    public static String conjunct(String conjunction, List<String> phrases) {
        String retVal;
        final int n = phrases.size() - 1;
        switch (phrases.size()) {
        case 0:
            retVal = "";
            break;
        case 1:
            retVal = phrases.get(0);
            break;
        case  2:
            retVal = phrases.get(0) + " " + conjunction + " " +  phrases.get(1);
            break;
        default:
            //  Here we need to assemble the phrases with the conjunction between the last two.
            int len = 10 + phrases.size() * 2 + phrases.stream().mapToInt(x -> x.length()).sum();
            StringBuilder buffer = new StringBuilder(len);
            IntStream.range(0, n).forEach(i -> buffer.append(phrases.get(i)).append(", "));
            buffer.append(conjunction).append(" ").append(phrases.get(n));
            retVal = buffer.toString();
        }
        return retVal;
    }

    /**
     * Extract a global data item.
     *
     * @param fileName	name of the input file
     * @param keyValue	key value for the item
     *
     * @return the item value, or an empty string if it was not found
     */
    public List<String> getGlobal(String fileName, String keyValue) {
        return this.globals.getStrings(fileName, keyValue);
    }

    /**
     * @return a list of choices from the specified choice list
     *
     * @param name		name of the choice list
     * @param answer	correct choice
     * @param num		number of choices to use
     *
     * @throws ParseFailureException
     */
    public List<String> getChoices(String name, String answer, int num) throws ParseFailureException {
        List<String> retVal;
        Set<String> choices = this.globals.getChoices(name);
        if (choices == null)
            throw new ParseFailureException("No choice list named \"" + name + "\" is available.");
        final int n = choices.size();
        ArrayList<String> choiceList = new ArrayList<String>(n);
        if (num >= n) {
            choiceList.addAll(choices);
            this.shuffle(choiceList, n);
            retVal = choiceList;
        } else {
            choices.stream().filter(x -> ! x.equals(answer)).forEach(x -> choiceList.add(x));
            this.shuffle(choiceList, num);
            // Now we have "num" random entries at the beginning.  Figure out where to add the
            // real answer.
            int idx = this.rand.nextInt(num);
            choiceList.set(idx, answer);
            retVal = choiceList.subList(0, num);
        }
        return retVal;
    }

    /**
     * Shuffle random entries into the first N positions of a list.
     *
     * @param choiceList	list to shuffle
     * @param n				number of entries to choose
     */
    public void shuffle(ArrayList<String> choiceList, int n) {
        // Now we need to shuffle the list.  We do this internally so we can set the seed for
        // testing.
        int i = 0;
        // Loop until there is no more shuffling to do.
        while (i < n) {
            // Determine how much space is available to pick from.
            int remaining = choiceList.size() - i;
            // Compute the place to pick from.
            int j = this.rand.nextInt(remaining) + i;
            if (j != i) {
                String buffer = choiceList.get(j);
                choiceList.set(j, choiceList.get(i));
                choiceList.set(i, buffer);
            }
            i++;
        }
    }

    /**
     * @return TRUE if the named choice list is present, else FALSE
     *
     * @param name	name of the choice list
     */
    public boolean hasChoiceList(String name) {
        return this.globals.getChoices(name) != null;
    }

    /**
     * @return the choice list with the given name, or NULL if there is none
     *
     * @param name	name of the choice list
     */
    public Set<String> getChoices(String name) {
        return this.globals.getChoices(name);
    }

    /**
     * Set up a new genome for the current FID mapper.
     *
     * @param gID		ID of new genome
     * @param gName		name of new genome
     */
    public void storeMapperGenome(String gID, String gName) {
        this.fidMapper.setup(gID, gName);
    }
}
