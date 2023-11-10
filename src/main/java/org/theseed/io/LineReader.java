/**
 *
 */
package org.theseed.io;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * This is a simple, iterable line reader than can be directly created from a file or a stream.
 *
 * @author Bruce Parrello
 *
 */
public class LineReader implements Iterable<String>, Iterator<String>, Closeable, AutoCloseable {

    /** underlying buffered reader */
    private BufferedReader reader;
    /** TRUE if end-of-file has been read */
    private boolean eof;
    /** next line to produce */
    private String nextLine;
    /** file name for error messages */
    private String fileName;

    /**
     * Create a line reader for the specified input file.
     *
     * @param inputFile		input file to read, or NULL to read the standard input
     *
     * @throws IOException
     */
    public LineReader(File inputFile) throws IOException {
        Reader streamReader;
        if (inputFile != null) {
            streamReader = new FileReader(inputFile);
            this.fileName = inputFile.toString();
        } else {
            streamReader = new InputStreamReader(System.in);
            this.fileName = "standard input";
        }
        setup(streamReader);
    }

    /**
     * Create a line reader for the specified input stream.
     *
     * @param inputStream	input stream to read
     *
     * @throws IOException
     */
    public LineReader(InputStream inputStream) throws IOException {
        this.fileName = "text file input stream";
        Reader streamReader = new InputStreamReader(inputStream);
        setup(streamReader);
    }

    /**
     * Read a string set from a tab-delimited file.  The strings to be put in the set are
     * taken from the first column.
     *
     * @param inFile	input file to read
     *
     * @return a set of the values in the first column of the file
     *
     * @throws IOException
     */
    public static Set<String> readSet(File inFile) throws IOException {
        return new HashSet<String>(readList(inFile));
    }

    /**
     * Read a string list from a tab-delimited file.  The strings to be put in the list are
     * taken from the first column.  Order is preserved.
     *
     * @param inFile	input file to read
     *
     * @return a list of the values in the first column of the file
     *
     * @throws IOException
     */
    public static List<String> readList(File inFile) throws IOException {
        List<String> retVal = new ArrayList<String>();
        try (LineReader reader = new LineReader(inFile)) {
            for (String line : reader) {
                String value = StringUtils.substringBefore(line, "\t");
                retVal.add(value);
            }
        }
        return retVal;
    }


    /**
     * Initialize this file for reading.
     *
     * @param streamReader	reader for the input stream
     *
     * @throws IOException
     */
    private void setup(Reader streamReader) throws IOException {
        this.reader = new BufferedReader(streamReader);
        this.eof = false;
        this.nextLine = null;
    }

    @Override
    public Iterator<String> iterator() {
        return this;
    }

    /**
     * @return the name of this file
     */
    public String getName() {
        return this.fileName;
    }

    /**
     * @return TRUE if another line is available
     */
    @Override
    public boolean hasNext() {
        boolean retVal = false;
        if (this.nextLine != null) {
            // Here we have a next line and it has not been consumed.
            retVal = true;
        } else if (! this.eof) {
            // Here we need to check for a next line.
            this.readAhead();
            if (this.nextLine == null) {
                this.eof = true;
            } else {
                retVal = true;
            }
        }
        return retVal;
    }

    /**
     * Get the next line of input into the next-line buffer.
     */
    private void readAhead() {
        try {
            this.nextLine = this.reader.readLine();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * @return the next line in the file, or NULL at end-of-file
     */
    @Override
    public String next() {
        String retVal = this.nextLine;
        if (retVal != null) {
            // Denote the next line has been consumed.
            this.nextLine = null;
        } else if (! this.eof) {
            // Here we do not have an available next line, but there may be another one.  We read it and consume it
            // in one operation.
            this.readAhead();
            retVal = this.nextLine;
            this.nextLine = null;
        }
        if (retVal == null)
            throw new NoSuchElementException("Premature end-of-file in " + this.fileName + ".");
        return retVal;
    }

    @Override
    public void close() throws IOException {
        this.reader.close();
    }

    /**
     * This object is iterable over the current section of the file, and returns tabbed fields.
     * A marker value of NULL means the section extends to end-of-file.
     */
    public class Section implements Iterable<String[]> {

        // FIELDS
        /** end-of-section marker string */
        private String marker;
        /** field delimiter */
        private String delim;

        /**
         * Construct an iterable for the next section of this file.
         *
         * @param marker	end-of-section marker
         */
        public Section(String marker) {
            this.marker = marker;
            this.delim = "\t";
        }

        /**
         * Construct an iterable for the next section of this file.
         *
         * @param marker	end-of-section marker
         * @param delim		field delimiter
         */
        public Section(String marker, String delim) {
            this.marker = marker;
            this.delim = delim;
        }

        @Override
        public Iterator<String[]> iterator() {
            return LineReader.this.new SectionIter(marker, delim);
        }

    }
    /**
     * This class iterates until a marker or end-of-file is found, and returns tabbed fields.
     * The marker is consumed.  A marker value of NULL means the section goes to end-of-file.
     */
    public class SectionIter implements Iterator<String[]> {

        // FIELDS
        /** TRUE if we have consumed an end-of-section marker */
        private boolean completed;
        /** end-of-section marker */
        private String marker;
        /** field delimiter */
        private String delim;

        /**
         * Construct a new iterator.
         *
         * @param marker	end-of-section marker
         * @param delim		field delimiter
         */
        public SectionIter(String marker, String delim) {
            this.marker = marker;
            this.delim = delim;
            this.completed = false;
        }

        @Override
        public boolean hasNext() {
            boolean retVal = ! this.completed;
            if (retVal) {
                // Here we may have more stuff to read.  Check the file status.
                retVal = LineReader.this.hasNext();
                if (retVal) {
                    // After a call to hasNext, the read-ahead buffer is filled.
                    // Look for a marker.
                    if (isMarker(LineReader.this.nextLine)) {
                        // Here we are at end-of-section.  Denote there is no next record
                        // and consume the read-ahead buffer.
                        retVal = false;
                        this.completed = true;
                        LineReader.this.nextLine = null;
                    }
                }
            }
            return retVal;
        }

        /**
         * @return TRUE if the specified line is the marker
         *
         * @param line	line to check
         */
        public boolean isMarker(String line) {
            return StringUtils.equals(line, this.marker);
        }

        @Override
        public String[] next() {
            String[] retVal;
            if (this.completed)
                throw new NoSuchElementException("Premature end-of-file in " + LineReader.this.fileName + ".");
            else {
                String buffer = LineReader.this.next();
                if (isMarker(buffer)) {
                    this.completed = true;
                    throw new NoSuchElementException("Premature end-of-file in " + LineReader.this.fileName + ".");
                } else {
                    retVal = StringUtils.splitPreserveAllTokens(buffer, this.delim);
                }
            }
            return retVal;
        }

    }

    /**
     * Skip the current section in the file.
     *
     * @param marker	end-of-section marker string (cannot be NULL)
     */
    public void skipSection(String marker) {
        while (this.hasNext() && ! this.next().contentEquals(marker));
    }

}
