/*
 * Copyright 2018 Johns Hopkins University
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

package org.dataconservancy.pass.deposit.provider.nihms;

import org.apache.commons.io.IOUtils;
import org.dataconservancy.pass.deposit.assembler.shared.SizedStream;
import org.dataconservancy.pass.deposit.model.DepositMetadata;
import org.dataconservancy.pass.deposit.model.JournalPublicationType;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlunit.validation.Languages;
import org.xmlunit.validation.ValidationResult;
import org.xmlunit.validation.Validator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Spliterators;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterator.SIZED;
import static java.util.stream.StreamSupport.stream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * This is a test for the metadata serializer. for now we just validate against the bulk submission dtd
 *
 * @author Jim Martino (jrm@jhu.edu)
 */
public class NihmsMetadataSerializerTest {

    private static NihmsMetadataSerializer underTest;
    private static DepositMetadata metadata= new DepositMetadata();

    @BeforeClass
    public static void setup() throws Exception {
        //set up metadata snd its fields;
        DepositMetadata.Journal journal = new DepositMetadata.Journal();
        DepositMetadata.Manuscript manuscript = new DepositMetadata.Manuscript();
        DepositMetadata.Article article = new DepositMetadata.Article();
        List<DepositMetadata.Person> personList = new ArrayList<>();

        //populate journal metadata
        journal.setJournalId("FJ001");
        journal.setJournalTitle("Dairy Cow Monthly");
        journal.setIssnPubTypes(new HashMap<String, DepositMetadata.IssnPubType>() {
            {
                put("8765-4321", new DepositMetadata.IssnPubType("8765-4321", JournalPublicationType.EPUB));
                // OPUB publication type should be expressed as an EPUB in the resulting metadata per the NIHMS
                // bulk submission metadata requirements
                put("1234-5678", new DepositMetadata.IssnPubType("1234-5678", JournalPublicationType.OPUB));
                put("0000-0000", new DepositMetadata.IssnPubType("0000-0000", JournalPublicationType.PPUB));
            }
        });

        //populate manuscript metadata
        manuscript.setManuscriptUrl(new URL("http://farm.com/Cows"));
        manuscript.setNihmsId("00001");
        manuscript.setPublisherPdf(true);
        manuscript.setShowPublisherPdf(false);
        manuscript.setTitle("Manuscript Title");

        // populate article metadata
        article.setDoi(URI.create("10.1234/smh0000001"));

        //populate persons
        DepositMetadata.Person person1 = new DepositMetadata.Person();
        person1.setType(DepositMetadata.PERSON_TYPE.author);
        person1.setEmail("person@farm.com");
        person1.setFirstName("Bessie");
        person1.setLastName("Cow");
        person1.setMiddleName("The");
        personList.add(person1);

        // Enter the first person twice, as both an author and a PI
        DepositMetadata.Person person1a = new DepositMetadata.Person(person1);
        person1.setType(DepositMetadata.PERSON_TYPE.pi);
        personList.add(person1);

        DepositMetadata.Person person2 = new DepositMetadata.Person();
        person2.setType(DepositMetadata.PERSON_TYPE.submitter);
        person2.setEmail("person@farm.com");
        person2.setFirstName("Elsie");
        person2.setLastName("Cow");
        person2.setMiddleName("The");
        personList.add(person2);

        DepositMetadata.Person person3 = new DepositMetadata.Person();
        person3.setType(DepositMetadata.PERSON_TYPE.author);
        person3.setEmail("person@farm.com");
        person3.setFirstName("Mark");
        person3.setLastName("Cow");
        person3.setMiddleName("The");
        personList.add(person3);

        DepositMetadata.Person person4 = new DepositMetadata.Person();
        person4.setType(DepositMetadata.PERSON_TYPE.copi);
        person4.setEmail("person@farm.com");
        person4.setFirstName("John");
        person4.setLastName("Cow");
        person4.setMiddleName("The");
        personList.add(person4);

        metadata.setJournalMetadata(journal);
        metadata.setManuscriptMetadata(manuscript);
        metadata.setPersons(personList);
        metadata.setArticleMetadata(article);

        underTest = new NihmsMetadataSerializer(metadata);
    }


