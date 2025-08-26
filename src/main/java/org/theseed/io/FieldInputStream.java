/**
 *
 */
package org.theseed.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.theseed.json.JsonListInputStream;

/**
 * This is the base class for field-oriented input streams.  These could be tab-delimited or JSON.
 * Each field is either stored as a string or a list of strings (so not all JSON files are legal).
 *
 * Unlike a standard TabbedLineReader (which is otherwise very similar), fields in this stream can only be
 * referenced by name, not position.  For flat files, a header record is read in that determines the allowable
 * fields.  For JSON files, field names are added as they are requested.  Once iteration through the file
 * starts, new fields cannot be added.
 *
 * @author Bruce Parrello
 *
 */
public abstract class FieldInputStream implements AutoCloseable, Iterable<FieldInputStream.Record>, Iterator<FieldInputStream.Record> {

    // FIELDS
    /** list of field names found in each record, in order */
    private List<String> fieldNames;
    /** active line reader */
    private LineReader reader;
    /** current input stream */
    private Iterator<String> lineIter;
    /** TRUE if field names are locked, else FALSE */
    private boolean fieldsLocked;
    /** ordinal number of current line */
    private int lineNumber;

    /**
     * Enumeration for types of field-input stream files.
     */
    public static enum Type {

        /** tab-delimited file */
        TABBED {
            @Override
            public boolean matches(File inFile) {
                return checkExtensions(inFile, ".txt", ".tbl", ".tab", ".tsv");
            }

            @Override
            public FieldInputStream open(File inFile) throws IOException {
                return new TabbedInputStream(inFile);
            }
        },

        /** JSON list file */
        JSON {
            @Override
            public boolean matches(File inFile) {
                return checkExtensions(inFile, ".json");
            }

            @Override
            public FieldInputStream open(File inFile) throws IOException {
                return new JsonListInputStream(inFile);
            }
        };

        /**
         * @return TRUE if the file matches this type of stream, else FALSE
         *
         * @param inFile	name of the file to test
         */
        public abstract boolean matches(File inFile);

        /**
         * @return an open stream of this type for the file
         *
         * @param inFile	name of the file to open
         *
         * @throws IOException
         */
        public abstract FieldInputStream open(File inFile) throws IOException;

    }

    /**
     * This object represents a single record of data.
     */
    public class Record {

        /** list of fields */
        private final List<List<String>> fields;

        /**
         * Construct a record from an array of strings.
         *
         * @param fieldString	array of strings to put in the fields
         */
        public Record(String[] fieldStrings) {
            this.fields = new ArrayList<>(fieldStrings.length);
            for (String field : fieldStrings) {
                if (StringUtils.isBlank(field))
                    this.fields.add(Attribute.EMPTY_LIST);
                else {
                    List<String> value = Arrays.asList(StringUtils.split(field, Attribute.DELIM));
                    this.fields.add(value);
                }
            }
        }

        /**
         * Construct an empty record with space for a specified number of fields.
         *
         * @param n		number of fields to expect
         */
        public Record(int n) {
            this.fields = new ArrayList<>(n);
            for (int i = 0; i < n; i++)
                this.fields.add(Attribute.EMPTY_LIST);
        }

        /**
         * Add a string to the field list.  If the name is not one of the known field names, nothing happens.
         *
         * @param name		name of field
         * @param string	string to add
         */
        public void setField(String name, String string) {
            int idx = FieldInputStream.this.fieldNames.indexOf(name);
            if (idx >= 0 && ! StringUtils.isBlank(string))
                this.fields.set(idx, List.of(string));
        }

        /**
         * Add a string list to the field list.  If the name is not one of the known field names, nothing happens.
         *
         * @param name		name of field
         * @param list		string list to add
         */
        public void setField(String name, List<String> list) {
            int idx = FieldInputStream.this.fieldNames.indexOf(name);
            if (idx >= 0 && list != null)
                this.fields.set(idx, list);
        }

        /**
         * @return the field at the specified column index as a string
         *
         * @param colIdx	index of desired field
         */
        public String get(int colIdx) {
            return this.getString(colIdx);
        }

        /**
         * This converts a column to a string.  An empty column (containing an empty list)
         * comes back as an empty string.  A singleton comes back unchanged.  Everything else
         * is joined with the delimiter.
         *
         * @return the data item in the specified column as a string
         *
         * @param colIdx	index of the desired column
         */
        private String getString(int colIdx) {
            if (colIdx < 0 || colIdx >= fields.size())
                throw new IllegalArgumentException("Invalid column index " + Integer.toString(colIdx) + " used for field-input stream.");
            return StringUtils.join(this.fields.get(colIdx), Attribute.DELIM);
        }

        /**
         * @return an integer value for the field at the specified column index
         *
         * @param colIdx	index of the desired column
         */
        public int getInt(int colIdx) {
            String colValue = this.getString(colIdx);
            int retVal = 0;
            if (colValue != null && ! colValue.isEmpty()) {
                try {
                    retVal = Integer.parseInt(colValue);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid value \"" + colValue + "\" in numeric input column.");
                }
            }
            return retVal;
        }

