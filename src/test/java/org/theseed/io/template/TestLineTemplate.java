/**
 *
 */
package org.theseed.io.template;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Matcher;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.basic.ParseFailureException;
import org.theseed.io.FieldInputStream;
import org.theseed.io.LineReader;
import org.theseed.io.template.output.TemplateHashWriter;

/**
 * @author Bruce Parrello
 *
 */
class TestLineTemplate {

    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(TestLineTemplate.class);


    @Test
    void testPattern() {
        String test1 = "abc{{def}}{{ghi}}jklmn{{op}}";
        Matcher m = LineTemplate.VARIABLE.matcher(test1);
        assertThat(m.matches(), equalTo(true));
        assertThat(m.group(1), equalTo("abc"));
        assertThat(m.group(2), equalTo("def"));
        assertThat(m.group(3), equalTo("{{ghi}}jklmn{{op}}"));
        m = LineTemplate.VARIABLE.matcher(m.group(3));
        assertThat(m.matches(), equalTo(true));
        assertThat(m.group(1).isEmpty(), equalTo(true));
        assertThat(m.group(2), equalTo("ghi"));
        assertThat(m.group(3), equalTo("jklmn{{op}}"));
        m = LineTemplate.VARIABLE.matcher(m.group(3));
        assertThat(m.matches(), equalTo(true));
        assertThat(m.group(1), equalTo("jklmn"));
        assertThat(m.group(2), equalTo("op"));
        assertThat(m.group(3).isEmpty(), equalTo(true));
        m = LineTemplate.VARIABLE.matcher("abd{{def}}ghi");
        assertThat(m.matches(), equalTo(true));
        assertThat(m.group(1), equalTo("abd"));
        assertThat(m.group(2), equalTo("def"));
        m = LineTemplate.VARIABLE.matcher(m.group(3));
        assertThat(m.matches(), equalTo(false));
    }

    @Test
    void testTemplates() throws IOException, ParseFailureException {
        TemplateHashWriter globals = new TemplateHashWriter();
        final String TEMPLATE = "The genome with identifier {{genome_id}} is called {{genome_name}} and has {{genome_length}} base pairs. " +
                "Its NCBI accession number is {{assembly_accession}} and it has {{contigs}} contigs with {{patric_cds}} known protein-coding regions. " +
                "{{$if:host_name}}Its organism is found in the species {{$list:host_name:and:, }}. {{$fi}}{{$if:disease}}The organism is known to cause {{$list:disease}}. {{$fi}}" +
                "{{$group:and}}It belongs to" +
                    "{{$clause:superkingdom}}the domain {{superkingdom}}" +
                    "{{$clause:species}}the species {{species}}" +
                    "{{$clause:genus}}the genus {{genus}}{{$end}}";
        try (var inStream = FieldInputStream.create(new File("data", "genomes10.tbl"));
                var testStream = new LineReader(new File("data", "genomes10.txt"))) {
            LineTemplate xlate = new LineTemplate(inStream, TEMPLATE, globals, null);
            Iterator<String> testIter = testStream.iterator();
            int i = 1;
            for (var line : inStream) {
                String output = xlate.apply(line);
                assertThat(String.format("Line %d",  i), output, equalTo(testIter.next()));
                i++;
            }
        }
    }

    @Test
    void testProducts() throws IOException, ParseFailureException {
        TemplateHashWriter globals = new TemplateHashWriter();
        final String TEMPLATE = "{{$if:type:fid}}{{$product:product:type}}{{$fi}}";
        try (var inStream = FieldInputStream.create(new File("data", "products.tbl"));
                var testStream = new LineReader(new File("data", "products.txt"))) {
            LineTemplate xlate = new LineTemplate(inStream, TEMPLATE, globals, null);
            Iterator<String> testIter = testStream.iterator();
            int i = 1;
            for (var line : inStream) {
                String output = xlate.apply(line);
                String test = testIter.next();
                assertThat(String.format("Line %d", i), output, equalTo(test));
                i++;
            }
        }
    }

    @Test
    void testLocations() throws IOException, ParseFailureException {
        TemplateHashWriter globals = new TemplateHashWriter();
        final String TEMPLATE = "Feature {{patric_id}} is on {{$strand:strand}} at location {{start}} to {{end}}.";
        try (var inStream = FieldInputStream.create(new File("data", "locs.txt"))) {
            int fidIdx = inStream.findField("patric_id");
            int valIdx = inStream.findField("value");
            LineTemplate xlate = new LineTemplate(inStream, TEMPLATE, globals, null);
            for (var record : inStream) {
                String output = xlate.apply(record);
                String fid = record.get(fidIdx);
                String expected = record.get(valIdx);
                assertThat(fid, output, equalTo(expected));
            }
        }
    }

    @Test
    void testGroup() throws IOException, ParseFailureException {
        TemplateHashWriter globals = new TemplateHashWriter();
        String TEMPLATE = "Hello, we have a group{{$group:and:.}} with{{$clause:f1}}one {{f1}}"
                + "{{$clause:f2}}two {{f2}}{{$clause:f3}}three {{f3}}{{$end}}";
        final File groupFile = new File("data", "groups.tbl");
        try (var inStream = FieldInputStream.create(groupFile)) {
            int i = 1;
            int testIdx = inStream.findField("expected");
            LineTemplate xlate = new LineTemplate(inStream, TEMPLATE, globals, null);
            for (var line : inStream) {
                String output = xlate.apply(line);
                String test = line.get(testIdx);
                assertThat(String.format("Line %d", i), output, equalTo(test));
                i++;
            }
        }
        // Here we do the special NL-style group.
        TEMPLATE = "{{$group:nl}}{{$clause:f1}}This is the {{f1}} question."
                + "{{$clause:f2}}This is the {{f2}} question."
                + "{{$clause:f3}}This is the {{f3}} question.{{$end}}";
        try (var inStream = FieldInputStream.create(groupFile)) {
            LineTemplate xlate = new LineTemplate(inStream, TEMPLATE, globals, null);
            var line = inStream.next();
            String output = xlate.apply(line);
            assertThat(output, equalTo(""));
            line = inStream.next();
            output = xlate.apply(line);
            assertThat(output, equalTo("This is the B question."));
            line = inStream.next();
            output = xlate.apply(line);
            assertThat(output, equalTo("This is the A question.\nThis is the C question."));
            line = inStream.next();
            output = xlate.apply(line);
            assertThat(output, equalTo("This is the A question.\nThis is the B question.\nThis is the C question."));
        }
    }

