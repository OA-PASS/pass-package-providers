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

import au.edu.apsr.mtk.base.Constants;
import au.edu.apsr.mtk.base.Div;
import au.edu.apsr.mtk.base.DmdSec;
import au.edu.apsr.mtk.base.FLocat;
import au.edu.apsr.mtk.base.File;
import au.edu.apsr.mtk.base.FileSec;
import au.edu.apsr.mtk.base.Fptr;
import au.edu.apsr.mtk.base.METS;
import au.edu.apsr.mtk.base.METSException;
import au.edu.apsr.mtk.base.METSWrapper;
import au.edu.apsr.mtk.base.MdSec;
import au.edu.apsr.mtk.base.StructMap;
import org.dataconservancy.pass.deposit.assembler.PackageStream;
import org.dataconservancy.pass.deposit.model.DepositSubmission;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;

import java.io.OutputStream;
import java.util.Collection;
import java.util.stream.Collectors;

import static edu.jhu.library.pass.deposit.provider.shared.dspace.DomWriterUtil.createFile;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.DomWriterUtil.getFileGrpByUse;
import static edu.jhu.library.pass.deposit.provider.shared.dspace.DomWriterUtil.mintId;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public abstract class AbstractDspaceMetadataDomWriter implements DspaceMetadataDomWriter {

    public static final String METS_ID = "DSPACE-METS-SWORD";

    public static final String METS_OBJ_ID = "DSPACE-METS-SWORD-OBJ";

    public static final String METS_DSPACE_LABEL = "DSpace SWORD Item";

    public static final String METS_DSPACE_PROFILE = "DSpace METS SIP Profile 1.0";

    public static final String CONTENT_USE = "CONTENT";

    public static final String LOCTYPE_URL = "URL";

    protected Document metsDocument;

    protected DocumentBuilderFactory dbf;

    protected METS mets;

    protected AbstractDspaceMetadataDomWriter(DocumentBuilderFactory dbf) {
        try {
            this.dbf = dbf;
            this.metsDocument = dbf.newDocumentBuilder().newDocument();
            Element root = metsDocument.createElementNS(Constants.NS_METS, Constants.ELEMENT_METS);
            metsDocument.appendChild(root);
            this.mets = new METS(metsDocument);
            this.mets.setID(mintId());
            this.mets.setProfile(METS_DSPACE_PROFILE);
            this.mets.setLabel(METS_DSPACE_LABEL);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public StructMap mapStructMap(DepositSubmission submission, Collection<DmdSec> dmdSec, FileSec fileSec) {
        StructMap structMap = null;
        try {
            structMap = this.mets.newStructMap();
        } catch (METSException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        structMap.setID(mintId());
        structMap.setLabel("DSpace CONTENT bundle structure");

        Div itemDiv = null;
        try {
            itemDiv = structMap.newDiv();
        } catch (METSException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        itemDiv.setID(mintId());
        itemDiv.setLabel("DSpace Item Div");
        itemDiv.setDmdID(dmdSec.stream().map(MdSec::getID).collect(Collectors.joining(" ")));

        Div finalItemDiv = itemDiv;
        try {
            fileSec.getFileGrpByUse(CONTENT_USE)
                    .stream()
                    .flatMap(fileGrp -> {
                        try {
                            return fileGrp.getFiles().stream();
                        } catch (METSException e) {
                            throw new RuntimeException(e.getMessage(), e);
                        }
                    })
                    .forEach(f -> {
                        Fptr filePtr = null;
                        try {
                            filePtr = finalItemDiv.newFptr();
                        } catch (METSException e) {
                            throw new RuntimeException(e.getMessage(), e);
                        }
                        filePtr.setID(mintId());
                        filePtr.setFileID(f.getID());
                        finalItemDiv.addFptr(filePtr);
                    });
        } catch (METSException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        this.mets.addStructMap(structMap);
        structMap.addDiv(itemDiv);
        return structMap;
    }

    protected abstract Collection<DmdSec> mapDmdSec(DepositSubmission submission) throws METSException;

    public void write(OutputStream out) {
        METSWrapper wrapper = null;
        try {
            wrapper = new METSWrapper(metsDocument);
        } catch (METSException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        wrapper.write(out);
    }

    /**
     * Add a {@link PackageStream.Resource resource} to the DOM.  This creates a METS {@code File} with a {@code FLocat}
     * for the resource.  The resource's primary checksum, size, mime type, and name are included in the METS DOM.
     * The {@code FLocat} will be a URL type, using the resource name as the location.
     *
     * @param resource the package resource to be represented in the DOM
     */
    public AbstractDspaceMetadataDomWriter addResource(PackageStream.Resource resource) {
        File resourceFile = null;
        try {
            resourceFile = createFile(mets, CONTENT_USE);
        } catch (METSException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        if (resource.checksum() != null) {
            resourceFile.setChecksum(resource.checksum().asHex());
            resourceFile.setChecksumType(resource.checksum().algorithm().name());
        }

        if (resource.sizeBytes() > -1) {
            resourceFile.setSize(resource.sizeBytes());
        }

        if (resource.mimeType() != null && resource.mimeType().trim().length() > 0) {
            resourceFile.setMIMEType(resource.mimeType());
        }

        FLocat locat = null;
        try {
            locat = resourceFile.newFLocat();
            resourceFile.addFLocat(locat);
        } catch (METSException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        locat.setID(mintId());
        locat.setHref(resource.name());
        locat.setLocType(LOCTYPE_URL);

        return this;
    }

    public AbstractDspaceMetadataDomWriter addSubmission(DepositSubmission submission) {
        try {
            if (getFileGrpByUse(mets, CONTENT_USE) == null || getFileGrpByUse(mets, CONTENT_USE).getFiles().isEmpty()) {
                throw new IllegalStateException("No <fileGrp USE=\"" + CONTENT_USE + "\"> element was found, or was" +
                        " empty.  Resources must be added before submissions.  Has addResource(Resource) been called?");
            }
        } catch (METSException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        try {
            mapStructMap(submission, mapDmdSec(submission), mets.getFileSec());
        } catch (METSException e) {
            throw new RuntimeException(e.getMessage(), e);
        }

        return this;
    }

}