    @Test
    public void testSerializedMetadataValidity() throws Exception {
        SizedStream sizedStream = underTest.serialize();
        byte[] buffer = new byte[sizedStream.getInputStream().available()];
        sizedStream.getInputStream().read(buffer);
        sizedStream.getInputStream().close();

        File targetFile = new File("MetadataSerializerTest.xml");

        OutputStream os = new FileOutputStream(targetFile);

        os.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n".getBytes());
        os.write("<!DOCTYPE nihms-submit SYSTEM \"bulksubmission.dtd\">\n".getBytes());
        os.write(buffer);
        os.close();

        Validator v = Validator.forLanguage(Languages.XML_DTD_NS_URI);
        StreamSource dtd = new StreamSource(getClass().getResourceAsStream("bulksubmission.dtd"));
        dtd.setSystemId(getClass().getResource("bulksubmission.dtd").toURI().toString());
        v.setSchemaSource(dtd);
        StreamSource s = new StreamSource("MetadataSerializerTest.xml");
        ValidationResult r = v.validateInstance(s);
        assertTrue(r.isValid());
    }


    @Test
    // DOI must be in "raw" format (no leading http://domain/ or https://domain/)
    public void testSerializedMetadataDoi() throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        String path = "10.1234/smh0000001";
        SizedStream sizedStream;
        InputStream is;
        Node node;
        String doi;

        metadata.getArticleMetadata().setDoi(URI.create(path));
        sizedStream = underTest.serialize();
        is = sizedStream.getInputStream();
        node = builder.parse(is).getDocumentElement().getFirstChild().getNextSibling();
        doi = node.getAttributes().getNamedItem("doi").getTextContent();
        is.close();
        assertTrue("Valid DOI was modified during export.", doi.contentEquals(path));

