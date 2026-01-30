/**
 *
 */
package org.theseed.magic;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.theseed.counters.CountMap;

import murmur3.MurmurHash3.LongPair;

/**
 * This object creates magic IDs, a readable short form of a long string and
 * uses them to store named objects.  It mimics a map of IDs to names, but
 * it does not allow new items to be added via the map interface.
 *
 * @author Bruce Parrello
 *
 */
public class MagicMap<T extends MagicObject> implements Map<String, String>, Iterable<T> {

    // FIELDS
    /** map from prefixes to the next usable suffix number */
    private final CountMap<String> suffixMapper;
    /** map from ids to objects */
    private final Map<String, T> idMapper;
    /** map from checksums to objects */
    private final Map<LongPair, T> checkMapper;
    /** dummy object for lookups (this object CANNOT be modified; we just use it to call methods) */
    private final T searchObj;
    /** list of aliases */
    private final List<T> aliases;
    /** set of little words */
    private static final HashSet<String> LITTLE_WORDS =
            Stream.of("and", "or", "the", "a", "of", "in", "an", "to", "on", "").collect(Collectors.toCollection(HashSet::new));
    /** parentheticals */
    private static final Pattern PARENTHETICAL = Pattern.compile("\\(.*?\\)");
    /** things that are not digits and letters */
    private static final String PUNCTUATION = "\\W+";
    /** separate prefix from suffix */
    private static final Pattern ID_PARSER = Pattern.compile("(\\S+?)(\\d+)");

    /**
     * Create a new, blank magic ID table.
     *
     * @param searchObject	a sample, read-only table item used to call item methods
     */
    public MagicMap(T searchObject) {
        this.suffixMapper = new CountMap<>();
        this.idMapper = new HashMap<>();
        this.checkMapper = new HashMap<>();
        this.searchObj = searchObject;
        this.aliases = new ArrayList<>();
    }

    /**
     * Condense a string into a magic ID.  Note there will be no
     * suffix.
     *
     * @param full	full string to be condensed
     *
     * @return a shorter representation of the string
     */
    public static String condense(String full) {
        // Remove outer parentheses, if any.
        String noParens = deparenthesize(full);
        // Remove remaining parentheticals.
        noParens = RegExUtils.replaceAll((CharSequence) noParens.toLowerCase(),
                PARENTHETICAL, " ");
        // Separate into words.
        String[] words = noParens.split(PUNCTUATION);
        // Loop through the words, putting them into the output.
        StringBuilder retVal = new StringBuilder(16);
        for (int wordIdx = 0; retVal.length() < 16 && wordIdx < words.length; wordIdx++) {
            String thisWord = words[wordIdx];
            // Note we skip little words.
            if (! LITTLE_WORDS.contains(thisWord)) {
                // Capitalize the first letter and shrink to four characters.
                retVal.append(Character.toUpperCase(thisWord.charAt(0)));
                for (int i = 1; i < 4 && i < thisWord.length(); i++) {
                    retVal.append(Character.toLowerCase(thisWord.charAt(i)));
                }
            }
        }
       return retVal.toString();
    }

    /**
     * Remove the outer parentheses from a string (if any).
     *
     * @param full	full string to parse
     *
     * @return the original string with outer parentheses removed (if any), or the original string (if not)
     */
    public static String deparenthesize(String full) {
        String noParens = full;
        if (full.startsWith("(") && full.endsWith(")")) {
            int level = 1;
            int i = 1;
            final int n = full.length() - 1;
            boolean error = false;
            while (i < n && ! error) {
                char c = full.charAt(i);
                switch (c) {
                case '(' -> level++;
                case ')' -> {
                    level--;
                    if (level < 1) error = true;
                    }
                }
                i++;
            }
            if (! error)
                noParens = full.substring(1, n);
        }
        return noParens;
    }