    @Test
    void testInclude() throws IOException, ParseFailureException {
        TemplateHashWriter globals = new TemplateHashWriter();
        globals.write("dlits.txt", "key3", "key3 dlit text");
        globals.write("dlits.txt", "key3", "more key3 dlit text");
        globals.write("dlits.txt", "key3", "another key3 dlit text");
        globals.write("dlits.txt", "key2", "key2 dlit text");
        globals.write("dlits.txt", "key2", "more key2 dlit text");
        globals.write("dlits.txt", "key1", "key1 dlit text");
        globals.write("ilits.txt", "key1", "key1 ilit text");
        final String TEMPLATE = "This is an important test.  We need to know if {{datum}} works. " +
                "Also, we are testing include. {{$if:include(dlits.txt, key)}}" +
                "We have {{$list:include(dlits.txt, key):and}}" +
                "{{$else}}{{$if:list}}We do not have key, but we have {{$list:list}}" +
                "{{$fi}}{{$fi}}.";
        try (var inStream = FieldInputStream.create(new File("data", "includes.tbl"))) {
            LineTemplate xlate = new LineTemplate(inStream, TEMPLATE, globals, null);
            int i = 1;
            int testIdx = inStream.findField("expected");
            for (var line : inStream) {
                String output = xlate.apply(line);
                String test = line.get(testIdx);
                log.info(output);
                assertThat(String.format("Line %d", i), output, equalTo(test));
                i++;
            }
        }
    }

    @Test
    void testLiterals() throws IOException, ParseFailureException {
        TemplateHashWriter globals = new TemplateHashWriter();
        final String TEMPLATE = "This tests the {{fld1}} null ({{$0}}), tab ({{$tab}}), and new-line {{$nl}} commands.";
        try (var inStream = FieldInputStream.create(new File("data", "single.tbl"))) {
            LineTemplate xlate = new LineTemplate(inStream, TEMPLATE, globals, null);
            var line = inStream.next();
            String output = xlate.apply(line);
            assertThat(output, equalTo("This tests the a null (), tab (\t), and new-line \n commands."));
        }
    }

    @Test
    void testRandom() throws IOException, ParseFailureException {
        TemplateHashWriter globals = new TemplateHashWriter();
        File simpleFile = new File("data", "simple.tbl");
        globals.readChoiceLists(simpleFile, "genus", "species");
        final String TEMPLATE = "What is the genus of {{genome}}?{{$tab}}{{$choices:genus:genus:4}}{{$nl}}" +
                    "Is the species of {{genome}} s1?{{$tab}}A) Yes or B) No{{$nl}}" +
                    "What is the species of {{genome}}?{{$tab}}{{$choices:species:species:3}}";
        try (LineReader testStream = new LineReader(new File("data", "simple.txt"));
                var inStream = FieldInputStream.create(simpleFile)) {
            LineTemplate xlate = new LineTemplate(inStream, TEMPLATE, globals, null);
            xlate.setSeed(12345679L);
            int i = 1;
            for (var line : inStream) {
                String output = xlate.apply(line);
                String l1 = testStream.next();
                String l2 = testStream.next();
                String l3 = testStream.next();
                assertThat(Integer.toString(i), output, equalTo(l1 + "\n" + l2 + "\n" + l3));
            }
        }
    }

    @Test
    void testSamples() throws IOException, ParseFailureException {
        TemplateHashWriter globals = new TemplateHashWriter();
        File simpleFile = new File("data", "simple.tbl");
        globals.readChoiceLists(simpleFile, "genus", "species");
        final String TEMPLATE = "The random genera for {{genome}} are {{$list:sample(genus,4)}}. The random species are {{$list:sample(species,4)}}.";
        try (LineReader testStream = new LineReader(new File("data", "sample.txt"));
                var inStream = FieldInputStream.create(simpleFile)) {
            LineTemplate xlate = new LineTemplate(inStream, TEMPLATE, globals, null);
            xlate.setSeed(12345679L);
            for (var line : inStream) {
                String output = xlate.apply(line);
                String l1 = testStream.next();
                assertThat(output, equalTo(l1));
            }
        }
    }

    @Test
    void testJson() throws IOException, ParseFailureException {
        TemplateHashWriter globals = new TemplateHashWriter();
        try (var inStream = FieldInputStream.create(new File("data", "genome_feature.json"));
                var testStream = new LineReader(new File("data", "features3.txt"))) {
            final String TEMPLATE = "Json strings for {{p2_feature_id}} include {{$json:list:segments}} and {{$json:string:acc:accession}}.";
            LineTemplate xlate = new LineTemplate(inStream, TEMPLATE, globals, null);
            Iterator<String> testIter = testStream.iterator();
            for (var line : inStream) {
                String output = xlate.apply(line);
                assertThat(output, equalTo(testIter.next()));
            }
        }

    }

}
