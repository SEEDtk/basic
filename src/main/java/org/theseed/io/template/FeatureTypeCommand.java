/**
 *
 */
package org.theseed.io.template;

import java.io.IOException;

import org.theseed.io.FieldInputStream;
import org.theseed.io.FieldInputStream.Record;

/**
 * This command forms a sentence predicate to describe a feature type, including virus feature types.
 *
 * @author Bruce Parrello
 *
 */
public class FeatureTypeCommand extends PrimitiveTemplateCommand {

    // FIELDS
    /** column index for input field to use */
    private int colIdx;

    /**
     * Construct a feature type command.
     *
     * @param template	controlling line template
     * @param inStream	template input stream
     * @param parms		parameter string containing the input column name
     *
     * @throws IOException
     */
    public FeatureTypeCommand(LineTemplate template, FieldInputStream inStream, String parms) throws IOException {
        super(template);
        this.colIdx = inStream.findField(parms);
    }

    @Override
    protected String translate(Record line) {
        // Get the feature type.
        String fType = line.get(this.colIdx);
        String retVal;
        switch (fType) {
        case "CDS" :
            retVal = "is a protein-producing coding region";
            break;
        case "gene" :
            retVal = "is a protein-producing gene";
            break;
        case "source" :
            retVal = "is a source";
            break;
        case "5'UTR" :
            retVal = "is a 5'-end untranslated region";
            break;
        case "mRNA" :
            retVal = "produces messenger RNA";
            break;
        case "3'UTR" :
            retVal = "is a 3'-end untranslated region";
            break;
        case "misc_feature" :
        case "unsure" :
        case "misc_difference" :
            retVal = "is an unknown type of feature";
            break;
        case "misc_RNA" :
            retVal = "produces miscellaneous RNA";
            break;
        case "assembly_gap" :
        case "gap" :
            retVal = "represents a gap";
            break;
        case "intron" :
            retVal = "is an intron";
            break;
        case "sig_peptide" :
            retVal = "produces a signal peptide";
            break;
        case "mat_peptide" :
            retVal = "produces a mature peptide";
            break;
        case "primer_bind" :
            retVal = "is a primer binding";
            break;
        case "stem_loop" :
            retVal = "is a stem loop";
            break;
        default :
            retVal = "is a " + fType + " feature";
        }
        return retVal;
    }

    @Override
    protected String getName() {
        return "ftype";
    }

}
