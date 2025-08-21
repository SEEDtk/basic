/**
 *
 */
package org.theseed.json;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.Jsoner;

/**
 * This object facilitates looping through all the JSON files in a JSON dump directory.
 *
 * @author Bruce Parrello
 *
 */
public class JsonFileDir implements Iterable<File> {

    // FIELDS
    /** logging facility */
    private static final Logger log = LoggerFactory.getLogger(JsonFileDir.class);
    /** array of JSON files in this directory */
    private File[] jsonFiles;
    /** JSON file filter */
    private FileFilter JSON_FILE_FILTER = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            boolean retVal = pathname.isFile();
            if (retVal) {
                String fileName = pathname.getName();
                retVal = fileName.endsWith(".json");
            }
            return retVal;
        }
    };

    /**
     * This is a simple iterator for looping through the JSON files.
     */
    protected class Iter implements Iterator<File> {

        /** current position in the file array */
        private int pos;

        /**
         * Construct the iterator.
         */
        public Iter() {
            this.pos = 0;
        }

        @Override
        public boolean hasNext() {
            return (this.pos < JsonFileDir.this.jsonFiles.length);
        }

        @Override
        public File next() {
            File retVal = JsonFileDir.this.jsonFiles[this.pos];
            this.pos++;
            return retVal;
        }

    }

    /**
     * Construct a JSON dump iterable for the specified directory.
     *
     * @param inDir		JSON dump directory to parse
     */
    public JsonFileDir(File inDir) {
        // Get the JSON file list.
        this.jsonFiles = inDir.listFiles(JSON_FILE_FILTER);
    }

    @Override
    public Iterator<File> iterator() {
        return this.new Iter();
    }

    /**
     * @return the number of files in this directory
     */
    public int size() {
        return this.jsonFiles.length;
    }

    /**
     * Extract a JSON array from a JSON dump file.
     *
     * @param jsonFile	json file to read
     *
     * @return the file contents as a JSON array
     *
     * @throws IOException
     * @throws JsonException
     */
    public static JsonArray getJson(File jsonFile) throws IOException, JsonException {
        JsonArray retVal;
        try (FileReader reader = new FileReader(jsonFile)) {
            retVal = (JsonArray) Jsoner.deserialize(reader);
        }
        return retVal;
    }

    /**
     * Write a JSON array to an output print writer.
     *
     * @param outJson	JSON array to write
     * @param writer	output print writer
     */
    public static void writeJson(JsonArray outJson, PrintWriter writer) {
        log.debug("Writing json for {}-element list.", outJson.size());
        writer.println("[");
        Iterator<Object> iter = outJson.iterator();
        boolean moreLeft = iter.hasNext();
        while (moreLeft) {
            Object nextJson = iter.next();
            String jsonString = Jsoner.serialize(nextJson);
            if (! iter.hasNext()) {
                moreLeft = false;
                writer.println("    " + jsonString);
            } else
                writer.println("    " + jsonString + ",");
        }
        writer.println("]");
    }
}
