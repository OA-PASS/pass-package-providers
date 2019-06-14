/*
 *
 *  * Copyright 2019 Johns Hopkins University
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package edu.jhu.library.pass.deposit.provider.shared.dspace;

import au.edu.apsr.mtk.base.METS;
import au.edu.apsr.mtk.base.METSException;
import au.edu.apsr.mtk.base.METSWrapper;
import au.edu.apsr.mtk.ch.METSReader;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.DigestObserver;
import org.apache.commons.io.input.ObservableInputStream;
import org.apache.commons.io.output.NullOutputStream;
import org.dataconservancy.pass.deposit.assembler.shared.ExplodedPackage;
import org.dataconservancy.pass.deposit.assembler.shared.PackageVerifier;
import org.dataconservancy.pass.deposit.assembler.shared.ResourceBuilderImpl;
import org.dataconservancy.pass.deposit.model.DepositSubmission;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static edu.jhu.library.pass.deposit.provider.shared.dspace.DspaceMetsPackageProvider.METS_XML;
import static org.apache.commons.io.filefilter.FileFilterUtils.nameFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.notFileFilter;
import static org.dataconservancy.pass.deposit.assembler.PackageOptions.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class DspaceMetsPackageVerifier implements PackageVerifier {

    private Checksum.OPTS defaultChecksumAlgo = Checksum.OPTS.MD5;

    public DspaceMetsPackageVerifier() {

    }

    public DspaceMetsPackageVerifier(Checksum.OPTS defaultChecksumAlgo) {
        this.defaultChecksumAlgo = defaultChecksumAlgo;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void verify(DepositSubmission submission, ExplodedPackage explodedPackage, Map<String, Object> options)
            throws Exception {
        // Verify custodial content is present and accounted for

        FileFilter excludeMets = notFileFilter(nameFileFilter(METS_XML, IOCase.SYSTEM));
        verifyCustodialFiles(submission, explodedPackage.getExplodedDir(), excludeMets, (baseDir, custodialFile) -> {
            return submission.getFiles()
                    .stream()
                    .filter(df -> df.getName().equals(custodialFile.getName()))
                    .findAny()
                    .orElseThrow(() -> new RuntimeException("Unable to map custodial file " + custodialFile +
                            " to a DepositFile in the submission.  If this file is a supplemental file, " +
                            "double-check the custodial file filter."));
        });

        // Verify supplemental content - in this case, METS.xml and its content

        Checksum.OPTS preferredChecksumAlgo = ((List<Checksum.OPTS>) options.getOrDefault(
                Checksum.KEY, Collections.singletonList(defaultChecksumAlgo))).get(0);

        File metsXml = new File(explodedPackage.getExplodedDir(), METS_XML);
        assertTrue(metsXml.exists() && metsXml.length() > 0);
        METSReader metsReader = new METSReader();
        metsReader.mapToDOM(new FileInputStream(metsXml));
        METS mets = new METSWrapper(metsReader.getMETSDocument()).getMETSObject();
        assertEquals(1, mets.getFileSec().getFileGrpByUse("CONTENT").size());

        mets.getFileSec().getFileGrpByUse("CONTENT").forEach(metsFileGroup -> {
            try {
                assertEquals(submission.getFiles().size(), metsFileGroup.getFiles().size());
                metsFileGroup.getFiles().forEach(metsFile -> {
                    File asJavaIoFile = null;
                    try {
                        asJavaIoFile = new File(explodedPackage.getExplodedDir(), metsFile.getFLocats().get(0).getHref());
                    } catch (METSException e) {
                        throw new RuntimeException(e);
                    }

                    assertTrue(asJavaIoFile.exists());

                    // assert preferred checksum type and value
                    assertEquals(preferredChecksumAlgo.toString(), metsFile.getChecksumType());
                    try {
                        ObservableInputStream obsIn = new ObservableInputStream(
                                new FileInputStream(asJavaIoFile));
                        ResourceBuilderImpl builder = new ResourceBuilderImpl();
                        obsIn.add(new DigestObserver(builder, preferredChecksumAlgo));
                        IOUtils.copy(obsIn, new NullOutputStream());
                        assertEquals(builder.build().checksum().asHex(), metsFile.getChecksum());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    // assert size
                    assertEquals(asJavaIoFile.length(), metsFile.getSize());

                    // TODO assert mime type?

                });
            } catch (METSException e) {
                throw new RuntimeException(e);
            }
        });

        // TODO: validate dmdSec and structMap
        /*
  <dmdSec GROUPID="21bd84fc-5722-400c-8a3f-c8f61d55b827" ID="997f0943-e079-4ce2-bd3c-e3233f7f602a">
    <mdWrap ID="472d554a-80e7-45a4-b2fc-87e9d0e27e8e" MDTYPE="DC">
      <xmlData>
        <qualifieddc xmlns="http://purl.org/dc/elements/1.1/" xmlns:dc="http://purl.org/dc/elements/1.1/"
        xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
        xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
          <dc:title xmlns:dc="http://purl.org/dc/elements/1.1/">Specific protein supplementation using soya, casein
          or whey differentially affects regional gut growth and luminal growth factor bioactivity in rats;
          implications for the treatment of gut injury and stimulating repair</dc:title>
          <dcterms:abstract xmlns:dcterms="http://purl.org/dc/terms/">Differential enhancement of luminal growth
          factor bioactivity and targeted regional gut growth occurs dependent on dietary protein supplement
          .</dcterms:abstract>
          <dc:publisher xmlns:dc="http://purl.org/dc/elements/1.1/">Royal Society of Chemistry (RSC)</dc:publisher>
          <dc:contributor xmlns:dc="http://purl.org/dc/elements/1.1/">Tania Marchbank</dc:contributor>
          <dc:contributor xmlns:dc="http://purl.org/dc/elements/1.1/">Nikki Mandir</dc:contributor>
          <dc:contributor xmlns:dc="http://purl.org/dc/elements/1.1/">Denis Calnan</dc:contributor>
          <dc:contributor xmlns:dc="http://purl.org/dc/elements/1.1/">Robert A. Goodlad</dc:contributor>
          <dc:contributor xmlns:dc="http://purl.org/dc/elements/1.1/">Theo Podas</dc:contributor>
          <dc:contributor xmlns:dc="http://purl.org/dc/elements/1.1/">Raymond J. Playford</dc:contributor>
          <dc:contributor xmlns:dc="http://purl.org/dc/elements/1.1/">Suzanne Vega</dc:contributor>
          <dc:contributor xmlns:dc="http://purl.org/dc/elements/1.1/">John Doe</dc:contributor>
          <dcterms:bibliographicCitation xmlns:dcterms="http://purl.org/dc/terms/">Tania Marchbank, Nikki Mandir,
          Denis Calnan, et al. "Specific protein supplementation using soya, casein or whey differentially affects
          regional gut growth and luminal growth factor bioactivity in rats; implications for the treatment of gut
          injury and stimulating repair." Food &amp; Function. 9 (1). 10.1039/c7fo01251a
          .</dcterms:bibliographicCitation>
        </qualifieddc>
      </xmlData>
    </mdWrap>
  </dmdSec>
  <dmdSec GROUPID="7c4e456c-16f7-4f10-9643-b0c3c898050e" ID="dd785fd0-374d-4027-8b03-00726b2a6ca0">
    <mdWrap ID="510247ff-c49f-4c0c-9630-a90947b07482" MDTYPE="OTHER" OTHERMDTYPE="DIM">
      <xmlData>
        <dim:dim xmlns:dim="http://www.dspace.org/xmlns/dspace/dim" xmlns:dc="http://purl.org/dc/elements/1.1/"
        xmlns:dcterms="http://purl.org/dc/terms/" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3
        .org/2001/XMLSchema-instance">
          <dim:field element="embargo" mdschema="local" qualifier="lift">2018-06-30</dim:field>
          <dim:field element="embargo" mdschema="local" qualifier="terms">2018-06-30</dim:field>
          <dim:field element="description" mdschema="dc" qualifier="provenance">Submission published under an
          embargo, which will last until 2018-06-30</dim:field>
        </dim:dim>
      </xmlData>
    </mdWrap>
  </dmdSec>
  <structMap ID="0f0e12ef-bc75-4bec-9cca-7192b45c5310" LABEL="DSpace CONTENT bundle structure">
    <div DMDID="997f0943-e079-4ce2-bd3c-e3233f7f602a dd785fd0-374d-4027-8b03-00726b2a6ca0"
    ID="fa1dbba7-9251-4c69-b889-45fa1a2becfb" LABEL="DSpace Item Div">
      <fptr FILEID="13ea17a9-54bd-446f-a344-756522e7be36" ID="e7ffad21-f66f-47f1-ba53-65f4f5282a65"/>
      <fptr FILEID="a53c4e20-0546-4e3d-a035-96dd27a9d33e" ID="b117fb4d-2ac4-4fdf-a7fa-e10903955cd0"/>
      <fptr FILEID="aa76676c-cde7-4730-8c79-1aee57c68e81" ID="5e1ff649-fae5-4f2d-8c36-32fb16639108"/>
      <fptr FILEID="55b929b8-8c90-41cd-952b-f29bfde7ba00" ID="fd92683b-23df-4f41-95d7-a715e7ee9b00"/>
    </div>
  </structMap>
         */
    }

    public Checksum.OPTS getDefaultChecksumAlgo() {
        return defaultChecksumAlgo;
    }

    public void setDefaultChecksumAlgo(Checksum.OPTS defaultChecksumAlgo) {
        this.defaultChecksumAlgo = defaultChecksumAlgo;
    }
}
