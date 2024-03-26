/**
 *
 */
package org.theseed.io.template.output;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.io.FieldInputStream;

/**
 * This template writer saves the template output in a hash that can later
 * be interrogated by the $include directive.  It can also be used to store
 * choice lists.
 */
public class TemplateHashWriter implements ITemplateWriter {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(TemplateHashWriter.class);
    /** master hash -- fileName -> key -> string */
    private Map<String, Map<String, List<String>>> masterHash;
    /** choice lists -- type -> choice set */
    private Map<String, Set<String>> choiceLists;

    /**
     * Construct a template hash writer.
     */
    public TemplateHashWriter() {
        // We expect few file names, so we use a tree map at the high level.
        this.masterHash = new TreeMap<String, Map<String, List<String>>>();
        // We expect slightly more choice lists.
        this.choiceLists = new HashMap<String, Set<String>>();
        // Create a choice list for yes/no.
        this.choiceLists.put("YesNo", Set.of("Yes", "No"));
    }

    @Override
    public void write(String fileName, String key, String outString) throws IOException {
        // Get the sub-hash for this file.
        Map<String, List<String>> subHash = this.masterHash.computeIfAbsent(fileName, x -> new HashMap<String, List<String>>());
        // Get the string list for this key.
        List<String> valueList = subHash.computeIfAbsent(key, x -> new ArrayList<String>(2));
        // Store the template string, using the provided key.
        valueList.add(outString);
    }

    /**
     * @return the template strings for the specifed file name and key, or an empty list if none exist
     *
     * @param fileName	name of the input file for the desired string
     * @param key		key value of the desired string
     */
    public List<String> getStrings(String fileName, String key) {
        Map<String, List<String>> subHash = this.masterHash.get(fileName);
        List<String> retVal;
        if (subHash == null)
            retVal = Collections.emptyList();
        else
            retVal = subHash.getOrDefault(key, Collections.emptyList());
        return retVal;
    }

    @Override
    public void close() {
        // This is an in-memory structure.  No action is needed.
    }

    @Override
    public void readChoiceLists(File fileName, String... fields) throws IOException {
        // Open the input file.
        try (FieldInputStream inStream = FieldInputStream.create(fileName)) {
            // Get the field indices.
            int[] idxes = new int[fields.length];
            for (int i = 0; i < fields.length; i++)
                idxes[i] = inStream.findField(fields[i]);
            // Set up the string sets.
            List<Set<String>> sets = IntStream.range(0, fields.length).mapToObj(i -> new TreeSet<String>()).collect(Collectors.toList());
            // Now read the file.
            for (var line : inStream) {
                for (int i = 0; i < fields.length; i++) {
                    String value = line.get(idxes[i]);
                    if (! StringUtils.isBlank(value))
                        sets.get(i).add(value);
                }
            }
            // With all the choice sets created, we add the sets.
            for (int i = 0; i < fields.length; i++) {
                Set<String> set = sets.get(i);
                log.info("{} items added to choice list for {}.", set.size(), fields[i]);
                this.choiceLists.put(fields[i], set);
            }
        }
    }

    /**
     * @return the choice set for the specified set name, or NULL if there is none
     *
     * @param name	name of the desired choice list
     */
    public Set<String> getChoices(String name) {
        return this.choiceLists.get(name);
    }

    @Override
    public long getTokenCount() {
        // We cache stuff in memory, so the return is always 0 words.
        return 0L;
    }

}
