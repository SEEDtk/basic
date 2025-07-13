/**
 *
 */
package org.theseed.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * This is a simple JSON tokenizer.  It takes a string as input, and breaks it into JSON tokens.  These
 * are either delimiters or strings.  Whitespace does not create tokens.
 *
 * @author Bruce Parrello
 *
 */
public class JsonTokenizer implements Iterable<String> {

    // FIELDS
    /** list of tokens to pass back to caller */
    private List<String> tokens;
    /** string being tokenized */
    protected String line;
    /** next character to process in string */
    protected int pos;
    /** string buffer for building tokens */
    protected StringBuilder buffer;
    /** current line number */
    protected int lineNumber;
    /** string of JSON delimiters */
    private static final String DELIMS = ",}]:";

    /**
     * This is a version of the tokenizer that returns the strings with the quotes in place.
     * Thus, the only change is how we parse a quoted string.
     */
    public static class Raw extends JsonTokenizer {

        /**
         * Parse a string into raw JSON tokens. Unlike the base class, this does not strip quotes
         * or unescape strings.
         * 
         * @param line      input line to tokenize
         * @param lineNum   line number for error messages
         * 
         * @throws IOException
         */
        public Raw(String line, int lineNum) throws IOException {
            super(line, lineNum);
        }

        /**
         * This method parses a quoted string.  It is presumed the current position is on the
         * open quote.  It will be moved past the close quote.
         *
         * @return the string extracted from the line
         *
         * @throws IOException
         */
        @Override
        protected String parseString() throws IOException {
            final int n = this.line.length();
            // Push past the open quote.
            this.buffer.append('"');
            this.pos++;
            // Loop ahead, looking for the close quote.
            boolean closed = false;
            while (this.pos < n && ! closed) {
                char c = line.charAt(this.pos);
                switch (c) {
                case '\\' :
                    // Here we have a backslash, so we need to push the next character regardless of what it is.
                    this.buffer.append(c);
                    this.pos++;
                    if (this.pos >= n)
                        throw new IOException("Invalid escape sequence at end of string in line " + this.lineNumber + ".");
                    c = line.charAt(this.pos);
                    break;
                case '"' :
                    // Here we have the close quote.
                    closed = true;
                    break;
                }
                this.buffer.append(c);
                this.pos++;
            }
            // Return the token and empty the buffer.
            String retVal = this.buffer.toString();
            this.buffer.setLength(0);
            return retVal;
        }

    }

    /**
     * Parse a string into JSON tokens.
     *
     * @param line		string to parse
     * @param lineNum 	line number for error messages
     *
     * @throws IOException
     */
    public JsonTokenizer(String line, int lineNum) throws IOException {
        this.lineNumber = lineNum;
        // Create the token list.
        this.tokens = new ArrayList<String>();
        // Set up to parse the line.
        this.line = line;
        final int n = line.length();
        this.buffer = new StringBuilder(n);
        this.pos = 0;
        while (this.pos < n) {
            // Here we are in open text.  We should either find a number, a delimiter, whitespace, or a quote.
            char c = line.charAt(this.pos);
            if (Character.isWhitespace(c)) {
                // Skip whitespace.
                this.pos++;
            } else if (Character.isAlphabetic(c) || Character.isDigit(c) || c == '.' || c == '-') {
                // Here we have an unquoted token
                this.tokens.add(this.parseUnquoted(c));
            } else switch (c) {
            case '"' :
                // Here we have a string.
                String tokenString = this.parseString();
                this.tokens.add(tokenString);
                break;
            case '[' :
            case ']' :
            case ',' :
            case ':' :
            case '{' :
            case '}' :
                // Here we have a delimiter.
                this.tokens.add(Character.toString(c));
                this.pos++;
                break;
            default :
                // Here we have an invalid character.  Get its context.
                throw new IOException("Invalid character in JSON stream on line " + this.lineNumber + ".");
            }
        }
    }

     /**
     * This method parses a quoted string.  It is presumed the current position is on the
     * open quote.  It will be moved past the close quote.
     *
     * @return the string extracted from the line
     *
     * @throws IOException
     */
    protected String parseString() throws IOException {
        final int n = this.line.length();
        // Push past the open quote.
        this.pos++;
        // Loop ahead, looking for the close quote.
        boolean closed = false;
        while (this.pos < n && ! closed) {
            char c = line.charAt(this.pos);
            switch (c) {
            case '\\' :
                // Here we have a backslash, so we need to parse the escape.
                this.parseEscape();
                break;
            case '"' :
                // Here we have the close quote.
                closed = true;
                this.pos++;
                break;
            default :
                // Here we have an ordinary character.
                this.buffer.append(c);
                this.pos++;
            }
        }
        String retVal = this.buffer.toString();
        this.buffer.setLength(0);
        return retVal;
    }

    /**
     * Parse an escape sequence.  We need to move the current position past it.
     *
     * @throws IOException
     */
    protected void parseEscape() throws IOException {
        final int n = this.line.length();
        this.pos++;
        if (this.pos >= n)
            throw new IOException("Escape character at end of line " + this.lineNumber + ".");
        char c2 = line.charAt(this.pos);
        switch (c2) {
        case '\\' :
        case '"' :
        case '/' :
            this.buffer.append(c2);
            this.pos++;
            break;
        case 'b' :
            this.buffer.append("\b");
            this.pos++;
            break;
        case 'f' :
            this.buffer.append("\f");
            this.pos++;
            break;
        case 'n' :
            this.buffer.append("\n");
            this.pos++;
            break;
        case 'r' :
            this.buffer.append("\r");
            this.pos++;
            break;
        case 't' :
            this.buffer.append("\t");
            this.pos++;
            break;
        case 'u' :
            this.pos++;
            int end = this.pos + 4;
            if (end >=  n)
                throw new IOException("Too few digits in \\u construction on line " + this.lineNumber + ".");
            try {
                char c3 = (char) Integer.parseInt(this.line.substring(this.pos, end), 16);
                this.buffer.append(c3);
            } catch (NumberFormatException e) {
                throw new IOException("Invalid \\u construction digits \"" + this.line.substring(this.pos, end) + " on line "
                        + this.lineNumber + ".");
            }
            this.pos = end;
            break;
        default :
            throw new IOException("Invalid escape sequence in quoted string on line " + this.lineNumber + ".");
        }
    }

    /**
     * This method parses an unquoted token.  The position is on the first character, which is
     * passed in.  We absorb everything up to the first whitespace or JSON delimiter (":", "]", "}", ",").
     * The current position will end on this whitespace/delimited character.
     *
     * @param c		first character of unquoted token
     *
     * @return the unquoted token string
     */
    private String parseUnquoted(char c) {
        this.buffer.append(c);
        this.pos++;
        final int n = line.length();
        boolean done = false;
        while (this.pos < n && ! done) {
            char c2 = line.charAt(this.pos);
            if (Character.isWhitespace(c2))
                done = true;
            else if (DELIMS.indexOf(c2) >= 0)
                done = true;
            else {
                this.buffer.append(c2);
                this.pos++;
            }
        }
        String retVal = this.buffer.toString();
        this.buffer.setLength(0);
        // We need to handle the special case of the unquoted token "null".  We never want to see this,
        // but we encode it as an empty string.
        if (retVal.contentEquals("null"))
            retVal = "";
        return retVal;
    }

    @Override
    public Iterator<String> iterator() {
        return tokens.iterator();
    }

}
