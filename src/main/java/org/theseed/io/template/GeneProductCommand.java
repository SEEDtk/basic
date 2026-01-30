/**
 *
 */
package org.theseed.io.template;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;
import org.theseed.roles.RoleUtilities;

/**
 * This command takes two column names as input and presumes the columns contain a gene product string.
 * and a feature type, respectively.  The columns are decoded into descriptive text and then output.
 *
 * @author Bruce Parrello
 *
 */
public class GeneProductCommand extends PrimitiveTemplateCommand {

    // FIELDS
    /** index of the column containing the gene product string */
    private int prodColIdx;
    /** index of the column containing the feature type code */
    private int typeColIdx;
    /** map of protein abbreviations to names for tRNA */
    private static final Map<String, String> AMINO_ACIDS = Map.ofEntries(
            new AbstractMap.SimpleEntry<>("Ala", "Alanine"),
            new AbstractMap.SimpleEntry<>("Arg", "Arginine"),
            new AbstractMap.SimpleEntry<>("Asn", "Asparagine"),
            new AbstractMap.SimpleEntry<>("Asp", "Aspartic acid"),
            new AbstractMap.SimpleEntry<>("Cys", "Cysteine"),
            new AbstractMap.SimpleEntry<>("Glu", "Glutamic acid"),
            new AbstractMap.SimpleEntry<>("Gln", "Glutamine"),
            new AbstractMap.SimpleEntry<>("Gly", "Glycine"),
            new AbstractMap.SimpleEntry<>("His", "Histidine"),
            new AbstractMap.SimpleEntry<>("Ile", "Isoleucine"),
            new AbstractMap.SimpleEntry<>("Leu", "Leucine"),
            new AbstractMap.SimpleEntry<>("Lys", "Lysine"),
            new AbstractMap.SimpleEntry<>("Met", "Methionine"),
            new AbstractMap.SimpleEntry<>("Phe", "Phenylalanine"),
            new AbstractMap.SimpleEntry<>("Pro", "Proline"),
            new AbstractMap.SimpleEntry<>("Ser", "Serine"),
            new AbstractMap.SimpleEntry<>("Thr", "Threonine"),
            new AbstractMap.SimpleEntry<>("Trp", "Tryptophan"),
            new AbstractMap.SimpleEntry<>("Tyr", "Tyrosine"),
            new AbstractMap.SimpleEntry<>("Val", "Valine"));
    /** split pattern for rRNAs */
    private static final Pattern RNA_SPLITTER = Pattern.compile("(?:\\s+#+|;)\\s+");
    /** RNA string finder for rRNAs */
    private static final Pattern RNA_NAMER = Pattern.compile("\\b(?:ribosomal\\s+)?r?RNA\\b", Pattern.CASE_INSENSITIVE);
    /** tRNA product parser */
    private static final Pattern T_RNA_PARSER = Pattern.compile("tRNA-(\\w{3,3})(?:-([A-Z]{3,3})|-\\d+)?");
    /** tRNA alternate product parser */
    private static final Pattern T_RNA_PARSER2 = Pattern.compile("(\\w{3,3})\\s+tRNA.*");
    /** pseudo-tRNA product parser */
    private static final Pattern T_RNA_PSEUDO = Pattern.compile("tRNA-Pseudo-([A-Z]{3,3})");
    /** array of roman numeral indices */
    private static final String[] ROMAN_IDX = new String[] { "i", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix", "x" };
    /** pattern for descriptive @-pieces */
    private static final Pattern DESCRIPTIVE = Pattern.compile("\\@\\s+\\w+\\-containing", Pattern.CASE_INSENSITIVE);
    /** subtype pattern */
    private static final Pattern SUBTYPE_ROLE = Pattern.compile("(.+)\\s+=>\\s+(.+)");
    /** mean size of a product description */
    private static final int DEFAULT_PRODUCT_SIZE = 40;


    /**
     * Construct a gene-product translation command.
     *
     * @param columnName	index (1-based) or name of source column
     * @param inStream		main input stream
     *
     * @throws IOException
     * @throws ParseFailureException
     */
    public GeneProductCommand(LineTemplate template, FieldInputStream inStream, String columnName)
            throws IOException, ParseFailureException {
        super(template);
        String[] tokens = StringUtils.split(columnName, ':');
        if (tokens.length != 2)
            throw new ParseFailureException("$product command requires exactly two column names-- the product column and the feature type column.");
        this.prodColIdx = inStream.findField(tokens[0]);
        this.typeColIdx = inStream.findField(tokens[1]);
        this.setEstimatedLength(DEFAULT_PRODUCT_SIZE);
    }

    @Override
    public String translate(FieldInputStream.Record line) {
        String product = line.get(this.prodColIdx);
        String type = line.get(this.typeColIdx);
        String retVal;
        // Each type has a different approach.
        retVal = switch (type) {
            case "tRNA" -> this.processTRna(product);
            case "rRNA" -> this.processRRna(product);
            case "misc_RNA" -> this.processMiscRna(product);
            case "CDS" -> this.processProtein(product);
            default -> this.processOther(type);
        };
        return retVal;
    }

    /**
     * @return a string describing this miscellaneous feature type
     *
     * @param type	type of feature to describe
     */
    private String processOther(String type) {
        // Convert underscores to spaces.
        String typeDesc = StringUtils.replaceChars(type, '_', ' ');
        // Return the description.
        return prefixArticle(typeDesc);
    }

    /**
     * @return the specified phrase with an indefinite article in front of it.
     *
     * @param phrase	input phrase
     */
    public static String prefixArticle(String phrase) {
        // Compute the article.
        String article;
        article = switch (Character.toLowerCase(phrase.charAt(0))) {
            case 'a', 'e', 'i', 'o', 'u', '8' -> "an";
            default -> "a";
        };
        return article + " " + phrase;
    }

    /**
     * @return a description of a miscellaneous RNA feature
     *
     * @param product	supplied product string
     */
    private String processMiscRna(String product) {
        String retVal;
        if (StringUtils.isBlank(product)) {
            // Here we have no product string.
            retVal = "a miscellaneous RNA";
        } else {
            // Here we have a product string to use.
            String productString = RoleUtilities.commentFree(product);
            retVal = "a miscellaneous RNA believed to be " + productString;
        }
        return retVal;
    }

    /**
     * This method produces the description of a ribosomal RNA.
     *
     * @param product	supplied product string
     *
     * @return a sentence describing the ribosomal RNA
     */
    private String processRRna(String product) {
        // Use the product to determine the type of ribosomal RNA.
        String retVal;
        if (StringUtils.isBlank(product)) {
            retVal = "an unknown ribosomal RNA";
        } else if (RoleUtilities.LSU_R_RNA.matcher(product).find()) {
            // Here we have an LSU.
            retVal = "a large subunit ribosomal RNA";
        } else if (RoleUtilities.SSU_R_RNA.matcher(product).find()) {
            // Here we have the venerable SSU.
            retVal = "a 16S small subunit ribosomal RNA";
        } else {
            // Break up the product and return the longest portion.
            String[] pieces = RNA_SPLITTER.split(product);
            String longestPiece = pieces[0];
            for (int i = 1; i < pieces.length; i++) {
                if (pieces[i].length() > longestPiece.length())
                    longestPiece = pieces[i];
            }
            // Look for a substring that names the product as an RNA.
            var m = RNA_NAMER.matcher(longestPiece);
            if (! m.find()) {
                // Here there is no such substring.
                retVal = "a ribosomal RNA of type " + longestPiece;
            } else {
                // Here we must replace the substring with the full version of the name.
                String tail = (m.end() + 1 < longestPiece.length() ?
                        longestPiece.substring(m.end() + 1) : "");
                retVal = prefixArticle(longestPiece.substring(0, m.start()) + "ribosomal RNA" + tail);
            }
        }
        return retVal;
    }

    /**
     * This method produces the description of a transfer RNA.
     *
     * @param product	relevant product string
     *
     * @return a text description of the transfer RNA
     */
    private String processTRna(String product) {
        String retVal;
        if (StringUtils.isBlank(product)) {
            retVal = "an unknown type of transfer RNA";
        } else {
            // Parse the product.  If we have a match, group 1 is the amino acid abbreviation, and if group 2 exists, it
            // is the codon.
            var m = T_RNA_PARSER.matcher(product);
            if (m.matches()) {
                // Here we have the standard format.  Translate the amino acid name.
                String aa = AMINO_ACIDS.get(m.group(1));
                if (aa == null)
                    aa = "an unknown amino acid " + m.group(1);
                String codon = "";
                if (m.group(2) != null)
                    codon = " from codon " + m.group(2);
                retVal = "a transfer RNA for " + aa + codon;
            } else {
                m = T_RNA_PARSER2.matcher(product);
                if (m.matches()) {
                    // Here we have the alternate format.  Again, we translate the amino acid name.
                    String aa = AMINO_ACIDS.get(m.group(1));
                    if (aa == null)
                        aa = "an unknown amino acid " + m.group(1);
                    retVal = "a transfer RNA for " + aa;
                } else {
                    m = T_RNA_PSEUDO.matcher(product);
                    if (m.matches()) {
                        // Here we have a pseudo-transfer RNA.
                        retVal = "a pseudo-transfer RNA for codon " + m.group(1);
                    } else
                        retVal = "a transfer RNA of unknown type";
                }
            }
        }
        return retVal;
    }

    /**
     * This is the most complicated product translator-- protein-coding regions.  Protein-coding regions can have comments,
     * multi-functional roles, and EC and TC numbers.
     *
     * @param product	protein product string
     *
     * @return a text description of the product
     */
    private String processProtein(String product) {
        StringBuilder retVal = new StringBuilder(product.length() * 2);
        // Check for the null case.
        if (StringUtils.isBlank(product) || Strings.CI.equals(product, "hypothetical protein"))
            retVal.append("a hypothetical protein");
        else {
            // Strip off the comment.
            String productBody = RoleUtilities.commentFree(product);
            // Now, we split into domains.  In most cases there is only one.
            String[] pieces = StringUtils.splitByWholeSeparator(productBody, " / ");
            if (pieces.length > 1) {
                // This gets complicated.  We present the alternatives as a numbered list.  Note that the first
                // function is preceded by a colon, and the remaining ones separated by semi-colons.
                retVal.append("a protein with ").append(pieces.length).append(" domains, whose products are: (1) ")
                        .append(this.interpretDomain(pieces[0]));
                final int n = pieces.length - 1;
                for (int i = 1; i < n; i++)
                    retVal.append("; (").append(i+1).append(") ").append(this.interpretDomain(pieces[i]));
                retVal.append("; and (").append(pieces.length).append(") ").append(this.interpretDomain(pieces[n]));
            } else {
                // Here we have a much simpler case:  an umambiguous product.
                retVal.append("a protein whose product is ").append(this.interpretDomain(productBody));
            }
        }
        return retVal.toString();
    }

    /**
     * This method produces a product description for a single domain, that is, the portion of a product
     * that is between the " / " delimiters, or is the only product.  We split the function into ambiguous possibilities
     * and then produce a description for each one.
     *
     * @param domainString	the domain string to interpret
     *
     * @return a descriptive phrase for the domain product (most commonly the domain string itself)
     */
    private String interpretDomain(String domainString) {
        StringBuilder retVal = new StringBuilder(domainString.length() * 2);
        // We need to split by "; ".  If we only have one piece, then the product is unambiguous.
        String[] pieces = StringUtils.splitByWholeSeparator(domainString, "; ");
        if (pieces.length > 1) {
            retVal.append("an ambiguous function with ").append(pieces.length).append(" possibilities, including (a) ")
                    .append(this.interpretFunction(pieces[0]));
            final int n = pieces.length - 1;
            for (int i = 1; i < n; i++) {
                retVal.append(", (").append((char) ('a' + i)).append(") ")
                    .append(this.interpretFunction(pieces[i]));
            }
            retVal.append(", or (").append((char) ('a' + n)).append(") ")
                    .append(this.interpretFunction(pieces[n]));
        } else
            retVal.append(this.interpretFunction(domainString));
        return retVal.toString();
    }

    /**
     * This method produces a product description for a single function, that is, the portion of a product
     * that is between the "; " delimiters, or is the only domain product.  We split the function into roles
     * and then produce a description for each one.  The function can have multiple roles, split by " @ ", though
     * in specific cases, the delimiter indicates a single role with a comment.  If one role is a substring of
     * another, it is suppressed.
     *
     * @param functionString	the function string to interpret
     *
     * @return a descriptive phrase for the function (most commonly the function string itself)
     */
    private String interpretFunction(String functionString) {
        StringBuilder retVal = new StringBuilder(functionString.length() * 2);
        // Try the @-split.
        String[] pieces = StringUtils.splitByWholeSeparator(functionString, " @ ");
        // A singleton is returned unchanged.
        if (pieces.length == 1)
            retVal.append(this.interpretRole(functionString));
        else if (pieces.length == 2 && DESCRIPTIVE.matcher(pieces[1]).matches()) {
            // Here we have a descriptive string for the role.
            retVal.append(this.interpretRole(pieces[0])).append( "(").append(pieces[1]).append(")");
        } else if (pieces.length == 2 && Strings.CS.startsWith(pieces[1], pieces[0])) {
            // Here we have a generic function followed by an identical detailed one.
            retVal.append(this.interpretRole(pieces[1]));
        } else {
            // Here we have a multifunctional domain.
            retVal.append(pieces.length).append(" roles including (i) ").append(this.interpretRole(pieces[0]));
            final int n = pieces.length - 1;
            for (int i = 1; i < n; i++)
                retVal.append(", (").append(ROMAN_IDX[i]).append(") ").append(this.interpretRole(pieces[i]));
            retVal.append(", and (").append(ROMAN_IDX[n]).append(") ").append(this.interpretRole(pieces[n]));
        }
        return retVal.toString();
    }

    /**
     * This method interprets a role.  The only fancy thing is that we may need to handle the " => "
     * construct, which indicates a subtype.
     *
     * @param roleString	the role string to interpret
     *
     * @return a descriptive phrase for the role (most commonly the role string itself)
     */
    private String interpretRole(String roleString) {
        String retVal;
        // Check for the " => " construct.
        Matcher m = SUBTYPE_ROLE.matcher(roleString);
        if (m.matches()) {
            // Here we have the subtype construct.
            retVal = m.group(1) + ", a subtype of " + m.group(2);
        } else
            retVal = roleString;
        return retVal;
    }

    @Override
    protected String getName() {
        return "product";
    }

}
