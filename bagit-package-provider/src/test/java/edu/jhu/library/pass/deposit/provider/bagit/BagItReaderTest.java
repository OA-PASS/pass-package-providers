/*
 * Copyright 2019 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.jhu.library.pass.deposit.provider.bagit;

import static edu.jhu.library.pass.deposit.provider.bagit.BagItWriter.CR;
import static edu.jhu.library.pass.deposit.provider.bagit.BagItWriter.CR_ENCODED;
import static edu.jhu.library.pass.deposit.provider.bagit.BagItWriter.LF;
import static edu.jhu.library.pass.deposit.provider.bagit.BagItWriter.LF_ENCODED;
import static edu.jhu.library.pass.deposit.provider.bagit.BagItWriter.PERCENT;
import static edu.jhu.library.pass.deposit.provider.bagit.BagItWriter.PERCENT_ENCODED;
import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class BagItReaderTest {

    private static final String PERCENT_DOUBLE_ENCODED_SPACE = "%2520";

    private static final String FILENAME_WITH_ENCODED_SPACE = "file%20with%20space";

    private static final String FILENAME_WITH_LF = "file" + LF + "with" + LF + "lf";

    private static final String FILENAME_WITH_ENCODED_LF = "file" + LF_ENCODED + "with" + LF_ENCODED + "lf";

    private static final String FILENAME_WITH_CR = "file" + CR + "with" + CR + "lf";

    private static final String FILENAME_WITH_ENCODED_CR = "file" + CR_ENCODED + "with" + CR_ENCODED + "lf";

    private static final String FILENAME_WITH_DOUBLE_ENCODED_SPACE =
        "file" + PERCENT_DOUBLE_ENCODED_SPACE + "with" + PERCENT_DOUBLE_ENCODED_SPACE + "space";

    /**
     * Sample {@code bag-info.txt}
     */
    private static final String BAG_INFO = "" +
                                           "Source-Organization: Johns Hopkins\n" +
                                           "Organization-Address: 3400 N. Charles St, Baltimore, MD 21218\n" +
                                           "Contact-Name: Joe Contact\n" +
                                           "Contact-Phone: joecontact@jhu.edu\n" +
                                           "Contact-Email: 555-555-5555\n" +
                                           "Contact-Name: Jane Contact\n" +
                                           "Contact-Phone: janecontact@jhu.edu\n" +
                                           "Contact-Email: 123-456-7890\n" +
                                           "External-Description: Submitted as " +
                                           "uri:uuid:cf7a14da-8db4-4323-a15f-cff635ba6168 to PASS on 20190508T151013Z" +
                                           " by Joe User (joeuser@user.com), published as 10.1039/c7fo01251a\n" +
                                           "Bagging-Date: 2019-05-07\n" +
                                           "External-Identifier: 10.1039/c7fo01251a\n" +
                                           "Bag-Size: 15 GiB\n" +
                                           "Payload-Oxum: 300.16106127360\n" +
                                           "Internal-Sender-Identifier: uri:uuid:cf7a14da-8db4-4323-a15f-cff635ba6168" +
                                           "\n" +
                                           "Internal-Sender-Description: Submitted as " +
                                           "uri:uuid:cf7a14da-8db4-4323-a15f-cff635ba6168 to PASS on 20190508T151013Z" +
                                           " by Joe User (joeuser@user.com), published as 10.1039/c7fo01251a\n";

    /**
     * Simplified {@code bag-info.txt} encoded as UTF-16
     * 0x0e9 is e with an acute encoded as utf_16
     */
    private static final String BAG_INFO_UTF16 = "Contact-Name: \u00e9\n";

    /**
     * Sample Bag Declaration {@code bagit.txt}
     */
    private static final String BAG_DECL = "" +
                                           "BagIt-Version: 1.0\n" +
                                           "Tag-File-Character-Encoding: UTF-8\n";

    /**
     * Sample manifest
     */
    private static final String MANIFEST = "" +
                                           "hexchecksum_1 data/path/to/file.txt\n" +
                                           "hexchecksum_2\tdata/path/to/file2.txt\n" +
                                           "hexchecksum_3  data/path/to/file3.txt\n" +
                                           "hexchecksum_4\t\tdata/path/to/file4.txt\n" +
                                           "hexchecksum_5\t data/path/to/file5.txt\n" +
                                           "hexchecksum_6 \tdata/path/to/file6.txt\n" +
                                           "hexchecksum_7 \t data/path/to/file7.txt\n" +
                                           "hexchecksum_8\t \tdata/path/to/file8.txt\n" +
                                           "hexchecksum_1 data/path/to/file9.txt\n" +
                                           "hexchecksum_1 data/" + FILENAME_WITH_DOUBLE_ENCODED_SPACE + "\n";

    private BagItReader reader = new BagItReader(UTF_8);

    @Test
    public void readEncoding() {
        // A reader that decodes UTF_16
        final Charset readerEncoding = UTF_16;
        reader = new BagItReader(readerEncoding);

        // Verify we're supplying UTF_16 0x00E9 (the new line takes two bytes)
        byte[] input = BAG_INFO_UTF16.getBytes(UTF_16);
        assertArrayEquals(new byte[] {(byte) 0x00, (byte) 0xE9},
                          new byte[] {input[input.length - 4], input[input.length - 3]});

        // Read the InputStream, encoded with UTF_16.
        Map<String, List<String>> labelAndValue = reader.readLabelsAndValues(
            toInputStream(BAG_INFO_UTF16, readerEncoding));

        // Should be able to parse out the contact name
        String value = labelAndValue.get(BagMetadata.CONTACT_NAME).get(0);

        // Value should be properly encoded as UTF-8
        assertArrayEquals(new byte[] {(byte) 0xc3, (byte) 0xa9}, value.getBytes(UTF_8));

        // Or UTF-16 BOM 0xFEFF and 0x00E9 e acute
        assertArrayEquals(new byte[] {(byte) 0xFE, (byte) 0xFF, (byte) 0x00, (byte) 0xE9}, value.getBytes(UTF_16));
    }

    @Test
    public void readBagDecl() {
        Map<String, String> decl = reader.readBagDecl(toInputStream(BAG_DECL, UTF_8));
        assertEquals(2, decl.size());
        assertEquals("1.0", decl.get(BagMetadata.BAGIT_VERSION));
        assertEquals("UTF-8", decl.get(BagMetadata.TAG_FILE_ENCODING));
    }

    @Test
    public void readManifest() {
        Map<String, String> manifest = reader.readManifest(toInputStream(MANIFEST, UTF_8));
        assertEquals(10, manifest.size());
        assertEquals("hexchecksum_1", manifest.get("data/path/to/file.txt"));
        assertEquals("hexchecksum_2", manifest.get("data/path/to/file2.txt"));
        assertEquals("hexchecksum_3", manifest.get("data/path/to/file3.txt"));
        assertEquals("hexchecksum_4", manifest.get("data/path/to/file4.txt"));
        assertEquals("hexchecksum_5", manifest.get("data/path/to/file5.txt"));
        assertEquals("hexchecksum_6", manifest.get("data/path/to/file6.txt"));
        assertEquals("hexchecksum_7", manifest.get("data/path/to/file7.txt"));
        assertEquals("hexchecksum_8", manifest.get("data/path/to/file8.txt"));
        assertEquals("hexchecksum_1", manifest.get("data/path/to/file9.txt"));
        assertEquals("hexchecksum_1", manifest.get("data/" + FILENAME_WITH_ENCODED_SPACE));

    }

    @Test
    public void labels() {
        List<String> labels = reader.readLabels(toInputStream(BAG_INFO, UTF_8));
        assertEquals(15, labels.size());
        assertTrue(labels.contains("Bag-Size"));
        assertEquals(BagMetadata.INTERNAL_SENDER_DESCRIPTION, labels.get(labels.size() - 1));
        assertEquals(BagMetadata.SOURCE_ORGANIZATION, labels.get(0));
    }

    @Test
    public void labelsAndValues() {
        Map<String, List<String>> labelsAndValues = reader.readLabelsAndValues(toInputStream(BAG_INFO, UTF_8));

        assertEquals(12, labelsAndValues.size());
        assertTrue(labelsAndValues.containsKey(BagMetadata.CONTACT_NAME));

        List<String> contactNames = labelsAndValues.get(BagMetadata.CONTACT_NAME);
        assertEquals(2, contactNames.size());
        assertEquals("Joe Contact", contactNames.get(0));
        assertEquals("Jane Contact", contactNames.get(1));

        Iterator<Map.Entry<String, List<String>>> itr = labelsAndValues.entrySet().iterator();
        Map.Entry<String, List<String>> entry = itr.next();
        assertEquals(BagMetadata.SOURCE_ORGANIZATION, entry.getKey());
        assertEquals("Johns Hopkins", entry.getValue().get(0));
        assertEquals(1, entry.getValue().size());

        while (itr.hasNext()) {
            entry = itr.next();
        }

        assertEquals(BagMetadata.INTERNAL_SENDER_DESCRIPTION, entry.getKey());
        assertEquals("Submitted as uri:uuid:cf7a14da-8db4-4323-a15f-cff635ba6168 to PASS on " +
                     "20190508T151013Z by Joe User (joeuser@user.com), published as 10.1039/c7fo01251a",
                     entry.getValue().get(0));
        assertEquals(1, entry.getValue().size());
    }

    @Test
    public void decodePathWithDoubleEncodedSpace() {
        assertEquals(FILENAME_WITH_ENCODED_SPACE, BagItReader.decodePath(FILENAME_WITH_DOUBLE_ENCODED_SPACE));
    }

    @Test
    public void decodePathWithEncodedLf() {
        assertEquals(FILENAME_WITH_LF, BagItReader.decodePath(FILENAME_WITH_ENCODED_LF));
    }

    @Test
    public void decodePathWithEncodedCr() {
        assertEquals(FILENAME_WITH_CR, BagItReader.decodePath(FILENAME_WITH_ENCODED_CR));
    }

    @Test
    public void decodePathWithEndingCr() {
        assertEquals("foo" + CR, BagItReader.decodePath("foo" + CR_ENCODED));
    }

    @Test
    public void decodePathWithStartingCr() {
        assertEquals(CR + "foo", BagItReader.decodePath(CR_ENCODED + "foo"));
    }

    @Test
    public void decodeKitchenSink() {
        assertEquals(PERCENT + "foo bar" + PERCENT + LF + "baz" + PERCENT,
                     BagItReader.decodePath(
                         PERCENT_ENCODED + "foo bar" + PERCENT_ENCODED + LF_ENCODED + "baz" + PERCENT_ENCODED));
    }
}