    /**
     * Associate a pre-generated ID with an object.
     *
     * @param obj	object to be mapped to the ID
     */
    public void register(T obj) {
        // Get the ID and name
        String id = obj.getId();
        String name = obj.getName();
        // Check for an existing object for this name.
        T found = this.getByName(name);
        if (found == null) {
            // No existing object.  See if this is an alias.
            found = this.getItem(id);
            if (found != null) {
                // Here we have an alias.  Save the association.
                this.aliases.add(obj);
                // Denote we want to associate the alias with this checksum.
                found = obj;
            } else {
                // We have a new object. Parse out the prefix and suffix.
                Matcher m = ID_PARSER.matcher(id);
                String prefix, suffixString;
                if (m.matches()) {
                    prefix = m.group(1);
                    suffixString = m.group(2);
                } else {
                    prefix = id;
                    suffixString = "";
                }
                int suffix = (suffixString.isEmpty() ? 0 : Integer.parseInt(suffixString));
                // Insure this suffix is not reused.
                CountMap<String>.Count suffixCounter = this.suffixMapper.findCounter(prefix);
                if (suffixCounter == null)
                    this.suffixMapper.count(prefix, suffix + 1);
                else if (suffixCounter.getCount() <= suffix)
                    this.suffixMapper.setCount(prefix, suffix + 1);
                // Associate the value with the ID.
                this.idMapper.put(id, obj);
                // Remember it as the found object.
                found = obj;
            }
            // Update the checksum map.
            this.checkMapper.put(obj.getChecksum(), found);
        }
    }

    /**
     * @return the number of objects in this map (including aliases)
     */
    public int fullSize() {
        return this.idMapper.size() + this.aliases.size();
    }


    /**
     * @return TRUE if this map has nothing in it
     */
    @Override
    public boolean isEmpty() {
        return this.idMapper.isEmpty();
    }

    /**
     * @return TRUE if this map has an object with the given ID
     *
     * @param key	ID to check
     */
    @Override
    public boolean containsKey(Object key) {
        return this.idMapper.containsKey(key);
    }

    /**
     * @return TRUE if this map has the given object's key in it
     *
     * @param obj	object for which to search
     */
    public boolean containsValue(T value) {
        return this.idMapper.containsKey(value.getId());
    }

    /**
     * @return TRUE if this map has an object with the given name in it.
     *
     * @param name	name of the object for which to search
     */
    public boolean containsName(String name) {
        return (this.getByName(name) != null);
    }

    /**
     * @return the object with the given ID, or NULL if it does not exist
     *
     * @param key	ID of interest
     */
    public T getItem(String key) {
        return this.idMapper.get(key);
    }

    /**
     * Store the specified object in this map.  If the object has no ID,
     * one will be created.
     *
     * @param 	value	object to store
     * @return	the original object
     */
    public T put(T value) {
        if (value.getId() == null) {
            this.storeNew(value);
        }
        this.register(value);
        return value;
    }

    /**
     * Generate an ID for the specified object and store it in the object,
     * then add the object to the map.
     *
     * @param value	object for which a magic ID is desired
     */
    private void storeNew(T value) {
        String prefix = condense(value.getName());
        int minSuffix = 0;
        // Does the prefix end with a digit?
        String end = StringUtils.right(prefix, 1);
        if (StringUtils.isNumeric(end)) {
            // Insure it doesn't.
            prefix += "n";
            // The suffixes start with 1 in this case.
            minSuffix = 1;
        }
        // Get the new suffix and append it.
        int suffix = this.suffixMapper.getCount(prefix);
        if (suffix < minSuffix) suffix = minSuffix;
        String id = (suffix > 0 ? prefix + suffix : prefix);
        // Update the suffix map. Note that we don't use 1 as a suffix except for
        // the "n"-prefix case.
        int newSuffix = (suffix < 2 ? 2 : suffix + 1);
        this.suffixMapper.setCount(prefix, newSuffix);
        // Update the target object.
        value.setId(id);
        // Update the master map.
        this.idMapper.put(id, value);
        // Update the checksum map.
        this.checkMapper.put(value.getChecksum(), value);
    }

    /**
     * Remove the objects with the specified key.  Note this requires also removing them from the checksum map.
     *
     * @param key	key of objects to remove
     *
     * @return the primary removed object
     */
    public T remove(String key) {
        T retVal = idMapper.remove(key);
        this.checkMapper.remove(retVal.getChecksum());
        // Clear any aliases from the alias list.
        for (int i = this.aliases.size() - 1; i >=0; i--) {
            T other = this.aliases.get(i);
            if (other.getId().contentEquals(retVal.getId())) {
                this.aliases.remove(i);
                this.checkMapper.remove(other.getChecksum());
            }
        }
        return retVal;
    }

    /**
     * Copy the objects into this magic ID mapping.
     *
     * @param m
     */
    public void putAll(Collection<T> m) {
        for (T value : m) {
            this.put(value);
        }
    }

