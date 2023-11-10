/**
 *
 */
package org.theseed.io.template;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;
import org.theseed.io.FieldInputStream.Record;
import org.theseed.io.template.cols.FieldExpression;

import com.github.cliftonlabs.json_simple.Jsoner;

/**
 * This command generates a JSON string in the form of a single-element hash.  The parameters are
 * colon-separated (as always), and consist of a field type ("list" or "string"), a literal tag to
 * use as the key, and a field expression for the field value.
 *
 * @author Bruce Parrello
 *
 */
public class JsonCommand extends PrimitiveTemplateCommand {

    // FIELDS
    /** field expression for the output */
    private FieldExpression valueExpr;
    /** type of output */
    private ValueType vType;
    /** quoted tag name */
    private String tag;
    /** map of type names to types */
    private static final Map<String, ValueType> TYPE_MAP = Map.of("string", ValueType.STRING, "list", ValueType.LIST);

    /**
     * This enum describes the various JSON output types we support.
     */
    protected static enum ValueType {
        /** JSON string list */
        LIST {
            @Override
            public String emit(Record record, FieldExpression expr) {
                // Get the list values.
                List<String> values = expr.getList(record);
                // Emit it as a JSON list with the elements quoted.
                String retVal = values.stream().map(x -> quote(x)).collect(Collectors.joining(", ", "[", "]"));
                return retVal;
            }
        },

        /** singleton string */
        STRING {
            @Override
            public String emit(Record record, FieldExpression expr) {
                // Get the string value.
                String value = expr.get(record);
                // Emit it quoted.
                return quote(value);
            }

        };

        /**
         * Emit the value string for a JSON value of this type.
         *
         * @param record	current input line
         * @param expr		field expression to generate the value
         *
         * @return the value string in JSON format.
         */
        public abstract String emit(FieldInputStream.Record record, FieldExpression expr);

        /**
         * @return a quoted version of the specified string
         *
         * @param value		unquoted string to convert
         */
        private static String quote(String value) {
            return "\"" + Jsoner.escape(value) + "\"";
        }

    }

    /**
     * Construct a JSON command.
     *
     * @param lineTemplate	controlling master template
     * @param inStream		field input stream
     * @param parms			parameter string
     *
     * @throws ParseFailureException
     */
    public JsonCommand(LineTemplate lineTemplate, FieldInputStream inStream, String parms) throws ParseFailureException {
        super(lineTemplate);
        // Split up the parameters.
        String[] pieces = StringUtils.split(parms, ':');
        if (pieces.length < 2 || pieces.length > 3)
            throw new ParseFailureException("\"$json\" command must have two or three parameters.");
        // Save the type.
        this.vType = TYPE_MAP.get(pieces[0]);
        if (this.vType == null)
            throw new ParseFailureException("Invalid $json type code \"" + pieces[0] + "\".");
        // Quote and save the tag.
        this.tag = ValueType.quote(pieces[1]);
        // The default for the third parameter is the second.
        String fieldName;
        if (pieces.length == 2)
            fieldName = pieces[1];
        else
            fieldName = pieces[2];
        // Compile the value expression.
        this.valueExpr = FieldExpression.compile(lineTemplate, inStream, fieldName);
    }

    @Override
    protected String translate(Record line) {
        // Compute the JSON value.
        String value = this.vType.emit(line, this.valueExpr);
        // Format it for output.
        String retVal = "{ " + this.tag + ":" + value + " }";
        return retVal;
    }

    @Override
    protected String getName() {
        return "json";
    }

}
