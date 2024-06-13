/**
 *
 */
package org.theseed.counters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 *
 * This is a simple map from objects of a specified type to numbers. It is used for simple counters.
 *
 * This class will fail if the string representations of two unequal objects are the same.
 *
 * @author Bruce Parrello
 *
 */
public class CountMap<K> {

    /** underlying hash map */
    private HashMap<K, Count> map;

    /**
     * Utility class to encapsulate the count.
     */
    public class Count implements Comparable<Count> {
        private K key;
        private int num;

        /**
         * Create a new count.
         *
         * @param key	key being counted
         */
        private Count(K key) {
            this.key = key;
            this.num = 0;
        }

        /**
         * @return the key counted by this counter.
         */
        public K getKey() {
            return this.key;
        }

        /**
         * @return the value counted for this key
         */
        public int getCount() {
            return this.num;
        }

        /**
         * The sort order is highest count to lowest count.  If counts are equal but the keys
         * are not, we need to compare different.  In that case, we sort by the string
         * representation.
         */
        @Override
        public int compareTo(CountMap<K>.Count o) {
            // Note we compare the counts in reverse.
            int retVal = o.num - this.num;
            if (retVal == 0) {
                retVal = this.key.toString().compareTo(o.key.toString());
            }
            return retVal;
        }

        /**
         * The string representation includes the key and the count.
         */
        @Override
        public String toString() {
            String retVal;
            switch (this.num) {
            case 0:
                retVal = this.key.toString() + " (not found)";
                break;
            case 1:
                retVal = this.key.toString() + " (1 occurrence)";
                break;
            default:
                retVal = String.format("%s (%d occurrences,)", this.key.toString(),
                        this.num);
            }
            return retVal;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getEnclosingInstance().hashCode();
            result = prime * result + ((key == null) ? 0 : key.hashCode());
            result = prime * result + num;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            @SuppressWarnings("unchecked")
            Count other = (Count) obj;
            if (!getEnclosingInstance().equals(other.getEnclosingInstance()))
                return false;
            if (key == null) {
                if (other.key != null)
                    return false;
            } else if (!key.equals(other.key))
                return false;
            if (num != other.num)
                return false;
            return true;
        }

        private CountMap<K> getEnclosingInstance() {
            return CountMap.this;
        }

        /**
         * Increment the counter value.
         */
        public void increment() {
            this.num++;
        }

    }


    /**
     * Create a blank counting map.
     */
    public CountMap() {
        this.map = new HashMap<K, Count>();
    }

    /**
     * @return the count for a given key (which is 0 if the key is unknown)
     *
     * @param key	key for the counter of interest
     */
    public int getCount(K key) {
        int retVal = 0;
        Count found = this.map.get(key);
        if (found != null) {
            retVal = found.num;
        }
        return retVal;
    }

    /**
     * @return the total of all the counts
     */
    public int getTotal() {
        int retVal = 0;
        for (Count count : this.map.values())
            retVal += count.num;
        return retVal;
    }

    /**
     * Erase all the counts in this map without deleting any keys.
     */
    public void clear() {
        for (Count count : this.map.values()) {
            count.num = 0;
        }
    }

    /**
     * @return the counter object for a specified key.  If none exists, one will be created.
     *
     * @param key	key for the counter of interest
     */
    public Count getCounter(K key) {
        Count retVal;
        retVal = this.map.get(key);
        if (retVal == null) {
            retVal = new Count(key);
            this.map.put(key, retVal);
        }
        return retVal;
    }

    /**
     * @return the counter object for the specified key, or NULL if the key has not been counted
     *
     * @param key	key of interest
     */
    public Count findCounter(K key) {
        return this.map.get(key);
    }

    /** Increment the count for a key and return the new result.
     *
     * @param key	key of interest
     *
     * @return the new count value.
     *
     *  */
    public int count(K key) {
        return this.count(key, 1);
    }

    /** Increment the count for a key and return the new result.
    *
    * @param key	key of interest
    * @param num	number to add to the count
    *
    * @return the new count value.
    *
    *  */
   public int count(K key, int num) {
       Count myCount = this.getCounter(key);
       myCount.num += num;
       return myCount.num;
   }

    /**
     * @return	a sorted collection of all the keys in this object
     */
    public SortedSet<K> keys() {
        TreeSet<K> retVal = new TreeSet<K>(this.map.keySet());
        return retVal;
    }

    /**
     * @return the number of keys in this map
     */
    public int size() {
        return this.map.size();
    }

    /**
     * @return a collection of all the counts in this object, sorted from highest to lowest
     */
    public List<Count> sortedCounts() {
        ArrayList<Count> retVal = new ArrayList<Count>(this.map.values());
        retVal.sort(null);
        return retVal;
    }

    /**
     * @return an unordered list of all the counts
     */
    public Collection<Count> counts() {
        return this.map.values();
    }

    /**
     * Erase all classes and counts from this map.
     */
    public void deleteAll() {
        this.map.clear();
    }

    /**
     * Set the count to a specific value.
     *
     * @param key		key whose count is to be set
     * @param newValue	value of the new count
     */
    public void setCount(K key, int newValue) {
       Count myCount = this.getCounter(key);
       myCount.num = newValue;
    }

    /**
     * @return a set of all the counts with a value of 1.
     */
    public Set<K> getSingletons() {
        Set<K> retVal = new HashSet<K>(this.map.size());
        for (Count counter : this.map.values())
            if (counter.getCount() == 1)
                retVal.add(counter.getKey());
        return retVal;
    }

    /**
     * Accumulate all the counted from a small map into this map.
     *
     * @param otherMap		other map whose counts are to be added
     */
    public void accumulate(CountMap<K> projCounts) {
        projCounts.map.values().stream().forEach(x -> this.count(x.getKey(), x.getCount()));
    }

    /**
     * @return the sum of all the counts in this map
     */
    public int sum() {
        return this.map.values().stream().mapToInt(x -> x.getCount()).sum();
    }

    /**
     * Remove a count from the map.
     *
     * @param key	key of the count
     */
    public void remove(K key) {
        this.map.remove(key);
    }

    /**
     * @return the counter entry with the highest count, or NULL if there are no counts
]	 */
    public Count getBestEntry() {
        Count retVal = null;
        if (this.map.size() > 0)
            retVal = Collections.min(this.map.values());
        return retVal;
    }

}
