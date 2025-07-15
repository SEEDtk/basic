/**
 *
 */
package org.theseed.basic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a variant of the basic command processor that writes a report.  It handles opening, closing
 * and flushing of a PrintWriter for output, and specification of output to STDOUT or a file.
 *
 * @author Bruce Parrello
 *
 */
public abstract class BaseReportProcessor extends BaseProcessor {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(BaseReportProcessor.class);
    /** output stream */
    private OutputStream outStream;

    // COMMAND-LINE OPTIONS

    /** output file (if not STDOUT) */
    @Option(name = "-o", aliases = { "--output" }, usage = "output file for report (if not STDOUT)")
    private File outFile;

    @Override
    protected final void setDefaults() {
        this.outFile = null;
        this.setReporterDefaults();
    }

    /**
     * Set the default values of command-line options.
     */
    protected abstract void setReporterDefaults();

    @Override
    protected final void validateParms() throws IOException, ParseFailureException {
        this.validateReporterParms();
        if (this.outFile == null) {
            log.info("Output will be to the standard output.");
            this.outStream = System.out;
        } else {
            log.info("Output will be to {}.", this.outFile);
            this.outStream = new FileOutputStream(this.outFile);
        }
    }

    /**
     * Validate the command-line options and parameters.
     *
     * @throws IOException
     * @throws ParseFailureException
     */
    protected abstract void validateReporterParms() throws IOException, ParseFailureException;

    @Override
    protected final void runCommand() throws Exception {
        try (PrintWriter writer = new PrintWriter(this.outStream)) {
            this.runReporter(writer);
        } finally {
            // Insure the output file is closed.
            if (this.outFile != null)
                this.outStream.close();
        }
    }

    /**
     * Execute the command and produce the report.
     *
     *  @param writer	print writer to receive the report
     *
     *  @throws Exception
     */
    protected abstract void runReporter(PrintWriter writer) throws Exception;

}