        /**
         * @return a floating-point value for the field at the specified column index
         *
         * @param colIdx	index of the desired column
         */
        public double getDouble(int colIdx) {
            double retVal;
            String colValue = this.getString(colIdx);
            if (colValue.isEmpty())
                retVal = Double.NaN;
            else try {
                retVal = Double.parseDouble(colValue);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid value \"" + colValue + "\" in numeric input column.");
            }
            return retVal;
        }

        /**
         * @return TRUE if the specified column contains a TRUE value, else FALSE
         *
         * @param colIdx	index of the desired column
         */
        public boolean getFlag(int colIdx) {
            if (colIdx < 0 || colIdx >= fields.size())
                throw new IllegalArgumentException("Invalid column index " + Integer.toString(colIdx) + " used for field-input stream.");
            List<String> colValue = this.fields.get(colIdx);
            boolean retVal = Attribute.analyzeBoolean(colValue);
            return retVal;
        }

        /**
         * @return the specified column as a string list
         *
         * @param colIdx	index of the desired column
         */
        public List<String> getList(int colIdx) {
            if (colIdx < 0 || colIdx >= fields.size())
                throw new IllegalArgumentException("Invalid column index " + Integer.toString(colIdx) + " used for field-input stream.");
            return this.fields.get(colIdx);
        }

        /**
         * Add an empty-list field at the end.  This is used to insure we have enough fields.
         */
        protected void addField() {
            this.fields.add(Attribute.EMPTY_LIST);
        }

    }

    /**
     * This method opens a field-input stream, using the filename extension to determine the file
     * type.
     *
     * @param inFile	name of the input file to open
     *
     * @return a field-input stream for the specified file
     *
     * @throws IOException
     */
    public static FieldInputStream create(File inFile) throws IOException {
        FieldInputStream retVal = null;
        Type[] types = Type.values();
        for (int i = 0; i < types.length && retVal == null; i++) {
            if (types[i].matches(inFile))
                retVal = types[i].open(inFile);
        }
        if (retVal == null)
            throw new IOException("File '\"" + inFile.getName() + "\" is not a recognized field-input file type.");
        return retVal;
    }

    /**
     * Open a field input stream on a file.
     *
     * @param inputFile		file containing the data records to return
     *
     * @throws IOException
     */
    public FieldInputStream(File inputFile) throws IOException {
        this.reader = new LineReader(inputFile);
        this.setup();
    }

    /**
     * Open a field input stream on a stream.
     *
     * @param inputStream	input stream containing the data records to return
     *
     * @throws IOException
     */
    public FieldInputStream(InputStream inputStream) throws IOException {
        this.reader = new LineReader(inputStream);
        this.setup();
    }

    /**
     * Initialize the reading process for this stream.
     */
    private void setup() {
        this.lineIter = this.reader.iterator();
        // Initialize the field-name list.
        this.fieldNames = new ArrayList<>();
        // Denote that field names are unlocked.
        this.fieldsLocked = false;
        // Denote that we have not read any lines.
        this.lineNumber = 0;
    }

    /**
     * This method will find the index for the field with the specified name.  If the field
     * does not exist, it may throw an IOException; however, for some file types the field names
     * cannot be pre-determined, so the exception will never happen; instead, the field always
     * returns a default value-- 0, false, empty string, empty list, etc.
     *
     * @param fieldName		name of the field whose index is desired
     *
     * @return the array index associated with the specified field name
     *
     * @throws IOException
     */
    public abstract int findField(String fieldName) throws IOException;

    /**
     * @return TRUE if there is another line in the file, else FALSE
     */
    protected boolean hasNextLine() {
        return this.lineIter.hasNext();
    }

    /**
     * @return the next line in the file
     */
    protected String nextLine() {
        this.lineNumber++;
        return this.lineIter.next();
    }

    /**
     * @return an iterator for this stream
     */
    @Override
    public Iterator<Record> iterator() {
        this.fieldsLocked = true;
        return this;
    }

    @Override
    public boolean hasNext() {
        return this.hasNextLine();
    }

    @Override
    public void close() {
        // Close the underlying line reader.  Note we convert any IO exception to unchecked.
        try {
            this.reader.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * @return the field-name list
     */
    protected List<String> getFieldNames() {
        return this.fieldNames;
    }

    /**
     * Add a new name to the field-name list and return its index.  We convert
     * the name to lower-case to speed up field-name searches.
     *
     * @param fieldName		name to add
     *
     * @return the index the field name will have
     */
    protected int addFieldName(String fieldName) {
        String normalized = fieldName.toLowerCase();
        int retVal = this.fieldNames.size();
        this.fieldNames.add(normalized);
        return retVal;
    }

    /**
     * @return TRUE if the specified file has one of the listed extensions, else FALSE
     *
     * @param inFile	file to check
     * @param exts		array of filename extensions to check (included the period)
     */
    private static boolean checkExtensions(File inFile, String... exts) {
        final String name = inFile.getName();
        return Arrays.stream(exts).anyMatch(x -> name.endsWith(x));
    }

    /**
     * @return the index of a specified field in the field name list, or -1 if the field is not found
     *
     * @param name		name to look for
     */
    protected int findColumn(String name) {
        int retVal;
        if (name == null)
            retVal = -1;
        else {
            OptionalInt idx = IntStream.range(0, this.fieldNames.size())
                    .filter(i -> isName(name, this.fieldNames.get(i))).findFirst();
            retVal = idx.orElse(-1);
        }
        return retVal;
    }

    /**
     * @return TRUE if the specified name matches the specified field name, else FALSE
     *
     * @param name			name to check
     * @param fieldName		fieldName to check it against
     */
    public static boolean isName(String name, String fieldName) {
        String lcName = name.toLowerCase();
        return name.equals(fieldName) || StringUtils.endsWith(fieldName, "." + lcName);
    }

    /**
     * @return TRUE if field names can no longer be added
     */
    protected boolean areFieldsLocked() {
        return this.fieldsLocked;
    }

    /**
     * @return the number of the current line
     */
    public int getLineNumber() {
        return this.lineNumber;
    }

    /**
     * @return the current number of defined fields
     */
    public int width() {
        return this.fieldNames.size();
    }

}
