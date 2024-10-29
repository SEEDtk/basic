/**
 *
 */
package org.theseed.io;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonKey;
import com.github.cliftonlabs.json_simple.JsonObject;

/**
 * @author Bruce Parrello
 *
 */
class TestJsonFileDir {

    private static final Set<String> FILES = Set.of("epitope.json", "genome_feature.json", "genome_sequence.json",
            "genome.json", "protein_feature.json", "protein_structure.json");

    /** This enum defines the keys used and their default values.
     */
    public static enum FeatureKeys implements JsonKey {
        AA_LENGTH(0),
        FEATURE_TYPE("CDS"),
        PRODUCT(""),
        GENOME_ID(""),
        GENOME_NAME(""),
        LOCATION(""),
        PATRIC_ID("");

        private final Object m_value;

        FeatureKeys(final Object value) {
            this.m_value = value;
        }

        /** This is the string used as a key in the incoming JsonObject map.
         */
        @Override
        public String getKey() {
            return this.name().toLowerCase();
        }

        /** This is the default value used when the key is not found.
         */
        @Override
        public Object getValue() {
            return this.m_value;
        }

    }

    @Test
    void testJsonFileDir() {
        File mainDir = new File("data", "11159.6");
        JsonFileDir looper = new JsonFileDir(mainDir);
        assertThat(looper.size(), equalTo(FILES.size()));
        for (File filer : looper) {
            String name = filer.getName();
            assertThat(name, filer.getParentFile(), equalTo(mainDir));
            assertThat(name, filer.canRead(), equalTo(true));
            assertThat(name, in(FILES));
        }
    }

    @Test
    void testJsonRead() throws IOException, JsonException {
        File mainJson = new File("data/11168.4", "genome_feature.json");
        JsonArray json = JsonFileDir.getJson(mainJson);
        assertThat(json.size(), equalTo(62));
        int found = 0;
        for (Object jsonObj : json) {
            JsonObject jsonItem = (JsonObject) jsonObj;
            String fid = (String) jsonItem.getStringOrDefault(FeatureKeys.PATRIC_ID);
            if (fid.contentEquals("fig|11168.4.CDS.3")) {
                // Here we have the sample feature we want.
                found++;
                assertThat(jsonItem.getInteger(FeatureKeys.AA_LENGTH), equalTo(224));
                assertThat(jsonItem.getStringOrDefault(FeatureKeys.FEATURE_TYPE), equalTo("CDS"));
                assertThat(jsonItem.getStringOrDefault(FeatureKeys.GENOME_ID), equalTo("11168.4"));
                assertThat(jsonItem.getStringOrDefault(FeatureKeys.GENOME_NAME), equalTo("Mumps virus strain Jeryl Lynn"));
                assertThat(jsonItem.getStringOrDefault(FeatureKeys.LOCATION), equalTo("1979..2653"));
                assertThat(jsonItem.getStringOrDefault(FeatureKeys.PRODUCT), equalTo("V protein"));
            }
        }
        assertThat(found, equalTo(1));
        // Write the JSON and read it back.
        File outJsonFile = new File("data", "output.ser");
        try (PrintWriter writer = new PrintWriter(outJsonFile)) {
            JsonFileDir.writeJson(json, writer);
        }
        JsonArray json2 = JsonFileDir.getJson(outJsonFile);
        assertThat(json.size(), equalTo(json2.size()));
        for (int i = 0; i < json.size(); i++) {
            JsonObject item1 = (JsonObject) json.get(i);
            JsonObject item2 = (JsonObject) json2.get(i);
            String label = "JsonObject[" + i + "]";
            assertThat(label, item1.size(), equalTo(item2.size()));
            for (var entry1 : item1.entrySet()) {
                String key = entry1.getKey();
                Object value1 = entry1.getValue();
                String entryLabel = label + "{" + key + "}";
                assertThat(entryLabel, value1, not(nullValue()));
                assertThat(entryLabel, item2.get(key), equalTo(value1));
            }
        }

    }
}
