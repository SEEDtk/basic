/**
 *
 */
package org.theseed.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * In this case, the input file is in JSON format.  The format is somewhat restricted.  Each record is a map, and
 * all the maps are in a global list.  A field can be a string, a number, or a (possibly empty) list of strings.
 * Any fancy recursion will cause an IO error.
 *
 * We process the input file character by character.  The map keys are quoted strings representing column labels
 * (e.g. field names).  The values can be numbers (which are kept as strings), quoted strings, or lists.  Each
 * list element can be a number or a quoted string.  We use the old trick of breaking the input into tokens to
 * accomplish this.  Fortunately, it is illegal to break a token across a line.
 *
 * @author Bruce Parrello
 *
 */
public class JsonListInputStream extends FieldInputStream {

    // FIELDS
    /** iterator for the current line's unprocessed tokens */
    private Iterator<String> tokenIter;
    /** set of invalid key tokens */
    private static final Set<String> INVALID_KEYS = Set.of("[", "]", "{", "}", ",");

    public JsonListInputStream(File inputFile) throws IOException {
        super(inputFile);
        this.initialize();
    }

    public JsonListInputStream(InputStream inputStream) throws IOException {
        super(inputStream);
        this.initialize();
    }

    /**
     * This method primes the parse by reading past the open-list token and positioning after the next
     * open-record token.
     *
     * @throws IOException
     */
    private void initialize() throws IOException {
        // Initialize to an empty tokenizer, which is permanent end-of-file.
        this.tokenIter = Attribute.EMPTY_LIST.iterator();
        // Open the main list.
        String possibleOpen = this.getNextToken();
        if (! possibleOpen.contentEquals("["))
            throw new IOException("JSON field input stream does not begin with start-of-list delimiter.");
        // Now start the first record.
        this.startNewRecord();
    }

    /**
     * This method positions on the first token of the next non-empty record.  There may not be one.
     * We basically loop until we eat an open brace or a close bracket.  Once we've eaten the open
     * brace, we make sure the token list is nonempty.  Either we will accomplish this or we will
     * hit end-of-file and throw an error.
     *
     * @throws IOException
     */
    private void startNewRecord() throws IOException {
        String possibleOpen = this.getNextToken();
        if (possibleOpen.contentEquals(","))
            possibleOpen = this.getNextToken();
        if (possibleOpen.contentEquals("]")) {
            // Here we've reached the end of the main list.  Insure we have end-of-file condition.
            this.tokenIter = Attribute.EMPTY_LIST.iterator();
        } else if (! possibleOpen.contentEquals("{")) {
            // Here we have found something that doesn't belong.
            throw new IOException("Unexpected token \"" + possibleOpen + "\" in line " + this.getLineNumber() + ".");
        } else {
            // We have eaten an open brace.  Insure the next token is ready.
            this.reposition();
        }
    }

    /**
     * @return the next token in the input stream
     *
     * @throws IOException
     */
    protected String getNextToken() throws IOException {
        // Get a token in the queue.
        this.reposition();
        // Here we have a token in the queue.
        String retVal = this.tokenIter.next();
        return retVal;
    }

    /**
     * Insure a next token is in the queue.
     *
     * @throws IOException
     */
    private void reposition() throws IOException {
        while (! this.tokenIter.hasNext()) {
            // No more tokens, so get the next line.
            if (! this.hasNextLine())
                throw new IOException("Unexpected end-of-file in JSON input stream.");
            String line = this.nextLine();
            JsonTokenizer tokens = new JsonTokenizer(line, this.getLineNumber());
            this.tokenIter = tokens.iterator();
        }
    }

    @Override
    public boolean hasNext() {
        return this.tokenIter.hasNext();
    }

    @Override
    public Record next() {
        // We will build the return value in here.
        Record retVal = this.new Record(this.width());
        try {
            // Here we must read the next record into a hash.  We expect to find tokens in the order [key, colon, value, comma].
            // The only wrinkle is that sometimes the value can be a list.
            boolean done = false;
            String key = this.getNextToken();
            // If the first key is in fact the close-brace, we have an empty record.
            if (! key.contentEquals("}")) {
                // Loop through the key-value pairs.
                while (! done) {
                    if (INVALID_KEYS.contains(key))
                        throw new IOException("Unexpected token \"" + key + "\" when looking for map key in line " + this.getLineNumber() + ".");
                    String delim = this.getNextToken();
                    if (! delim.contentEquals(":"))
                        throw new IOException("Expecting colon after \"" + key + "\", found \"" + delim + "\" in line " + this.getLineNumber() + ".");
                    // The next token should be the value.  It could also be an open-list bracket.
                    String value = this.getNextToken();
                    if (value.contentEquals("["))
                        retVal.setField(key, this.parseList());
                    else if (value.contentEquals("{"))
                        throw new IOException("Unsupported use of map value for key \"" + key + "\" in line " + this.getLineNumber() + ".");
                    else if (INVALID_KEYS.contains(value))
                        throw new IOException("Unexpected token \"" + value + "\" found parsing value of \"" + key + "\" in line " + this.getLineNumber() + ".");
                    else
                        retVal.setField(key, value);
                    // The next token should be a comma or end-of-record.
                    delim = this.getNextToken();
                    if (delim.contentEquals("}"))
                        done = true;
                    else if (! delim.contentEquals(","))
                        throw new IOException("Unexpected token \"" + delim + "\" found during record parsing in line " + this.getLineNumber() + ".");
                    else
                        key = this.getNextToken();
                }
            }
            // Position on the next record.
            this.startNewRecord();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return retVal;
    }

    /**
     * Parse the list value at the current position.  At the end, we will be positioned on the comma after
     * the list.
     *
     * @return the list of strings to use as the value
     *
     * @throws IOException
     */
    private List<String> parseList() throws IOException {
        List<String> retVal = new ArrayList<String>();
        // We expect string, comma, string, comma, ... string, close-bracket.
        String element = this.getNextToken();
        // Handle the special case of an empty list.
        if (! element.contentEquals("]")) {
            // Loop until we find a close bracket.
            boolean closed = false;
            while (! closed) {
                if (element.contentEquals("{") || element.contentEquals("["))
                    throw new IOException("Unsupported feature:  list in line " + this.getLineNumber() + " contains a non-primitive element.");
                // Add this element to the return list.
                retVal.add(element);
                // Get the delimiter.
                String delim = this.getNextToken();
                if (delim.contentEquals("]"))
                    closed = true;
                else if (! delim.contentEquals(","))
                    throw new IOException("Expecting comma or close-bracket, but found \"" + delim + " in line " + this.getLineNumber() + ".");
                else
                    element = this.getNextToken();
            }
        }
        return retVal;
    }

    @Override
    public int findField(String fieldName) throws IOException {
        int retVal = this.findColumn(fieldName);
        if (retVal < 0) {
            // Here the field was not found.  If we are not locked, we add it.
            if (this.areFieldsLocked())
                throw new IOException("Unknown field \"" + fieldName + "\" requested after start of iteration.");
            else
                retVal = this.addFieldName(fieldName);
        }
        return retVal;
    }

}
