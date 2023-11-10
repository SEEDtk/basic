/**
 *
 */
package org.theseed.io;

/**
 * This interface provides unified access to the various sorts of output streams used to train, test,
 * and execute dl4j.run neural networks.  The essential methods are the two types of writes.
 *
 * @author Bruce Parrello
 *
 */
public interface ILabeledOutputStream {

    /**
     * Write a line directly to output.  This is useful for headers.
     *
     * @param label		text for the label column
     * @param text		text for the rest of the line
     */
    public void writeImmediate(String label, String text);

    /**
     * Queue a line for output.
     *
     * @param label		class label for this line
     * @param line		data for the line
     */
    public void write(String label, String line);

    /**
     * Close the file.
     */
    public void close();

}
