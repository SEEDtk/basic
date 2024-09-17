/**
 *
 */
package org.theseed.io;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * This object is used to represent an attribute value in a data-based in-memory database.
 * The value is stored as a list of strings, and can be evaluated as a list, a string,
 * an integer, a floating-point number, or a boolean.
 *
 * Note that a record contains fields that can be updated, while Attribute objects
 * are read-only.
 *
 * @author Bruce Parrello
 *
 */
public class Attribute {

    // FIELDS
    /** field value */
    private List<String> values;
    /** delimiter for converting lists into single strings */
    public static final String DELIM = "::";
    /** empty string list */
    protected static final List<String> EMPTY_LIST = Collections.emptyList();

    /**
     * Construct an attribute from a field in an input record.
     *
     * @param record	input data record
     * @param idx		input column index
     */
    public Attribute(FieldInputStream.Record record, int idx) {
        this.values = record.getList(idx);
        // Insure we always have a list.
        if (this.values == null)
            this.values = EMPTY_LIST;
    }

    /**
     * Construct a null attribute.
     */
    public Attribute() {
        this.values = EMPTY_LIST;
    }

    /**
     * @return this attribute as a string
     */
    public String get() {
        return StringUtils.join(this.values, DELIM);
    }

    /**
     * @return this value as an integer, or 0 if it is invalid
     */
    public int getInt() {
        String value = this.get();
        int retVal;
        if (value.isBlank()) {
            retVal = 0;
        } else try {
            retVal = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            retVal = 0;
        }
        return retVal;
    }

    /**
     * @return this value as a floating-point number, or 0 if it is invalid
     */
    public double getDouble() {
        String value = this.get();
        double retVal;
        if (value == null || value.isBlank()) {
            retVal = 0;
        } else try {
            retVal = Double.parseDouble(value);
        } catch (NumberFormatException e) {
            retVal = 0;
        }
        return retVal;
    }

    /**
     * @return TRUE if the specified column contains a TRUE value, else FALSE
     */
    public boolean getFlag() {
        return Attribute.analyzeBoolean(this.values);
    }

    /**
     * @return this value as a string list
     */
    public List<String> getList() {
        return this.values;
    }

    /**
     * This method analyzes a list of strings and interprets it as TRUE or FALSE. Empty lists, blanks,
     * zero-values, and strings that look false are FALSE. Everything else is TRUE.
     *
     * @param value		value to interpret
     *
     * @return the boolean interpretation of a value
     */
    public static boolean analyzeBoolean(List<String> value) {
        boolean retVal;
        if (value.size() > 1)
            retVal = true;
        else if (value.size() < 1)
            retVal = false;
        else {
            String text = value.get(0);
            if (text.isBlank())
                retVal = false;
            else {
                String normalized = text.toLowerCase();
                switch (normalized) {
                case "n" :
                case "no" :
                case "0" :
                case "f" :
                case "false" :
                    retVal = false;
                    break;
                default :
                    retVal = true;
                }
            }
        }
        return retVal;
    }

}
