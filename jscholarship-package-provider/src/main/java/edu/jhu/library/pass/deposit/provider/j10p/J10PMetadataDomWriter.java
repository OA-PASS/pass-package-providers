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
package edu.jhu.library.pass.deposit.provider.j10p;

import au.edu.apsr.mtk.base.Div;
import au.edu.apsr.mtk.base.DmdSec;
import au.edu.apsr.mtk.base.FileSec;
import au.edu.apsr.mtk.base.Fptr;
import au.edu.apsr.mtk.base.METSException;
import au.edu.apsr.mtk.base.MdSec;
import au.edu.apsr.mtk.base.MdWrap;
import au.edu.apsr.mtk.base.StructMap;
import edu.jhu.library.pass.deposit.provider.shared.dspace.AbstractDspaceMetadataDomWriter;
import org.dataconservancy.pass.deposit.model.DepositMetadata;
import org.dataconservancy.pass.deposit.model.DepositSubmission;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static edu.jhu.library.pass.deposit.provider.shared.dspace.DomWriterUtil.asQname;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.DomWriterUtil.createDmdSec;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.DomWriterUtil.mintId;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.DomWriterUtil.newDocument;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.DomWriterUtil.newRootElement;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.MetsMdType.DC;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.MetsMdType.OTHER;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DCTERMS_NS;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DCT_ABSTRACT;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DCT_BIBLIOCITATION;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DCT_HASVERSION;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DC_CONTRIBUTOR;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DC_DESCRIPTION;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DC_NS;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DC_PUBLISHER;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DC_TITLE;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DIM;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DIM_DESCRIPTION;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DIM_ELEMENT;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DIM_EMBARGO;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DIM_EMBARGO_LIFT;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DIM_EMBARGO_TERMS;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DIM_FIELD;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DIM_MDSCHEMA;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DIM_MDSCHEMA_DC;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DIM_MDSCHEMA_LOCAL;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DIM_NS;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DIM_PROVENANCE;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.DIM_QUALIFIER;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class J10PMetadataDomWriter extends AbstractDspaceMetadataDomWriter {

    private int authorIndex;

    public J10PMetadataDomWriter(DocumentBuilderFactory dbf) {
        super(dbf);
    }

    protected Collection<DmdSec> mapDmdSec(DepositSubmission submission) throws METSException {
        List<DmdSec> result = new ArrayList<>();
        Element dcRecord = createDublinCoreMetadataDCMES(submission);
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

        if (submission.getMetadata().getArticleMetadata().getEmbargoLiftDate() != null) {
            Element dimRecord = createDimMetadataForEmbargo(submission);
            DmdSec dimDmdSec = createDmdSec(mets);

            try {
                MdWrap dimMdWrap = dimDmdSec.newMdWrap();
                dimMdWrap.setID(mintId());
                dimMdWrap.setMDType(OTHER.getType());
                dimMdWrap.setOtherMDType("DIM");
                dimMdWrap.setXmlData(dimRecord);
                dimDmdSec.setMdWrap(dimMdWrap);
                dimDmdSec.setGroupID(mintId());
                dimDmdSec.setMdWrap(dimMdWrap);
            } catch (METSException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            mets.addDmdSec(dimDmdSec);
            result.add(dimDmdSec);
        }

        return result;
    }

    private Element createDimMetadataForEmbargo(DepositSubmission submission) {
        Document dimDocument = newDocument(dbf);
        Element dimRoot = newRootElement(dimDocument, DIM_NS, asQname(DIM_NS, DIM));
        dimDocument.appendChild(dimRoot);

        /*
        local.embargo.terms: the date upon which the embargo will expire in the format of ‘yyyy-MM-dd’
        local.embargo.lift: the date upon which the embargo will expire in the format of ‘yyyy-MM-dd’
        dc.description: Submission published under an embargo, which will last until yyyy-MM-dd
        dc.description.provenance: Submission published under an embargo, which will last until yyyy-MM-dd
         */


        ZonedDateTime embargoLiftDate = submission.getMetadata().getArticleMetadata().getEmbargoLiftDate();
        if (embargoLiftDate == null) {
            throw new NullPointerException("Embargo lift date should not be null.");
        }
        String formattedDate = embargoLiftDate.format(DateTimeFormatter.ISO_LOCAL_DATE);


        // <dim:field mdschema="local" element="embargo" qualifier="terms">
        Element localEmbargoTerms = dimDocument.createElementNS(DIM_NS, asQname(DIM_NS, DIM_FIELD));
        localEmbargoTerms.setAttribute(DIM_MDSCHEMA, DIM_MDSCHEMA_LOCAL);
        localEmbargoTerms.setAttribute(DIM_ELEMENT, DIM_EMBARGO);
        localEmbargoTerms.setAttribute(DIM_QUALIFIER, DIM_EMBARGO_TERMS);
        localEmbargoTerms.setTextContent(formattedDate);

        // <dim:field mdschema="local" element="embargo" qualifier="lift">
        Element localEmbargoLift = dimDocument.createElementNS(DIM_NS, asQname(DIM_NS, DIM_FIELD));
        localEmbargoLift.setAttribute(DIM_MDSCHEMA, DIM_MDSCHEMA_LOCAL);
        localEmbargoLift.setAttribute(DIM_ELEMENT, DIM_EMBARGO);
        localEmbargoLift.setAttribute(DIM_QUALIFIER, DIM_EMBARGO_LIFT);
        localEmbargoLift.setTextContent(formattedDate);

        // <dim:field mdschema="dc" element="description" qualifier="provenance">
        Element dcDescProv = dimDocument.createElementNS(DIM_NS, asQname(DIM_NS, DIM_FIELD));
        dcDescProv.setAttribute(DIM_MDSCHEMA, DIM_MDSCHEMA_DC);
        dcDescProv.setAttribute(DIM_ELEMENT, DIM_DESCRIPTION);
        dcDescProv.setAttribute(DIM_QUALIFIER, DIM_PROVENANCE);
        dcDescProv.setTextContent(String.format("Submission published under an embargo, which will last until %s",
                formattedDate));

        dimRoot.appendChild(localEmbargoLift);
        dimRoot.appendChild(localEmbargoTerms);
        dimRoot.appendChild(dcDescProv);

        return dimRoot;
    }

    /**
     * Creates the Dublin Core metadata from the submission using the Qualified Dublin Core schema.  Includes:
     * <ul>
     *     <li>dc:contributor for each Person associated with the Manuscript</li>
     *     <li>dc:title for the Manuscript</li>
     *     <li>dcterms:hasVersion for the DOI of the Article</li>
     *     <li>dcterms:abstract for the Manuscript</li>
     *     <li>dc:description with the embargo lift date, if an embargo is on the Article</li>
     * </ul>
     * The returned Element will have a name {@code qualifieddc}, which has "special" meaning to DSpace.
     * <p>
     * Package-private for unit testing
     * </p>
     * @param submission
     * @return
     */
    Element createDublinCoreMetadataQualified(DepositSubmission submission) {
        Document dcDocument = newDocument(dbf);

        // Root <record> element
        Element record = newRootElement(dcDocument, DCTERMS_NS, "qualifieddc");

        dcDocument.appendChild(record);

        // Attach a <dc:contributor> for each Person associated with the submission to the Manuscript metadata
        DepositMetadata nimsMd = submission.getMetadata();
        DepositMetadata.Manuscript manuscriptMd = nimsMd.getManuscriptMetadata();
        DepositMetadata.Article articleMd = nimsMd.getArticleMetadata();

        nimsMd.getPersons().forEach(p -> {
            // Only include authors, PIs and CoPIs as contributors
            if (p.getType() != DepositMetadata.PERSON_TYPE.submitter) {
                Element contributor = dcDocument.createElementNS(DC_NS, asQname(DC_NS, DC_CONTRIBUTOR));
                contributor.setTextContent(p.getName());
                record.appendChild(contributor);
            }
        });

        // Attach a <dc:title> for the Manuscript title
        if (manuscriptMd.getTitle() != null) {
            Element titleElement = dcDocument.createElementNS(DC_NS, asQname(DC_NS, DC_TITLE));
            titleElement.setTextContent(manuscriptMd.getTitle());
            record.appendChild(titleElement);
        } else {
            throw new RuntimeException("No title found in the NIHMS manuscript metadata!");
        }

        // Attach a <dcterms:hasVersion> pointing to the published Article DOI
        if (articleMd.getDoi() != null) {
            Comment c = dcDocument.createComment("This DOI points to the published version of the manuscript, available after any embargo period has been satisfied.");
            record.appendChild(c);
            Element hasVersion = dcDocument.createElementNS(DCTERMS_NS, asQname(DCTERMS_NS, DCT_HASVERSION));
            hasVersion.setTextContent(articleMd.getDoi().toString());
            record.appendChild(hasVersion);
        }

        // Attach a <dcterms:abstract> for the manuscript, if one was provided
        if (manuscriptMd.getMsAbstract() != null) {
            Element msAbstractElement = dcDocument.createElementNS(DCTERMS_NS, asQname(DCTERMS_NS, DCT_ABSTRACT));
            msAbstractElement.setTextContent(manuscriptMd.getMsAbstract());
            record.appendChild(msAbstractElement);
        }

        // TODO: Journal metadata
        // ...

        // TODO: Article metadata
        // <dc:identifier> DOI for the published article
        // <dc:available> date available if there is an embargo on the published article

        // Add a description of the embargo, if one is present
        // "Submission published under an embargo, which will last until yyyy-MM-dd"
        if (articleMd.getEmbargoLiftDate() != null) {
            Element dcEmbargoDesc = dcDocument.createElementNS(DC_NS, asQname(DC_NS, DC_DESCRIPTION));
            dcEmbargoDesc.setTextContent(String.format("Submission published under an embargo, which will last until %s",
                    articleMd.getEmbargoLiftDate().format(DateTimeFormatter.ISO_LOCAL_DATE)));
            record.appendChild(dcEmbargoDesc);
        }

        return record;
    }

    /**
     * Creates the Dublin Core metadata from the submission using the original DCMES schema.
     * These contents explicitly match only those elements requested by JScholarship.  Includes:
     * <ul>
     *     <li>dc:title for the Manuscript</li>
     *     <li>dc:publisher for the publisher name</li>
     *     <li>dc:identifier.citation for the Manuscript</li>
     *     <li>dc:contributor for each Person associated with the Manuscript</li>
     *     <li>dc:description:abstract for the Manuscript</li>
     *     <li>dc:description with the embargo lift date, if an embargo is on the Article</li>
     * </ul>
     * The returned Element will have a name {@code qualifieddc}, which has "special" meaning to DSpace.
     * <p>
     * Package-private for unit testing
     * </p>
     * @param submission
     * @return
     */
    Element createDublinCoreMetadataDCMES(DepositSubmission submission) {
        Document dcDocument = newDocument(dbf);

        // Root <record> element
        // TODO - What is the correct qualified name for DCMES data?
        Element record = newRootElement(dcDocument, DC_NS, "qualifieddc");

        dcDocument.appendChild(record);

        // Attach a <dc:contributor> for each Person associated with the submission to the Manuscript metadata
        DepositMetadata nimsMd = submission.getMetadata();
        DepositMetadata.Manuscript manuscriptMd = nimsMd.getManuscriptMetadata();
        DepositMetadata.Article articleMd = nimsMd.getArticleMetadata();
        DepositMetadata.Journal journalMd = nimsMd.getJournalMetadata();

        // Attach a <dc:title> for the Manuscript title
        if (manuscriptMd.getTitle() != null) {
            Element titleElement = dcDocument.createElementNS(DC_NS, asQname(DC_NS, DC_TITLE));
            titleElement.setTextContent(manuscriptMd.getTitle());
            record.appendChild(titleElement);
        } else {
            throw new RuntimeException("No title found in the manuscript metadata!");
        }

        // Attach a <dc:description:abstract> for the manuscript, if one was provided
        if (manuscriptMd.getMsAbstract() != null) {
            Element msAbstractElement = dcDocument.createElementNS(DCTERMS_NS, asQname(DCTERMS_NS, DCT_ABSTRACT));
            msAbstractElement.setTextContent(manuscriptMd.getMsAbstract());
            record.appendChild(msAbstractElement);
        }

        // Attach a <dc:publisher> for the journal, if one was provided
        if (journalMd != null && journalMd.getPublisherName() != null) {
            Element publisher = dcDocument.createElementNS(DC_NS, asQname(DC_NS, DC_PUBLISHER));
            publisher.setTextContent(journalMd.getPublisherName());
            record.appendChild(publisher);
        }

        // Begin building citation string
        StringBuilder citationBldr = new StringBuilder();

        // Attach a <dc:contributor> for each author of the manuscript and add authorIndex to citation
        authorIndex = 0;
        nimsMd.getPersons().forEach(p -> {
            // Only include authorIndex, PIs and CoPIs as contributors
            if (p.getType() != DepositMetadata.PERSON_TYPE.submitter) {
                Element contributor = dcDocument.createElementNS(DC_NS, asQname(DC_NS, DC_CONTRIBUTOR));
                contributor.setTextContent(p.getName());
                record.appendChild(contributor);
            }

            if (p.getType() == DepositMetadata.PERSON_TYPE.author) {
                // Citation: For author 0, add name.  For authorIndex 1 and 2, add comma then name.
                // For author 3, add comma and "et al".  For later authorIndex, do nothing.
                if (authorIndex == 0)
                    citationBldr.append(p.getReversedName());
                else if (authorIndex <= 2)
                    citationBldr.append(", " + p.getReversedName());
                else if (authorIndex == 3)
                    citationBldr.append(", et al");
                authorIndex++;
            }
        });
        if (authorIndex == 0)
            throw new RuntimeException("No authors found in the manuscript metadata!");
        // Add period at end of author list in citation
        citationBldr.append(".");

        // Attach a <dc:identifier:citation> if not empty
        // publication date - after a single space, in parens, followed by "."
        if (journalMd != null && journalMd.getPublicationDate() != null && ! journalMd.getPublicationDate().isEmpty())
            citationBldr.append(" (" + journalMd.getPublicationDate() + ").");
        // article title - after single space, in double quotes with "." inside
        if (articleMd != null && articleMd.getTitle() != null && ! articleMd.getTitle().isEmpty())
            citationBldr.append(" \"" + articleMd.getTitle() + ".\"");
        // journal title - after single space, followed by "."
        if (journalMd != null && journalMd.getJournalTitle() != null && ! journalMd.getJournalTitle().isEmpty())
            citationBldr.append(" " + journalMd.getJournalTitle() + ".");
        // volume - after single space
        if (articleMd != null && articleMd.getVolume() != null && ! articleMd.getVolume().isEmpty())
            citationBldr.append(" " + articleMd.getVolume());
        // issue - after single space, inside parens, followed by "."
        if (articleMd != null && articleMd.getIssue() != null && ! articleMd.getIssue().isEmpty())
            citationBldr.append(" (" + articleMd.getIssue() + ").");
        // DOI - after single space, followed by "."
        if (articleMd != null && articleMd.getDoi() != null)
            citationBldr.append(" " + articleMd.getDoi().toString() + ".");

        if (! citationBldr.toString().isEmpty()) {
            Element citation = dcDocument.createElementNS(DCTERMS_NS, asQname(DCTERMS_NS, DCT_BIBLIOCITATION));
            citation.setTextContent(citationBldr.toString());
            record.appendChild(citation);
        }

        return record;
    }

}
