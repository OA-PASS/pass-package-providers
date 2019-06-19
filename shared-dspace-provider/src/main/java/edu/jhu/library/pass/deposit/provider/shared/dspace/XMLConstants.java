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
package edu.jhu.library.pass.deposit.provider.shared.dspace;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class XMLConstants {

    /**
     * Dublin Core Namespace
     */
    public static final String DC_NS = "http://purl.org/dc/elements/1.1/";

    public static final String DC_NS_PREFIX = "dc";

    /**
     * DC Terms Namespace
     */
    public static final String DCTERMS_NS = "http://purl.org/dc/terms/";

    public static final String DCTERMS_NS_PREFIX = "dcterms";

    /**
     * DSpace Internal Metadata Namespace (dim)
     */
    public static final String DIM_NS = "http://www.dspace.org/xmlns/dspace/dim";

    public static final String DIM_NS_PREFIX = "dim";

    /**
     * XLink Namespace
     */
    public static final String XLINK_NS = "http://www.w3.org/1999/xlink";

    public static final String XLINK_PREFIX = "xlink";

    /**
     * METS Namespace
     */
    public static final String METS_NS = "http://www.loc.gov/METS/";

    public static final Map<String, String> NS_TO_PREFIX_MAP = new HashMap<String, String>() {
        {
            put(DCTERMS_NS, DCTERMS_NS_PREFIX);
            put(DC_NS, DC_NS_PREFIX);
            put(DIM_NS, DIM_NS_PREFIX);
            put(XLINK_NS, XLINK_PREFIX);
        }
    };

    /**
     * XML Schema Instance Namespace
     */
    public static final String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";

    public static final String XSI_NS_PREFIX = "xsi";

    public static final String XSI_TYPE = XSI_NS_PREFIX + ":type";

    /*
     * See http://dublincore.org/documents/dces/
     */

    public static final String DC_TITLE = "title";

    public static final String DC_CONTRIBUTOR = "contributor";

    public static final String DC_CONTRIBUTOR_AUTHOR = "author";

    public static final String DC_COVERAGE = "coverage";

    public static final String DC_CREATOR = "creator";

    public static final String DC_DATE = "date";

    public static final String DC_DATE_ISSUED = "issued";

    public static final String DC_FORMAT = "format";

    public static final String DC_LANGUAGE = "language";

    public static final String DC_RELATION = "relation";

    public static final String DC_SOURCE = "source";

    public static final String DC_SOURCE_JOURNAL = "journal";

    public static final String DC_SOURCE_VOLUME = "volume";

    public static final String DC_TYPE = "type";

    public static final String DC_PUBLISHER = "publisher";

    public static final String DC_DESCRIPTION = "description";

    public static final String DC_ABSTRACT = "abstract";

    public static final String DC_IDENTIFIER = "identifier";

    public static final String DC_IDENTIFIER_ISSN = "issn";

    public static final String DC_IDENTIFIER_DOI = "doi";

    public static final String DC_SUBJECT = "subject";

    public static final String DC_RIGHTS = "rights";

    public static final String DC_CITATION = "citation";


    /*
     * See http://dublincore.org/documents/dcmi-terms/
     */

    public static final String DCT_ABSTRACT = "abstract";

    public static final String DCT_ACCESSRIGHTS = "accessRights";

    public static final String DCT_ACCRUALMETHOD = "accrualMethod";

    public static final String DCT_ACCRUALPERIODICITY = "accrualPeriodicity";

    public static final String DCT_ACCRUALPOLICY = "accrualPolicy";

    public static final String DCT_ALT = "alternative";

    public static final String DCT_AUDIENCE = "audience";

    public static final String DCT_AVAILABLE = "available";

    public static final String DCT_BIBLIOCITATION = "bibliographicCitation";

    public static final String DCT_CONFORMSTO = "conformsTo";

    public static final String DCT_CONTRIBUTOR = "contributor";

    public static final String DCT_COVERAGE = "coverage";

    public static final String DCT_CREATED = "created";

    public static final String DCT_CREATOR = "creator";

    public static final String DCT_DATE = "date";

    public static final String DCT_DATEACCEPTED = "dateAccepted";

    public static final String DCT_DATECOPYRIGHTED = "dateCopyrighted";

    public static final String DCT_DATESUBMITTED = "dateSubmitted";

    public static final String DCT_DESCRIPTION = "description";

    public static final String DCT_EDUCATIONLEVEL = "educationLevel";

    public static final String DCT_EXTENT = "extent";

    public static final String DCT_FORMAT = "format";

    public static final String DCT_HASFORMAT = "hasFormat";

    public static final String DCT_HASPART = "hasPart";

    public static final String DCT_HASVERSION = "hasVersion";

    public static final String DCT_IDENTIFIER = "identifier";

    public static final String DCT_INSTMETHOD = "instructionalMethod";

    public static final String DCT_ISFORMATOF = "isFormatOf";

    public static final String DCT_ISPARTOF = "isPartOf";

    public static final String DCT_ISREFBY = "isReferencedBy";

    public static final String DCT_ISREPLBY = "isReplacedBy";

    public static final String DCT_ISREQBY = "isRequiredBy";

    public static final String DCT_ISSUED = "issued";

    public static final String DCT_ISVERSIONOF = "isVersionOf";

    public static final String DCT_LANGUAGE = "language";

    public static final String DCT_LICENSE = "license";

    public static final String DCT_MEDIATOR = "mediator";

    public static final String DCT_MEDIUM = "medium";

    public static final String DCT_MODIFIED = "modified";

    public static final String DCT_PROV = "provenance";

    public static final String DCT_PUBLISHER = "publisher";

    public static final String DCT_REFERENCES = "references";

    public static final String DCT_RELATION = "relation";

    public static final String DCT_REPLACES = "replaces";

    public static final String DCT_REQUIRES = "requires";

    public static final String DCT_RIGHTS = "rights";

    public static final String DCT_RIGHTSHOLDER = "rightsHolder";

    public static final String DCT_SOURCE = "source";

    public static final String DCT_SPATIAL = "spatial";

    public static final String DCT_SUBJECT = "subject";

    public static final String DCT_TABLEOFCONTENTS = "tableOfContents";

    public static final String DCT_TEMPORAL = "temporal";

    public static final String DCT_TITLE = "title";

    public static final String DCT_TYPE = "type";

    public static final String DCT_VALID = "valid";

    public static final String DCT_PERIOD = "Period";

    public static final String DCT_URI = "URI";

    public static final String DCT_W3CDTF = "W3CDTF";

    public static final String DCT_IMT = "IMT";

    /*
     * DSpace Internal Metadata
     */

    public static final String DIM_FIELD = "field";

    public static final String DIM_MDSCHEMA = "mdschema";

    public static final String DIM_ELEMENT = "element";

    public static final String DIM_QUALIFIER = "qualifier";

    public static final String DIM_MDSCHEMA_DC = "dc";

    public static final String DIM_MDSCHEMA_LOCAL = "local";

    public static final String DIM = "dim";

    public static final String DIM_EMBARGO = "embargo";

    public static final String DIM_EMBARGO_LIFT = "lift";

    public static final String DIM_EMBARGO_TERMS = "terms";

    public static final String DIM_PROVENANCE = "provenance";

    public static final String DIM_DESCRIPTION = "description";

    /*
     * METS
     */

    public static final String METS_PROFILE = "PROFILE";

    public static final String METS_LABEL = "LABEL";

    public static final String METS_ID = "ID";

    public static final String METS_USE = "USE";

    public static final String METS_CONTENT = "CONTENT";

    public static final String METS_CHECKSUM = "CHECKSUM";

    public static final String METS_CHECKSUM_TYPE = "CHECKSUMTYPE";

    public static final String METS_CHECKSUM_TYPE_MD5 = "MD5";

    public static final String METS_MIMETYPE = "MIMETYPE";

    public static final String METS_SIZE = "SIZE";

    public static final String METS_LOCTYPE = "LOCTYPE";

    public static final String METS_LOCTYPE_URL = "URL";

    public static final String METS_FILESEC = "fileSec";

    public static final String METS_FILEGRP = "fileGrp";

    public static final String METS_FILE = "file";

    public static final String METS_FLOCAT = "FLocat";

    public static final String METS_DMDSEC = "dmdSec";

    public static final String METS_MDWRAP = "mdWrap";

    public static final String METS_XMLDATA = "xmlData";

    public static final String METS_GROUPID = "GROUPID";

    public static final String METS_MDTYPE = "MDTYPE";

    public static final String METS_MDTYPE_DC = "DC";

    public static final String METS_MDTYPE_OTHER = "OTHER";

    public static final String METS_OTHERMDTYPE = "OTHERMDTYPE";

    public static final String METS_OTHERMDTYPE_TYPE = "DIM";

    public static final String METS_STRUCTMAP = "structMap";

    public static final String METS_DIV = "div";

    public static final String METS_DMDID = "DMDID";

    public static final String METS_FPTR = "fptr";

    public static final String METS_FILEID = "FILEID";

    /*
     * XLink
     */

    public static final String XLINK_HREF = "href";

}
