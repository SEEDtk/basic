/**
 *
 */
package org.theseed.io;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * This is an iterable, streamable object for a master directory of genome directories. Each genome
 * directory must be a subdirectory of the master, with the genome ID for its name.
 *
 * @author Bruce Parrello
 *
 */
public class MasterGenomeDir implements Iterable<File> {

    // FIELDS
    /** list of subdirectories */
    private File[] dirList;
    /** genome directory name match pattern */
    private static final Pattern GENOME_ID_PATTERN = Pattern.compile("\\d+\\.\\d+");
    /** genome sub-directory file filter */
    private static final FileFilter GENOME_SUB_DIR_FILTER = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            // The file must be a directory.
            boolean retVal = pathname.isDirectory();
            // The file's name must be a genome ID.
            if (retVal)
                retVal = GENOME_ID_PATTERN.matcher(pathname.getName()).matches();
            return retVal;
        }
    };

    /**
     * This is a simple iterable through the subdirectory array.
     */
    public class Iter implements Iterator<File> {

        /** current position in array */
        private int pos;

        /**
         * Construct an iterable through the subdirectory array.
         */
        public Iter() {
            this.pos = 0;
        }

        @Override
        public boolean hasNext() {
            return (this.pos < MasterGenomeDir.this.size());
        }

        @Override
        public File next() {
            File retVal = MasterGenomeDir.this.dirList[this.pos];
            this.pos++;
            return retVal;
        }

    }

    /**
     * Construct a genome-directory master directory object.
     *
     * @param dir	name of the master directory
     */
    public MasterGenomeDir(File dir) {
        this.dirList = dir.listFiles(GENOME_SUB_DIR_FILTER);
    }

    /**
     * @return the number of genome subdirectories
     */
    public int size() {
        return this.dirList.length;
    }

    @Override
    public Iterator<File> iterator() {
        return this.new Iter();
    }

    /**
     * @return a stream through the genome subdirectories
     */
    public Stream<File> stream() {
        return Arrays.stream(this.dirList);
    }

    /**
     * @return a parallel stream through the genome subdirectories
     */
    public Stream<File> parallelStream() {
        return Arrays.stream(this.dirList).parallel();
    }

}
