package org.theseed.json;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;

import org.apache.commons.lang3.Strings;
import org.theseed.io.LineReader;

import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

/**
 * This iterator moves through records in a JSON list file. We will eat the open bracket,
 * then absorb from brace to brace, and convert that part into a JsonObject. This continues
 * until we hit the close bracket.
 */
public class JsonListIterator implements AutoCloseable, Iterator<JsonObject> {

    // FIELDS
    /** input line reader */
    private final LineReader inStream;
    /** current JSON token buffer */
    private final StringBuilder tokenBuffer;
    /** current line number */
    private int lineCount;
    /** next record to return, or NULL if we are at the end */
    private JsonObject nextJsonObject;
    /** currently-active tokenizer */
    private JsonTokenizer tokenizer;
    /** currently-active token iterator */
    private Iterator<String> tokenIterator;

    /**
     * Construct a JSON list iterator for the specified file.
     * 
     * @param fileName  name of the file containing the JSON list text
     * 
     * @throws IOException
     */
    public JsonListIterator(File fileName) throws IOException {
        // Initialize the token buffer.
        this.tokenBuffer = new StringBuilder(200);
        // Open the file for line input.
        this.inStream = new LineReader(fileName);
        // Do we have any data at all?
        if (! this.inStream.hasNext()) {
            // No. Go straight to end-of-file status.
            this.nextJsonObject = null;
            this.lineCount = 0;
        } else {
            // Read the first line and build a tokenizer for it.
            String line = this.inStream.next();
            this.tokenizer = new JsonTokenizer.Raw(line, 1);
            this.tokenIterator = this.tokenizer.iterator();
            this.lineCount = 1;
            // Insure we have the open bracket.
            if (! this.tokenIterator.hasNext())
                this.nextJsonObject = null;
            else {
                // Eat the open bracket.
                String token = this.tokenIterator.next();
                if (! token.equals("["))
                    throw new IOException("File " + fileName + " does not appear to be a JSON list file.");
                // Now we are positioned on the first record. Read it in.
                this.nextJsonObject = this.readRecord();
            }
        }
    }

    /**
     * Read the next JSON record and return it. At the end, the iterator will be positioned at the delimiter
     * before the next record, which should be a comma or a close bracket. If there is no active tokenizer, 
     * we are at end-of-file and return NULL.
     * 
     * @return the JSON object for the record read, or NULL if we are at end-of-file
     * 
     * @throws IOException 
     */
    private JsonObject readRecord() throws IOException {
        JsonObject retVal;
        String token = this.getNextToken();
        if (token == null) {
            // No more tokens means we are at the end.
            retVal = null;
        } else {
            // Skip past the comma (if any).
            if (Strings.CS.equals(token, ","))
                token = this.getNextToken();
            if (Strings.CS.equals(token, "]")) {
                // Here the next record is the actual end of the list.
                retVal = null;
                this.tokenizer = null;
            } else if (! token.equals("{"))
                throw new IOException("Expected open brace on line " + lineCount + " but found \"" + token + "\".");
            else {
                // Initialize the token buffer with the open brace.
                this.tokenBuffer.setLength(0);
                this.tokenBuffer.append("{");
                // Denote we have one unclosed brace.
                int braceLevel = 1;
                boolean done = false;
                // Consume the rest of the record.
                while (! done) {
                    // Get the next token and process it.
                    token = this.getNextToken();
                    if (token == null)
                        done = true;
                    else {
                        // We have a token. Stash it and check for braces.
                        this.tokenBuffer.append(token);
                        if (token.equals("{"))
                            braceLevel++;
                        else if (token.equals("}")) {
                            braceLevel--;
                            if (braceLevel == 0) {
                                // Here we are at the end of the record.
                                done = true;
                            }
                        }
                    }
                }
                // We have our record in the token buffer. Convert it to JSON.
                try {
                    retVal = (JsonObject) Jsoner.deserialize(this.tokenBuffer.toString());
                } catch (JsonException e) {
                    // Convert a JSON error to an IO exception.
                    throw new IOException("JSON error during conversion before line " + lineCount + ": " + e.toString());
                }
            }
        }
        return retVal;
    }


    /**
     * Get the next JSON token in the input stream.
     * 
     * @return the token string, or NULL if we are at the end
     * 
     * @throws IOException 
     * 
     */
    private String getNextToken() throws IOException {
        String retVal = null;
        // Insure there is a token available.
        while (this.tokenizer != null && ! this.tokenIterator.hasNext()) {
            // We need a new line. If there is none, we are done.
            if (! this.inStream.hasNext())
                this.tokenizer = null;
            else {
                String line = this.inStream.next();
                this.lineCount++;
                this.tokenizer = new JsonTokenizer.Raw(line, this.lineCount);
                this.tokenIterator = this.tokenizer.iterator();
            }
        }
        // If the tokenizer is gone, we are at the end, so only proceed if we have one.
        if (this.tokenizer != null) {
            // Get the next token.
            retVal = this.tokenIterator.next();
        }
        return retVal;
    }

    @Override
    public void close() throws IOException {
        if (this.inStream != null)
            this.inStream.close();
    }

    @Override
    public boolean hasNext() {
        return this.nextJsonObject != null;
    }

    @Override
    public JsonObject next() {
        JsonObject retVal = this.nextJsonObject;
        try {
            this.nextJsonObject = this.readRecord();
        } catch (IOException e) {
            // Convert to unchecked to fit interface requirements.
            throw new UncheckedIOException(e);
        }
        return retVal;
    }

}
