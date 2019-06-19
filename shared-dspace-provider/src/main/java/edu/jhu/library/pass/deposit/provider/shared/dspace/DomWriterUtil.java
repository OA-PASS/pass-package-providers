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

import au.edu.apsr.mtk.base.AmdSec;
import au.edu.apsr.mtk.base.DmdSec;
import au.edu.apsr.mtk.base.File;
import au.edu.apsr.mtk.base.FileGrp;
import au.edu.apsr.mtk.base.FileSec;
import au.edu.apsr.mtk.base.METS;
import au.edu.apsr.mtk.base.METSException;
import au.edu.apsr.mtk.base.SourceMD;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.NS_TO_PREFIX_MAP;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.XSI_NS;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.XMLConstants.XSI_NS_PREFIX;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class DomWriterUtil {

    /**
     * Creates a new {@link Document} from the supplied {@code DocumentBuilderFactory}
     *
     * @param dbf DocumentBuilderFactory
     * @return a new {@link Document}
     */
    public static Document newDocument(DocumentBuilderFactory dbf) {
        Document dcDocument = null;
        try {
            DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
            dcDocument = documentBuilder.newDocument();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return dcDocument;
    }

    /**
     * Creates a new element in the supplied document, using the supplied namespace and qualified name.  This method
     * adds {@code xmlns} attributes for each namespace-&gt;prefix mapping in {@link XMLConstants#NS_TO_PREFIX_MAP}
     *
     * @param doc the Document
     * @param namespace the namespace of the root element
     * @param qualifiedName the qualified name of the root element
     * @return the root element
     */
    public static Element newRootElement(Document doc, String namespace, String qualifiedName) {
        Element root = doc.createElementNS(namespace, qualifiedName);
        root.setAttribute("xmlns:" + XSI_NS_PREFIX, XSI_NS);
        NS_TO_PREFIX_MAP.keySet().stream().collect(Collectors.toMap(NS_TO_PREFIX_MAP::get, (key) -> key)).entrySet()
                .stream().filter((entry) -> {
            // filter out the namespace prefix supplied by the qualifiedName parameter, as the writer will add that
            // prefix in automatically
            if (qualifiedName.contains(":")) {
                String prefix = qualifiedName.substring(0, qualifiedName.indexOf(":"));
                if (prefix.equals(entry.getKey())) {
                    return false;
                }
            }
            return true;
        }).forEach((entry) -> root.setAttribute("xmlns:" + entry.getKey(), entry.getValue()));
        return root;
    }

    /**
     * Creates a new {@code File} in the {@code FileGrp} identified by the supplied {@code use}.  If a {@code FileGrp}
     * with {@code use} doesn't exist, it is created.
     * <p>
     * The newly created {@code File} is supplied an identifier and attached to the {@code FileGrp}.
     * </p>
     *
     * @param mets the METS instance
     * @param use the {@code USE} identifying the {@code FileGrp} to retrieve or create
     * @return the newly created {@code File}, attached to the {@code FileGrp}
     * @throws METSException if there is an error creating the {@code File}
     */
    public static File createFile(METS mets, String use) throws METSException {
        FileGrp fileGrp = getFileGrpByUse(mets, use);
        File file = fileGrp.newFile();
        fileGrp.addFile(file);
        file.setID(mintId());
        return file;
    }

    /**
     * Obtains the {@code <fileGrp>} element with a {@code USE} equal to the supplied {@code use} value.  If the element
     * does not exist, it is created and assigned an identifier.
     *
     * @param mets the METS instance
     * @param use the content use for the {@code FileGrp}
     * @return the {@code FileGrp} with a {@code USE} equal to {@code use}
     * @throws METSException if there is an error retrieving or creating the file group
     */
    public static FileGrp getFileGrpByUse(METS mets, String use) throws METSException {
        List<FileGrp> fileGroups = getFileSec(mets).getFileGrpByUse(use);
        if (fileGroups == null || fileGroups.isEmpty()) {
            return createFileGrp(mets, use);
        }

        return fileGroups.get(0);
    }

    /**
     * Creates the {@code <fileGrp>} element with a {@code USE} equal to the supplied {@code use} value, and assigns it
     * an identifier.
     *
     * @param mets the METS instance
     * @param use the content use for the {@code FileGrp}
     * @return the {@code FileGrp} with a {@code USE} equal to {@code use}
     * @throws METSException if there is an error retrieving or creating the file group
     */
    public static FileGrp createFileGrp(METS mets, String use) throws METSException {
        FileSec fileSec = getFileSec(mets);
        FileGrp fileGrp = fileSec.newFileGrp();
        fileSec.addFileGrp(fileGrp);
        fileGrp.setID(mintId());
        fileGrp.setUse(use);
        return fileGrp;
    }

    /**
     * Obtains the only {@code <fileSec>} element from the METS document.  If the element does not exist, it is created
     * and assigned an identifier.
     *
     * @param mets the METS instance
     * @return the {@code FileSec} for the current METS document
     * @throws METSException if there is an error retrieving or creating the fileSec
     */
    public static FileSec getFileSec(METS mets) throws METSException {
        FileSec fileSec = mets.getFileSec();
        if (fileSec == null) {
            return createFileSec(mets);
        }

        return fileSec;
    }

    /**
     * Creates a new {@code <fileSec>} element from the METS document and assigns it an identifier.
     *
     * @param mets  the METS instance
     * @return the newly created {@code FileSec} for the current METS document
     * @throws METSException if there is an error retrieving or creating the fileSec
     */
    public static FileSec createFileSec(METS mets) throws METSException {
        FileSec fs = mets.newFileSec();
        mets.setFileSec(fs);
        fs.setID(mintId());
        return fs;
    }

    public static SourceMD getSourceMd(METS mets, String id) throws METSException {
        if (id == null) {
            return createSourceMd(mets);
        }

        Optional<AmdSec> amdSec = mets
                .getAmdSecs()
                .stream()
                .filter(candidateAmdSec -> candidateAmdSec.getSourceMD(id) != null)
                .findAny();

        if (amdSec.isPresent()) {
            return amdSec.get().getSourceMD(id);
        }

        throw new RuntimeException("SourceMD with id '" + id + "' not found.");
    }

    public static SourceMD createSourceMd(METS mets) throws METSException {
        AmdSec amdSec = getAmdSec(mets);
        SourceMD sourceMD = amdSec.newSourceMD();
        sourceMD.setID(mintId());
        amdSec.addSourceMD(sourceMD);
        return sourceMD;
    }

    public static AmdSec getAmdSec(METS mets) throws METSException {
        if (mets.getAmdSecs() == null || mets.getAmdSecs().isEmpty()) {
            return createAmdSec(mets);
        }

        return mets.getAmdSecs().get(0);
    }

    public static AmdSec createAmdSec(METS mets) throws METSException {
        AmdSec as = mets.newAmdSec();
        mets.addAmdSec(as);
        as.setID(mintId());
        return as;
    }

    /**
     * Obtains the specified {@code <dmdSec>}, or creates a new {@code <dmdSec>} if {@code id} is {@code null}.
     *
     * @param mets the METS instance
     * @param id the identifier of the {@code <dmdSec>} to retrieve, or {@code null} to create a new {@code <dmdSec>}
     * @return the {@code <dmdSec>}
     * @throws RuntimeException if the {@code <dmdSec>} specified by {@code id} does not exist
     * @throws METSException if there is an error retrieving or creating the dmdSec
     */
    public static DmdSec getDmdSec(METS mets, String id) throws METSException {
        if (id == null) {
            return createDmdSec(mets);
        }

        DmdSec dmdSec = null;
        if ((dmdSec = mets.getDmdSec(id)) == null) {
            throw new RuntimeException("DmdSec with id '" + id + "' not found.");
        }

        return dmdSec;
    }

    /**
     * Creates a new {@code <dmdSec>} element, gives it an identifier, and returns.
     *
     * @param mets the METS instance
     * @return a new {@code <dmdSec>} with an auto-generated id
     */
    public static DmdSec createDmdSec(METS mets) throws METSException {
        DmdSec ds = mets.newDmdSec();
        ds.setID(mintId());
        return ds;
    }

    /**
     * Mints a unique, opaque, string identifier, suitable for identifying and linking between elements in a METS
     * document.
     *
     * @return an identifier
     */
    public static String mintId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Returns the qualified form of {@code elementName} after mapping the {@code namespace} to a prefix.
     * if {@code elementName} is already in a qualified form (e.g. contains a {@code :}), it is return unmodified.
     *
     * @param namespace the namespace for {@code elementName}
     * @param elementName the element name, which may already be qualified
     * @return the qualified name for {@code elementName}
     * @throws IllegalStateException if a {@code namespace} for which there is no prefix mapping is encountered
     */
    public static String asQname(String namespace, String elementName) {
        if (elementName.contains(":")) {
            return elementName;
        }
        if (!NS_TO_PREFIX_MAP.containsKey(namespace)) {
            throw new IllegalStateException("Missing prefix mapping for namespace '" + namespace + "'");
        }
        return String.format("%s:%s", NS_TO_PREFIX_MAP.get(namespace), elementName);
    }

}
