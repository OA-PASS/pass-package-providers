package edu.jhu.library.pass.deposit.provider.dash;

import org.apache.commons.io.IOUtils;
import org.dataconservancy.pass.client.PassClient;
import org.dataconservancy.pass.deposit.model.DepositMetadata;
import org.dataconservancy.pass.deposit.model.DepositSubmission;
import org.dataconservancy.pass.deposit.model.JournalPublicationType;
import org.dataconservancy.pass.model.Funder;
import org.dataconservancy.pass.model.Grant;
import org.dataconservancy.pass.model.PassEntity;
import org.dataconservancy.pass.model.Submission;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterators;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DCT_PROV;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DC_ABSTRACT;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DC_CONTRIBUTOR;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DC_DATE;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DC_DATE_ISSUED;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DC_DESCRIPTION;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DC_IDENTIFIER;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DC_IDENTIFIER_DOI;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DC_IDENTIFIER_ISSN;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DC_PUBLISHER;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DC_SOURCE;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DC_SOURCE_JOURNAL;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DC_SOURCE_VOLUME;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DC_TITLE;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DIM_ELEMENT;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DIM_FIELD;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DIM_MDSCHEMA;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DIM_NS;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DIM_QUALIFIER;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterator.SIZED;
import static java.util.stream.StreamSupport.stream;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class DashMetadataDomWriterTest {

    private static final Logger LOG = LoggerFactory.getLogger(DashMetadataDomWriterTest.class);

    private PassClient passClient;

    private DocumentBuilderFactory dbf;

    private DepositSubmission submission;

    private Submission passSubmission;

    private Funder passFunder;

    private Grant passGrant;

    private DepositMetadata.Journal journalMd;

    private DepositMetadata.Article articleMd;

    private DepositMetadata.Manuscript msMd;

    private List<DepositMetadata.Person> persons;

    private DepositMetadata.Person primaryAuthor;

    private DepositMetadata.Person secondaryAuthor;

    private DepositMetadata.Person submitter;

    private DashMetadataDomWriter underTest;

    private Element dimDoc;

    @Before
    public void setUp() throws Exception {
        dbf = DocumentBuilderFactory.newInstance();
        passClient = mock(PassClient.class);
        underTest = new DashMetadataDomWriter(dbf, passClient);

        passSubmission = new Submission();
        passSubmission.setId(randomUri());

        DepositMetadata depositMetadata = new DepositMetadata();

        submitter = new DepositMetadata.Person();
        primaryAuthor = new DepositMetadata.Person();
        secondaryAuthor = new DepositMetadata.Person();
        DepositMetadata.Person pi = new DepositMetadata.Person();
        DepositMetadata.Person coPi = new DepositMetadata.Person();
        persons = Arrays.asList(submitter, primaryAuthor, secondaryAuthor, pi, coPi);

        msMd = new DepositMetadata.Manuscript();
        journalMd = new DepositMetadata.Journal();
        articleMd = new DepositMetadata.Article();

        depositMetadata.setJournalMetadata(journalMd);
        depositMetadata.setArticleMetadata(articleMd);
        depositMetadata.setManuscriptMetadata(msMd);
        depositMetadata.setPersons(persons);

        submission = new DepositSubmission();
        submission.setMetadata(depositMetadata);
        submission.setId(passSubmission.getId().toString());

        submitter.setFirstName("Sub");
        submitter.setLastName("Mitter");
        submitter.setMiddleName("Middle");
        submitter.setFullName("Sub Middle Mitter");
        submitter.setEmail("submitter@instiution.edu");
        submitter.setType(DepositMetadata.PERSON_TYPE.submitter);

        primaryAuthor.setFirstName("Primary");
        primaryAuthor.setLastName("Middle");
        primaryAuthor.setMiddleName("Author");
        primaryAuthor.setFullName("Primary Middle Author");
        primaryAuthor.setEmail("primaryAuthor@instiution.edu");
        primaryAuthor.setType(DepositMetadata.PERSON_TYPE.author);

        secondaryAuthor.setFirstName("Secondary");
        secondaryAuthor.setLastName("Middle");
        secondaryAuthor.setMiddleName("Author");
        secondaryAuthor.setFullName("Secondary Middle Author");
        secondaryAuthor.setEmail("secondaryAuthor@instiution.edu");
        secondaryAuthor.setType(DepositMetadata.PERSON_TYPE.author);

        pi.setFirstName("Principle");
        pi.setLastName("Middle");
        pi.setMiddleName("Investigator");
        pi.setFullName("Principle Middle Investigator");
        pi.setEmail("pi@instiution.edu");
        pi.setType(DepositMetadata.PERSON_TYPE.pi);

        coPi.setFirstName("Co-Principle");
        coPi.setLastName("Middle");
        coPi.setMiddleName("Investigator");
        coPi.setFullName("Co-Principle Middle Investigator");
        coPi.setEmail("coPi@instiution.edu");
        coPi.setType(DepositMetadata.PERSON_TYPE.copi);

        msMd.setManuscriptUrl(new URL("http://pass.local/fcrepo/rest/submissions/a/b/c/abc.pdf"));
        msMd.setMsAbstract("Manuscript abstract");
        msMd.setNihmsId("NIHMS-MS-ID");
        msMd.setPublisherPdf(false);
        msMd.setShowPublisherPdf(false);
        msMd.setTitle("Manuscript Title");

        journalMd.setJournalId("NLM-TA_Abbrev");
        journalMd.setJournalTitle("Journal Title");
        journalMd.setPublisherName("Journal Publisher");
        journalMd.setPublicationDate("2009-01-01");
        journalMd.setIssnPubTypes(new HashMap<String, DepositMetadata.IssnPubType>() {
            {
                put("e-issn", new DepositMetadata.IssnPubType("e-issn", JournalPublicationType.EPUB));
                put("p-issn", new DepositMetadata.IssnPubType("p-issn", JournalPublicationType.PPUB));
            }
        });

        articleMd.setDoi(URI.create("http://dx.doi.org/10.1002/cncr.24463"));
        articleMd.setIssue("113");
        articleMd.setVolume("5");
        articleMd.setTitle("Article Title");

        passFunder = new Funder();
        passFunder.setId(randomUri());
        passFunder.setName("Funder One");

        passGrant = new Grant();
        passGrant.setId(randomUri());
        passGrant.setAwardNumber("Grant1 1234");
        passGrant.setProjectName("Project Name for Grant 1");
        passGrant.setPrimaryFunder(passFunder.getId());

        passSubmission.setGrants(Collections.singletonList(passGrant.getId()));

        when(passClient.readResource(any(), any())).thenAnswer(inv -> {
            Map<URI, PassEntity> passEntities = new HashMap<URI, PassEntity>() {
                {
                    put(passGrant.getId(), passGrant);
                    put(passSubmission.getId(), passSubmission);
                    put(passFunder.getId(), passFunder);
                }
            };
            return passEntities.get(inv.getArgument(0));
        });

        // Generate the DSpace Intermediate Metadata document by the DashMetadataDomWriter
        dimDoc = underTest.createDublinCoreMetadataDim(submission);

        if (LOG.isDebugEnabled()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            writeDim(dimDoc, out);
            LOG.debug("Generated DIM Doc\n{}", IOUtils.toString(out.toByteArray(), "UTF-8"));
        }
    }

    /**
     * Should be a Dublin Core "contributor.author" for each Person with a PERSON_TYPE == PERSON_TYPE.author.
     * <p>
     * The other persons should not be listed at all.
     */
    @Test
    public void verifyContributors() {
        Set<Element> contributors = elementsForDimField(dimDoc, DC_CONTRIBUTOR);

        assertEquals(2, contributors.size());

        contributors.stream()
                .filter(e -> e.getTextContent().equals(primaryAuthor.fullName))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Missing primary author"));

        contributors.stream()
                .filter(e -> e.getTextContent().equals(secondaryAuthor.fullName))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Missing secondary author"));
    }

    /**
     * Journal nlm title abbreviation, volume, issn(s), publisher name, publication date
     */
    @Test
    public void verifyJournalMeta() {
        validate(dimDoc, DC_SOURCE, DC_SOURCE_JOURNAL, (e) ->
                assertEquals(journalMd.getJournalId(), e.getTextContent()));

        validate(dimDoc, DC_SOURCE, DC_SOURCE_VOLUME, (e) ->
                assertEquals(articleMd.getVolume(), e.getTextContent()));

        validate(dimDoc, DC_PUBLISHER, null, (e) ->
                assertEquals(journalMd.getPublisherName(), e.getTextContent()));

        validate(dimDoc, DC_DATE, DC_DATE_ISSUED, (e) ->
                assertEquals(journalMd.getPublicationDate(), e.getTextContent()));

        Set<Element> issns = elementsForDimField(dimDoc, DC_IDENTIFIER, DC_IDENTIFIER_ISSN);

        assertEquals(2, issns.size());

        issns.stream()
                .filter(e -> e.getTextContent().equals("e-issn"))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Missing expected issn 'e-issn'"));

        issns.stream()
                .filter(e -> e.getTextContent().equals("p-issn"))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Missing expected issn 'p-issn'"));
    }

    /**
     * Ms abstract, title
     */
    @Test
    public void verifyManuscriptMeta() {
        validate(dimDoc, DC_DESCRIPTION, DC_ABSTRACT, (e) ->
                assertEquals(msMd.getMsAbstract(), e.getTextContent()));

        validate(dimDoc, DC_TITLE, null, (e) ->
                assertEquals(msMd.getTitle(), e.getTextContent()));
    }

    /**
     * Article DOI, volume
     */
    @Test
    public void verifyArticleMeta() {
        validate(dimDoc, DC_SOURCE, DC_SOURCE_VOLUME, (e) ->
                assertEquals(articleMd.getVolume(), e.getTextContent()));

        validate(dimDoc, DC_IDENTIFIER, DC_IDENTIFIER_DOI, (e) ->
                assertEquals(articleMd.getDoi().toString(), e.getTextContent()));
    }

    /**
     * Expect funder info: award number (from the PASS Grant), funder name (from the PASS Funder), and funder identifier (from the PASS Funder)
     */
    @Test
    public void verifyFundersAndGrants() {
        validate(dimDoc, DashXMLConstants.FUNDER, DC_IDENTIFIER, (e) -> {
            assertEquals(DashXMLConstants.DIM_MDSCHEMA_DASH, e.getAttribute(DIM_MDSCHEMA));
            assertEquals(passFunder.getId().toString(), e.getTextContent());
        });

        validate(dimDoc, DashXMLConstants.FUNDER, DashXMLConstants.NAME, (e) -> {
            assertEquals(DashXMLConstants.DIM_MDSCHEMA_DASH, e.getAttribute(DIM_MDSCHEMA));
            assertEquals(passFunder.getName(), e.getTextContent());
        });

        validate(dimDoc, DashXMLConstants.FUNDER, DashXMLConstants.AWARD, (e) -> {
            assertEquals(DashXMLConstants.DIM_MDSCHEMA_DASH, e.getAttribute(DIM_MDSCHEMA));
            assertEquals(passGrant.getAwardNumber(), e.getTextContent());
        });
    }

    /**
     * Three prov elements: metadata generated date stamp, submitter info, submission uri
     */
    @Test
    public void verifyProv() {
        Set<Element> provElements = elementsForDimField(dimDoc, DC_DESCRIPTION, DCT_PROV);

        assertEquals(3, provElements.size());

        provElements.stream()
                .filter(provE -> provE.getTextContent().contains(passSubmission.getId().toString()))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Missing prov element with the PASS Submission URI"));

        provElements.stream()
                .filter(provE -> provE.getTextContent().contains(submitter.getFullName())
                        && provE.getTextContent().contains(submitter.getEmail()))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Missing prov element with the PASS Submitter name and email"));

        provElements.stream()
                .filter(provE -> provE.getTextContent().contains("Metadata generated by PASS on"))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Missing prov element with the metadata generation date"));
    }

    private static void validate(Element dimDoc, String fieldName, String qualifier, Consumer<Element> validator) {
        Element e;
        if (qualifier == null) {
            e = elementForDimField(dimDoc, fieldName);
        } else {
            e = elementForDimField(dimDoc, fieldName, qualifier);
        }

        validator.accept(e);
    }

    private static Set<Element> elementsForDimField(Element dimDoc, String fieldName) {
        return asStream(dimDoc.getElementsByTagNameNS(DIM_NS, DIM_FIELD))
                .map(n -> (Element) n)
                .filter(e -> fieldName.equals(e.getAttribute(DIM_ELEMENT)))
                .collect(Collectors.toSet());
    }

    private static Set<Element> elementsForDimField(Element dimDoc, String fieldName, String qualifier) {
        return asStream(dimDoc.getElementsByTagNameNS(DIM_NS, DIM_FIELD))
                .map(n -> (Element) n)
                .filter(e -> fieldName.equals(e.getAttribute(DIM_ELEMENT)) && qualifier.equals(e.getAttribute(DIM_QUALIFIER)))
                .collect(Collectors.toSet());
    }

    private static Element elementForDimField(Element dimDoc, String fieldName) {
        return asStream(dimDoc.getElementsByTagNameNS(DIM_NS, DIM_FIELD))
                .map(n -> (Element) n)
                .filter(e -> fieldName.equals(e.getAttribute(DIM_ELEMENT)))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Missing expected DIM field " + fieldName));
    }

    private static Element elementForDimField(Element dimDoc, String fieldName, String qualifier) {
        return asStream(dimDoc.getElementsByTagNameNS(DIM_NS, DIM_FIELD))
                .map(n -> (Element) n)
                .filter(e -> fieldName.equals(e.getAttribute(DIM_ELEMENT)) && qualifier.equals(e.getAttribute(DIM_QUALIFIER)))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Missing expected DIM field " + fieldName + " with qualifier " + qualifier));
    }

    private static Stream<Node> asStream(NodeList nodeList) {
        int characteristics = SIZED | ORDERED;
        Stream<Node> nodeStream = stream(new Spliterators.AbstractSpliterator<Node>(nodeList.getLength(), characteristics) {
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

    private static void writeDim(Element dimDoc, OutputStream out) {
        DOMImplementation impl = dimDoc.getOwnerDocument().getImplementation();
        DOMImplementationLS implLS = (DOMImplementationLS) impl.getFeature("LS", "3.0");

        LSOutput lso = implLS.createLSOutput();

        lso.setByteStream(out);
        LSSerializer writer = implLS.createLSSerializer();

        writer.write(dimDoc.getOwnerDocument(), lso);
    }

    private static URI randomUri() {
        return URI.create("urn:uuid:" + UUID.randomUUID());
    }

}