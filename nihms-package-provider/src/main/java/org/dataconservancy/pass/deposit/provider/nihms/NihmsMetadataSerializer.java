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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.io.xml.XmlFriendlyNameCoder;
import org.dataconservancy.pass.deposit.assembler.shared.SizedStream;
import org.dataconservancy.pass.deposit.model.DepositMetadata;
import org.dataconservancy.pass.deposit.model.JournalPublicationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * XML serialization of our NihmsMetadata to conform with the bulk submission dtd
 *
 * @author Jim Martino (jrm@jhu.edu)
 */
public class NihmsMetadataSerializer implements StreamingSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(NihmsMetadataSerializer.class);

    private DepositMetadata metadata;

    public NihmsMetadataSerializer(DepositMetadata metadata){
        this.metadata = metadata;
    }

    public SizedStream serialize() {
        XStream xstream = new XStream(new DomDriver("UTF-8", new XmlFriendlyNameCoder("_-", "_")));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        xstream.registerConverter(new MetadataConverter());
        xstream.alias("nihms-submit", DepositMetadata.class);
        xstream.toXML(metadata, os);

        return NihmsAssemblerUtil.asSizedStream(os);
    }

    private class MetadataConverter implements Converter {
        @SuppressWarnings("rawtypes")
        public boolean canConvert(Class clazz) {
            return DepositMetadata.class == clazz;
        }

        public void marshal(Object value, HierarchicalStreamWriter writer,
                            MarshallingContext context) {
            DepositMetadata metadata = (DepositMetadata) value;

            //process manuscript element (except, strangely, for title, which we do after journal)
            DepositMetadata.Manuscript manuscript = metadata.getManuscriptMetadata();
            DepositMetadata.Article article = metadata.getArticleMetadata();
            if (manuscript != null) {
                writer.startNode("manuscript");
                if(manuscript.getNihmsId() != null) {
                    writer.addAttribute("id", manuscript.getNihmsId());
                }

                //primitive types
                writer.addAttribute("publisher_pdf", booleanConvert(manuscript.isPublisherPdf()));
                writer.addAttribute("show_publisher_pdf", booleanConvert(manuscript.isShowPublisherPdf()));
                if (metadata.getArticleMetadata() != null && metadata.getArticleMetadata().getEmbargoLiftDate() != null) {
                    // TODO: resolve the calculation of the embargo offset
                }

                if (manuscript.getManuscriptUrl() != null) {
                    writer.addAttribute("href", manuscript.getManuscriptUrl().toString());
                }
                if (article != null && article.getDoi() != null) {
                    // DOI may not include UTI's scheme or host, only path
                    String path = article.getDoi().getPath();
                    if (path.startsWith("/")) {
                        path = path.substring(1);
                    }
                    writer.addAttribute("doi", path);
                }

                writer.endNode(); //end manuscript
            }

            //process journal
            DepositMetadata.Journal journal = metadata.getJournalMetadata();
            if (journal != null) {
                writer.startNode("journal-meta");
                if (journal.getJournalId() != null) {
                    writer.startNode("journal-id");
                    if (journal.getJournalType() != null) {
                        writer.addAttribute("journal-id-type", journal.getJournalType());
                    }
                    writer.setValue(journal.getJournalId());
                    writer.endNode();
                }

                journal.getIssnPubTypes().values().forEach(issnPubType -> {
                    // if the IssnPubType is incomplete (either the pubType or issn is null or empty), we should
                    // omit it from the metadata, per NIH's requirements
                    // See https://github.com/OA-PASS/metadata-schemas/pull/28 and
                    // https://github.com/OA-PASS/jhu-package-providers/issues/16
                    if (issnPubType.pubType == null || issnPubType.issn == null || issnPubType.issn.trim().isEmpty()) {
                        LOG.debug("Discarding incomplete ISSN: {}", issnPubType);
                        return;
                    }
                    writer.startNode("issn");
                    // The JournalPublicationType OPUB should be translated to JournalPublicationType EPUB to stay
                    // valid with respect to the NIHMS metadata schema
                    if (issnPubType.pubType == JournalPublicationType.OPUB) {
                        writer.addAttribute("pub-type", JournalPublicationType.EPUB.name().toLowerCase());
                    } else {
                        writer.addAttribute("pub-type", issnPubType.pubType.name().toLowerCase());
                    }
                    writer.setValue(issnPubType.issn);
                    writer.endNode();
                });

                if (journal.getJournalTitle() != null) {
                    writer.startNode("journal-title");
                    writer.setValue(journal.getJournalTitle());
                    writer.endNode();
                }
                writer.endNode(); //end journal-meta
            }

            //now process full manuscript title
            if (manuscript != null && manuscript.getTitle() != null) {
                writer.startNode("title");
                writer.setValue(manuscript.getTitle());
                writer.endNode();
            }

            //process contacts
            List<DepositMetadata.Person> persons = metadata.getPersons();
            if (persons != null && persons.size() > 0) {
                writer.startNode("contacts");
                for (DepositMetadata.Person person : persons){
                    // There should be exactly one corresponding PI per deposit.
                    if (person.getType() == DepositMetadata.PERSON_TYPE.submitter) {
                        writer.startNode("person");
                        if (person.getFirstName() != null) {
                            writer.addAttribute("fname", person.getFirstName());
                        } else {
                            if (person.getFullName() != null) {
                                writer.addAttribute("fname", person.getFullName().split("\\s")[0]);
                            }
                        }
                        if (person.getMiddleName() != null) {
                            writer.addAttribute("mname", person.getMiddleName());
                        }
                        if (person.getLastName() != null) {
                            writer.addAttribute("lname", person.getLastName());
                        } else {
                            if (person.getFullName() != null) {
                                String[] split = person.getFullName().split("\\s");
                                if (split.length > 2) {
                                    // middle name is present
                                    writer.addAttribute("lname", String.join(" ", Arrays.copyOfRange(split, 2, split.length)));
                                } else {
                                    writer.addAttribute("lname", split[1]);
                                }
                            }
                        }
                        if (person.getEmail() != null) {
                            writer.addAttribute("email", person.getEmail());
                        }
                        //primitive types
                        writer.addAttribute("corrpi", booleanConvert(true));
                        writer.addAttribute("pi", booleanConvert(true));
                        // Searching for another Person who matches this one and who is an AUTHOR
                        // would be difficult due to name variations, so do not output that attribute.
                        writer.endNode(); // end person
                        break; // Make sure we only write one person to the metadata
                    }
                }
                writer.endNode(); //end contacts
            }
        }

        public Object  unmarshal(HierarchicalStreamReader reader,
                                 UnmarshallingContext context) {
            return null;
        }
    }

    /**
     * Method to convert boolean into yes or no
     * @param  b the boolean to convert
     * @return yes if true, no if false
     */
    String booleanConvert(boolean b){
        return(b?"yes":"no");
    }

}