        metadata.getArticleMetadata().setDoi(URI.create("http://dx.doi.org/" + path));
        sizedStream = underTest.serialize();
        is = sizedStream.getInputStream();
        node = builder.parse(is).getDocumentElement().getFirstChild().getNextSibling();
        doi = node.getAttributes().getNamedItem("doi").getTextContent();
        is.close();
        assertTrue("http:// prefix and/or domain not stripped from DOI during export.", doi.contentEquals(path));
    }

    /**
     * A complete IssnPubType (having a non null publication type and issn value) should be serialized to the metadata.
     */
    @Test
    public void completeIssnPubtype() throws IOException, ParserConfigurationException, SAXException {
        DepositMetadata metadata = new DepositMetadata();
        DepositMetadata.Journal journalMd = new DepositMetadata.Journal();
        String expectedIssn = "foo";
        String expectedPubType = JournalPublicationType.EPUB.name().toLowerCase(); // remember OPUB is mapped to EPUB when serializing

        DepositMetadata.IssnPubType issn = new DepositMetadata.IssnPubType(expectedIssn, JournalPublicationType.OPUB);
        journalMd.setIssnPubTypes(new HashMap<String, DepositMetadata.IssnPubType>() {
            {
                put(issn.issn, issn);
            }
        });

        metadata.setJournalMetadata(journalMd);

        underTest = new NihmsMetadataSerializer(metadata);

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Node doc = builder.parse(underTest.serialize().getInputStream());
        NodeList issnNodes = ((Document) doc).getElementsByTagName("issn");

        assertEquals(1, issnNodes.getLength());

        Node actualIssn = asStream(issnNodes)
                .filter(node -> expectedIssn.equals(node.getFirstChild().getNodeValue()))
                .findAny()
                .orElseThrow(() ->
                        new RuntimeException("Missing expected <issn> element for " + expectedIssn + " and " + expectedPubType));

        assertEquals(expectedPubType, actualIssn.getAttributes().getNamedItem("pub-type").getNodeValue());
    }

    /**
     * IssnPubType instances that are incomplete (one of the fields is null or empty) should not be serialized
     * https://github.com/OA-PASS/jhu-package-providers/issues/16
     */
    @Test
    public void incompleteIssnPubtypeNullIssn() throws IOException {
        DepositMetadata metadata = new DepositMetadata();
        DepositMetadata.Journal journalMd = new DepositMetadata.Journal();
        DepositMetadata.IssnPubType nullIssn = new DepositMetadata.IssnPubType(null, JournalPublicationType.OPUB);
        journalMd.setIssnPubTypes(new HashMap<String, DepositMetadata.IssnPubType>() {
            {
                put(nullIssn.issn, nullIssn);
            }
        });

        metadata.setJournalMetadata(journalMd);

        underTest = new NihmsMetadataSerializer(metadata);

        assertFalse(IOUtils.toString(underTest.serialize().getInputStream(), UTF_8).contains("issn"));
    }

    /**
     * IssnPubType instances that are incomplete (one of the fields is null or empty) should not be serialized
     * https://github.com/OA-PASS/jhu-package-providers/issues/16
     */
    @Test
    public void incompleteIssnPubtypeNullPubType() throws IOException {
        DepositMetadata metadata = new DepositMetadata();
        DepositMetadata.Journal journalMd = new DepositMetadata.Journal();
        DepositMetadata.IssnPubType nullPubType = new DepositMetadata.IssnPubType("foo", null);
        journalMd.setIssnPubTypes(new HashMap<String, DepositMetadata.IssnPubType>() {
            {
                put(nullPubType.issn, nullPubType);
            }
        });

        metadata.setJournalMetadata(journalMd);

        underTest = new NihmsMetadataSerializer(metadata);

        assertFalse(IOUtils.toString(underTest.serialize().getInputStream(), UTF_8).contains("issn"));
    }

    /**
     * If the User is missing a first name or last name, it should be parsed from the full name instead.  The middle
     * name is excluded if it is present in the full name.  Parsing is not exact, but will prevent NPEs and provide
     * a non-null value in the metadata, which is necessary for the package to pass validaton.
     */
    @Test
    public void missingUserFirstNameAndLastName() throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        // Override the first and last name of the Person with null, and then set the full name to expected values
        DepositMetadata.Person submitter =
                metadata.getPersons()
                        .stream()
                        .filter(p -> p.getType() == DepositMetadata.PERSON_TYPE.submitter)
                        .findAny()
                        .orElseThrow(() -> new RuntimeException("Missing Submitter for submission."));

        String expectedFirstName = "MooFirstName";
        String expectedLastName = "CowLastName";
        submitter.setFullName(String.format("%s %s %s", expectedFirstName, "SpotsMiddleName", expectedLastName));
        submitter.setFirstName(null);
        submitter.setLastName(null);

        Element doc = builder.parse(underTest.serialize().getInputStream()).getDocumentElement();

        Node foundPerson = asStream(doc.getElementsByTagName("person"))
                .filter(node -> node.getAttributes().getNamedItem("email").getTextContent().equals(submitter.email))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Missing submitter person in serialized metadata"));

        assertEquals(expectedFirstName, foundPerson.getAttributes().getNamedItem("fname").getTextContent());
        assertEquals(expectedLastName, foundPerson.getAttributes().getNamedItem("lname").getTextContent());
    }

    private static Stream<Node> asStream(NodeList nodeList) {
        int characteristics = SIZED | ORDERED;
        Stream<Node> nodeStream = stream(new AbstractSpliterator<Node>(nodeList.getLength(), characteristics) {
            int index = 0;
            @Override
            public boolean tryAdvance(Consumer<? super Node> action) {
                if (nodeList.getLength() == index) {
                    return false;
                }

                action.accept(nodeList.item(index++));

                return true;
            }
        }, false);

        return nodeStream;
    }

}