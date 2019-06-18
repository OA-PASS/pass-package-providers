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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

import static org.junit.Assert.assertTrue;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class DspaceDepositTestUtil {

    private static final Logger LOG = LoggerFactory.getLogger(DspaceDepositTestUtil.class);

    /**
     * Invokes {@link DspaceMetadataDomWriter#write(OutputStream)}, and returns a {@link Document} containing the the
     * parsed output.  This allows the internals of the {@code DspaceMetadataDomWriter} to change (to using SAX, for
     * example), without this test depending on the internal XML parsing model used by the writer.
     *
     * @param dbf
     * @param underTest
     * @return
     * @throws SAXException
     * @throws IOException
     * @throws ParserConfigurationException
     */
    public static Document writeAndParseResults(DocumentBuilderFactory dbf, DspaceMetadataDomWriter underTest)
            throws SAXException, IOException, ParserConfigurationException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        underTest.write(out);
//        System.err.println(">>> Wrote: \n" + out.toString("UTF-8"));
        Document result = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(out.toByteArray()));
        assertTrue(result.getChildNodes().getLength() > 0);
        return result;
    }

    /**
     * Returns the {@code mets.xml} from an opened package as a parsed {@link Document}
     *
     * @param extractedPackageDir the base directory of the extracted package
     * @return the METS file parsed into a DOM
     * @throws SAXException
     * @throws IOException
     * @throws ParserConfigurationException
     */
    public static Document getMetsXml(File extractedPackageDir) throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        return dbf.newDocumentBuilder().parse(Files.newInputStream(extractedPackageDir.toPath().resolve("mets.xml")));
    }
}
