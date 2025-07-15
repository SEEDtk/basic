/**
 *
 */
package org.theseed.basic;

/**
 * Simple interface for command-line processors.
 *
 * @author Bruce Parrello
 *
 */
public interface ICommand {

    /**
     * Parse the command-line arguments.
     *
     * @param args	array of command-line arguments and options
     *
     * @return	TRUE if the options were correct, else FALSE
     */
    public void parseCommand(String[] args);

    /**
     * Run the command.
     */
    public void run();

}
