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
package edu.jhu.library.pass.deposit.provider.dash;

import au.edu.apsr.mtk.base.DmdSec;
import au.edu.apsr.mtk.base.METSException;
import au.edu.apsr.mtk.base.MdWrap;
import edu.jhu.library.pass.deposit.provider.shared.dspace.AbstractDspaceMetadataDomWriter;
import org.dataconservancy.pass.client.PassClient;
import org.dataconservancy.pass.deposit.model.DepositMetadata;
import org.dataconservancy.pass.deposit.model.DepositSubmission;
import org.dataconservancy.pass.model.Funder;
import org.dataconservancy.pass.model.Grant;
import org.dataconservancy.pass.model.Submission;
import org.joda.time.DateTime;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static edu.jhu.library.pass.deposit.provider.shared.dspace.DomWriterUtil.asQname;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.DomWriterUtil.createDmdSec;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.DomWriterUtil.mintId;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.DomWriterUtil.newDocument;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.DomWriterUtil.newRootElement;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.MetsMdType.DC;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DCT_PROV;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DC_ABSTRACT;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DC_CONTRIBUTOR;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DC_CONTRIBUTOR_AUTHOR;
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
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DC_TYPE;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DIM;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DIM_ELEMENT;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DIM_FIELD;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DIM_MDSCHEMA;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DIM_MDSCHEMA_DC;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DIM_NS;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DIM_QUALIFIER;
import static java.lang.System.currentTimeMillis;
import static java.util.Optional.ofNullable;
import static java.util.TimeZone.getDefault;
import static org.joda.time.DateTimeZone.forTimeZone;
import static org.joda.time.format.ISODateTimeFormat.dateHourMinute;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class DashMetadataDomWriter extends AbstractDspaceMetadataDomWriter {

    private Document doc;

    private Element rootElement;

    private PassClient passClient;

    public DashMetadataDomWriter(DocumentBuilderFactory dbf, PassClient passClient) {
        super(dbf);
        this.passClient = passClient;
        this.doc = newDocument(dbf);
        this.rootElement = newRootElement(doc, DIM_NS, asQname(DIM_NS, DIM));
        doc.appendChild(rootElement);
    }

    protected Collection<DmdSec> mapDmdSec(DepositSubmission submission) throws METSException {
        List<DmdSec> result = new ArrayList<>();
        Element dcRecord = createDublinCoreMetadataDim(submission);
        DmdSec dcDmdSec = createDmdSec(mets);

        try {
            MdWrap dcMdWrap = dcDmdSec.newMdWrap();
            dcMdWrap.setID(mintId());
            dcMdWrap.setMDType(DC.getType());
            dcMdWrap.setXmlData(dcRecord);
            dcDmdSec.setMdWrap(dcMdWrap);
            dcDmdSec.setGroupID(mintId());
            dcDmdSec.setMdWrap(dcMdWrap);

        } catch (METSException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        mets.addDmdSec(dcDmdSec);
        result.add(dcDmdSec);

        // TODO Embargo metadata

        return result;
    }

    /**
     * Creates metadata according to DASH's requirements in the DSpace Intermediate Metadata (DIM) XML format:
     * <ul>
     * <li>title for the Manuscript</li>
     * <li>publisher publisher's name for the publishing Journal</li>
     * <li>identifier.doi for the Article</li>
     * <li>contributor.author for each author associated with the Manuscript</li>
     * <li>description.abstract for the Manuscript</li>
     * <li>date.issued for the publishing Journal</li>
     * <li>identifier.issn for each ISSN associated with the publishing Journal</li>
     * <li>source.journal the nlm title abbreviation of the publishing Journal</li>
     * <li>source.volume the volume of the publishing Journal</li>
     * <li>dash.affilation.other</li>
     * <li>dash.funder.name</li>
     * <li>dash.funder.award</li>
     * <li>dash.funder.identifier</li>
     * </ul>
     *
     * @param submission the submission
     * @return the root element of a DSpace Intermediate Metadata (DIM) XML document
     */
    Element createDublinCoreMetadataDim(DepositSubmission submission) {
        DepositMetadata metadata = submission.getMetadata();
        DepositMetadata.Manuscript manuscriptMd = metadata.getManuscriptMetadata();
        DepositMetadata.Article articleMd = metadata.getArticleMetadata();
        DepositMetadata.Journal journalMd = metadata.getJournalMetadata();

        // Attach a <dc:title> for the Manuscript title
        ofNullable(manuscriptMd.getTitle()).ifPresent(title -> {
            dimElement(doc, DC_TITLE, null)
                    .apply(e -> e.setTextContent(title));
        });

        dimElement(doc, DC_TYPE, null)
                .apply(e -> e.setTextContent("Journal Article"));

        // Attach a <dc:description:abstract> for the manuscript, if one was provided
        ofNullable(manuscriptMd.getMsAbstract()).ifPresent(msAbstract -> {
            dimElement(doc, DC_DESCRIPTION, DC_ABSTRACT)
                    .apply(e -> e.setTextContent(msAbstract));
        });

        // <dc:date:issued>
        ofNullable(journalMd.getPublicationDate()).ifPresent(pubDate -> {
            dimElement(doc, DC_DATE, DC_DATE_ISSUED)
                    .apply(e -> e.setTextContent(pubDate));
        });

        // <dc:identifier:issn>
        journalMd.getIssnPubTypes()
                .keySet()
                .forEach(issn -> {
                    dimElement(doc, DC_IDENTIFIER, DC_IDENTIFIER_ISSN)
                        .apply(e -> e.setTextContent(issn));
                });

        // <dc:identifier.doi>
        ofNullable(articleMd.getDoi()).ifPresent(doi -> {
            dimElement(doc, DC_IDENTIFIER, DC_IDENTIFIER_DOI)
                .apply(e-> e.setTextContent(doi.toString()));
        });

        // Attach a <dc:publisher> for the journal, if one was provided
        ofNullable(journalMd.getPublisherName()).ifPresent(publisherName -> {
            dimElement(doc, DC_PUBLISHER, null)
                .apply(e -> e.setTextContent(publisherName));
        });

        // nlm title authority is the journalId
        // Attach <dc:source:journal>
        ofNullable(journalMd.getJournalId()).ifPresent(journalId -> {
            dimElement(doc, DC_SOURCE, DC_SOURCE_JOURNAL)
                    .apply(e -> e.setTextContent(journalId));
        });

        // Attach <dc:source:volume>
        ofNullable(articleMd.getVolume()).ifPresent(volume -> {
            dimElement(doc, DC_SOURCE, DC_SOURCE_VOLUME)
                    .apply(e -> e.setTextContent(volume));
        });

        // Attach a <dc:contributor.author> for each author of the manuscript
        metadata.getPersons()
                .stream()
                .filter(p -> DepositMetadata.PERSON_TYPE.author == p.getType())
                .forEach(p -> {
                    dimElement(doc, DC_CONTRIBUTOR, DC_CONTRIBUTOR_AUTHOR)
                            .apply(c -> c.setTextContent(p.getFullName()));
                });

        // DASH elements

        // Add Grant and Funder info
        ofNullable(passClient.readResource(URI.create(submission.getId()), Submission.class)).ifPresent(s -> {
            s.getGrants()
                    .stream()
                    .map(u -> passClient.readResource(u, Grant.class))
                    .forEach(grant -> {
                        dimElement(doc, DashXMLConstants.FUNDER, DashXMLConstants.AWARD)
                                .apply(e -> {
                                    e.setAttribute(DIM_MDSCHEMA, DashXMLConstants.DIM_MDSCHEMA_DASH);
                                    e.setTextContent(grant.getAwardNumber());
                                });

                        ofNullable(grant.getPrimaryFunder())
                                .map(u -> passClient.readResource(u, Funder.class))
                                .ifPresent(funder -> {
                                    dimElement(doc, DashXMLConstants.FUNDER, DashXMLConstants.NAME)
                                            .apply(e -> {
                                                e.setAttribute(DIM_MDSCHEMA, DashXMLConstants.DIM_MDSCHEMA_DASH);
                                                e.setTextContent(funder.getName());
                                            });

                                    dimElement(doc, DashXMLConstants.FUNDER, DashXMLConstants.IDENTIFIER)
                                            .apply(e -> {
                                                e.setAttribute(DIM_MDSCHEMA, DashXMLConstants.DIM_MDSCHEMA_DASH);
                                                e.setTextContent(funder.getId().toString());
                                            });
                                });
                    });

        });

        // TODO DASH affiliation (pending addition to the metadata blob)

        // Provenance statement

        DepositMetadata.Person submitter = submission.getMetadata().getPersons().stream()
                .filter(p -> DepositMetadata.PERSON_TYPE.submitter == p.getType())
                .findAny()
                .orElseThrow(() ->
                        new RuntimeException("Missing submitter for Submission resource " + submission.getId()));

        DateTime submittedDate = submission.getSubmissionDate();

        dimElement(doc, DC_DESCRIPTION, DCT_PROV)
                .apply(e -> {
                    e.setTextContent(String.format("Metadata generated by PASS on %s",
                            dateHourMinute().withZone(forTimeZone(getDefault())).print(currentTimeMillis())));
                });

        dimElement(doc, DC_DESCRIPTION, DCT_PROV)
                .apply(e -> {

                    e.setTextContent(String.format("Submitted by %s (%s) via PASS on %s",
                            submitter.getFullName(),
                            submitter.getEmail(),
                            dateHourMinute().withZone(forTimeZone(getDefault())).print(submittedDate)));
                });

        dimElement(doc, DC_DESCRIPTION, DCT_PROV)
                .apply(e -> {
                    e.setTextContent(String.format("PASS Submission identifier: %s", submission.getId()));
                });

        return rootElement;
    }

    private Function<Consumer<Element>, Element> dimElement(Document doc, String elementName, String elementQualifier) {
        Element dimElement = doc.createElementNS(DIM_NS, asQname(DIM_NS, DIM_FIELD));
        dimElement.setAttribute(DIM_MDSCHEMA, DIM_MDSCHEMA_DC);
        dimElement.setAttribute(DIM_ELEMENT, elementName);
        ofNullable(elementQualifier).ifPresent(q -> {
            dimElement.setAttribute(DIM_QUALIFIER, q);
        });

        ElementTransform transform = new ElementTransform(dimElement);

        return transform.andThen(e -> {
            doc.getDocumentElement().appendChild(e);
            return e;
        });
    }

    private class ElementTransform implements Function<Consumer<Element>, Element> {

        private Element e;

        ElementTransform(Element e) {
            this.e = e;
        }

        @Override
        public Element apply(Consumer<Element> elementConsumer) {
            elementConsumer.accept(e);
            return e;
        }

    }

}
