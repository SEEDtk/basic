/**
 *
 */
package org.theseed.basic;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.joran.util.ConfigurationWatchListUtil;

/**
 * This is a simple base class for all processing methods.  It handles basic
 * command-parsing and automatically includes the help option.
 *
 * @author Bruce Parrello
 *
 */
public abstract class BaseProcessor implements ICommand {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(BaseProcessor.class);
    /** start time of processor */
    private long startTime;
    /** saved incoming parameters */
    private String[] options;
    /** logging context */
    private LoggerContext loggerContext;
    /** invocation command (set by subclass at its discretion) */
    private String commandString;

    // COMMAND-LINE OPTIONS

    /** help option */
    @Option(name = "-h", aliases = { "--help" }, help = true)
    protected boolean help;

    /** debug-message flag */
    @Option(name = "-v", aliases = { "--verbose", "--debug" }, usage = "show more detailed progress messages")
    private boolean debug;

    /**
     * Start the processor.  Here is where we track the start time.
     */
    public BaseProcessor() {
        this.startTime = System.currentTimeMillis();
        this.commandString = "(unknown)";
    }

    @Override
    public final boolean parseCommand(String[] args) {
        boolean retVal = false;
        this.help = false;
        this.debug = false;
        this.setDefaults();
        // Save the logging context.
        this.loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        // Save the incoming parameters.
        this.options = args;
        // Parse them.
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
            if (this.help) {
                parser.printUsage(System.err);
            } else {
                retVal = this.validateParms();
                if (retVal && this.debug) {
                    // To get more progress messages, we set the log level in logback.
                    ch.qos.logback.classic.Logger logger = this.loggerContext.getLogger("org.theseed");
                    logger.setLevel(Level.toLevel("TRACE"));
                    log.info("Debug logging ON.");
                } else {
                    URL mainURL = ConfigurationWatchListUtil.getMainWatchURL(this.loggerContext);
                    log.info("Normal logging selected using configuration at {}.", mainURL);
                }
            }
        } catch (CmdLineException | ParseFailureException e) {
            System.err.println(e.toString());
            parser.printUsage(System.err);
            System.exit(99);
        } catch (IOException e) {
            log.error("PARAMETER ERROR.", e);
            System.exit(99);
        }
        return retVal;
    }

    /**
     * Run the command.
     */
    @Override
    public final void run() {
        try {
            this.runCommand();
            log.info("{} seconds to run command.", (System.currentTimeMillis() - this.startTime) / 1000.0);
        } catch (Exception e) {
            log.error("EXECUTION ERROR.", e);
            System.exit(1);
        }
    }

    /**
     * Set the parameter defaults.
     */
    protected abstract void setDefaults();

    /**
     * Validate the parameters after parsing.
     */
    protected abstract boolean validateParms() throws IOException, ParseFailureException;

    /**
     * Run the command process.
     *
     * All the parameters are filled in, and exceptions are caught and logged.
     *
     * @throws Exception
     */
    protected abstract void runCommand() throws Exception;

    /**
     * @return the original command-line options and parameters
     */
    public String[] getOptions() {
        return options;
    }

    /**
     * @return the saved command string
     */
    public String getCommandString() {
        return this.commandString;
    }

    /**
     * Save a command string for logging purposes.
     *
     * @param commandString the command string to save
     */
    public void setCommandString(String commandString) {
        this.commandString = commandString;
    }

    /**
     * Open a print writer for a given output file.  If NULL is passed, the standard output will be used.
     *
     * @param outFile	output file name, or NULL to use the standard output
     *
     * @throws IOException
     */
    public PrintWriter openWriter(File outFile) throws IOException {
        PrintWriter retVal;
        if (outFile == null)
            retVal = new PrintWriter(System.out);
        else
            retVal = new PrintWriter(outFile);
        return retVal;
    }

    /**
     * Make a stream parallel if a flag is set.
     *
     * @param stream	incoming stream
     * @param paraFlag	TRUE if the stream should be made parallel
     *
     * @return the incoming stream in either normal or parallel mode
     */
    protected <T> Stream<T> makePara(Stream<T> stream, boolean paraFlag) {
        Stream<T> retVal;
        if (paraFlag) {
            retVal = stream.parallel();
            log.info("Parallel processing is being used.");
        } else
            retVal = stream;
        return retVal;
    }

    /**
     * Display the sub-commands for this application with a short description.
     *
     * @param commands	a string array containing each sub-command followed by its description
     */
    public static void showCommands(String[] commands) {
        System.err.println("Available sub-commands.");
        // Our first task is to sort the commands and figure out the length of the longest.
        var commandMap = new TreeMap<String, String>();
        int maxLen = 0;
        for (int i = 0; i < commands.length; i += 2) {
            commandMap.put(commands[i], commands[i+1]);
            if (commands[i].length() > maxLen)
                maxLen = commands[i].length();
        }
        // Now write out the commands.
        final int tabSize = maxLen + 2;
        for (var mapEntry : commandMap.entrySet())
            System.err.println(StringUtils.rightPad(mapEntry.getKey(), tabSize) + mapEntry.getValue());
    }

}