    /**
     * Erase everything in this mapping.  This will cause IDs to be reused.
     */
    @Override
    public void clear() {
        this.suffixMapper.clear();
        this.idMapper.clear();
    }

    /**
     * @return a list of role IDs
     */
    @Override
    public Set<String> keySet() {
        return this.idMapper.keySet();
    }

    /**
     * @return all the objects in this map
     */
    public Collection<T> objectValues() {
        ArrayList<T> retVal = new ArrayList<>(this.fullSize());
        retVal.addAll(this.idMapper.values());
        retVal.addAll(this.aliases);
        return retVal;
    }

    /**
     * @return the name of the object with the specified key, or NULL if it does
     * 		   not exist
     *
     * @param key	key to check
     */
    public String getName(String key) {
        T obj = this.getItem(key);
        return (obj == null ? null : obj.getName());
    }

    /**
     * @return the object with the specified name, or NULL if there is none
     *
     * @param name	name of the desired object
     */
    public T getByName(String name) {
        // Get the checksum.
        LongPair checksum = searchObj.getChecksum(name);
        // Try to find it in the checksum map.
        T retVal = this.checkMapper.get(checksum);
        return retVal;
    }

    @Override
    public boolean containsValue(Object value) {
        boolean retVal = false;
        if (value instanceof String string) {
            retVal = (this.getByName(string) != null);
        }
        return retVal;
    }

    @Override
    public String get(Object key) {
        String retVal = null;
        T target = this.idMapper.get(key);
        if (target != null)
            retVal = target.getName();
        return retVal;
    }

    @Override
    public String put(String key, String value) {
        throw new UnsupportedOperationException("Cannot use interface put on a Magic Map.");
    }

    @Override
    public String remove(Object key) {
        String retVal = null;
        if (key instanceof String string) {
            T target = this.getItem(string);
            if (target != null) {
                this.idMapper.remove(key);
                this.checkMapper.remove(target.getChecksum());
                for (T alias : this.aliases) {
                    if (alias.getId().contentEquals(string))
                        this.aliases.remove(alias);
                }
                retVal = target.getName();
            }
        }
        return retVal;
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> m) {
        throw new UnsupportedOperationException("Cannot use interface putAll on a Magic Map.");
    }

    @Override
    public Collection<String> values() {
        return this.idMapper.values().stream().map(x -> x.getName()).collect(Collectors.toList());
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        Set<Entry<String, String>> retVal = this.idMapper.values().stream()
                .map(x -> new AbstractMap.SimpleEntry<String, String>(x.getId(), x.getName())).collect(Collectors.toSet());
        return retVal;
    }

    @Override
    public int size() {
        return this.idMapper.size();
    }

    /**
     * Find the list of all objects associated with a particular ID
     *
     * @param key	ID to process
     *
     * @return a (possibly empty) list of all the aliases for an ID, with the primary first
     */
    public List<T> getAllById(String key) {
        // Create an empty return list.
        List<T> retVal = new ArrayList<>(5);
        // Get the primary.
        T obj = this.getItem(key);
        if (obj != null) {
            retVal.add(obj);
            // Search for aliases.
            for (T alias : this.aliases) {
                if (alias.getId().contentEquals(key))
                    retVal.add(alias);
            }
        }
        return retVal;
    }

    /**
     * This is an iterator for all the objects in this map.
     */
    public class Iter implements Iterator<T> {

        /** current iterator */
        private Iterator<T> current;
        /** TRUE if we're iterating the map and not the aliases */
        private boolean inMap;

        public Iter() {
            this.current = MagicMap.this.idMapper.values().iterator();
            this.inMap = true;
        }

        @Override
        public boolean hasNext() {
            boolean retVal = current.hasNext();
            if (! retVal && inMap) {
                reposition();
                retVal = current.hasNext();
            }
            return retVal;
        }

        /**
         * Switch from iterating the map to the alias list.
         */
        protected void reposition() {
            this.current = MagicMap.this.aliases.iterator();
            this.inMap = false;
        }

        @Override
        public T next() {
            // Insure we're positioned on the correct iterator.
            if (! current.hasNext() && this.inMap)
                reposition();
            return current.next();
        }

    }

     @Override
    public Iterator<T> iterator() {
        return this.new Iter();
    }


}
