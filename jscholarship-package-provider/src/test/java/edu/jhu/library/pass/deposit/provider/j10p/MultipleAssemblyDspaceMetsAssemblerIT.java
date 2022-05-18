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

import static edu.jhu.library.pass.deposit.provider.j10p.DspaceDepositTestUtil.getMetsXml;
import static org.dataconservancy.pass.deposit.DepositTestUtil.packageFile;

import java.io.File;
import java.net.URI;
import javax.xml.parsers.DocumentBuilderFactory;

import org.dataconservancy.pass.deposit.DepositTestUtil;
import org.dataconservancy.pass.deposit.assembler.Assembler;
import org.dataconservancy.pass.deposit.assembler.PackageStream;
import org.dataconservancy.pass.deposit.builder.fs.FilesystemModelBuilder;
import org.dataconservancy.pass.deposit.model.DepositSubmission;
import org.junit.BeforeClass;
import org.junit.Test;
import resources.SharedSubmissionUtil;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class MultipleAssemblyDspaceMetsAssemblerIT extends BaseDspaceMetsAssemblerIT {

    /**
     * Re-use the same assembler instance across tests.  This is to demonstrate that the collaborating objects,
     * including the DspaceMetsDomWriter, do not maintain state across invocations of {@link
     * Assembler#assemble(DepositSubmission, java.util.Map)}
     */
    private static DspaceMetsAssembler underTest;

    @Override
    public void setUp() throws Exception {
        builder = new FilesystemModelBuilder();
    }

    /**
     * Creates an instance of DsspaceMetsAssembler that is shared across test method invocations.
     * See {@link #assemblePackage(URI)}.
     */
    @BeforeClass
    public static void initAssembler() {
        DspaceMetadataDomWriterFactory metsWriterFactory = new DspaceMetadataDomWriterFactory(
            DocumentBuilderFactory.newInstance());
        DspaceMetsPackageProviderFactory packageProviderFactory = new DspaceMetsPackageProviderFactory(
            metsWriterFactory);
        underTest = new DspaceMetsAssembler(metadataBuilderFactory(), resourceBuilderFactory(), packageProviderFactory);
    }

    /**
     * Mocks a submission, and invokes the assembler to create a package based on the resources under the
     * {@code sample1/} resource path.  Sets the {@link #extractedPackageDir} to the base directory of the newly created
     * and extracted package.
     */
    private void assemblePackage(URI submissionUri) throws Exception {
        submissionUtil = new SharedSubmissionUtil();
        mbf = metadataBuilderFactory();
        rbf = resourceBuilderFactory();

        prepareSubmission(submissionUri);

        prepareCustodialResources();

        // Both tests in this IT will execute assemble(...) on the same instance of DspaceMetsAssembler because the
        // field is static
        PackageStream stream = underTest.assemble(submission, getOptions());

        File packageArchive = DepositTestUtil.savePackage(packageFile(this.getClass(), testName, stream.metadata()),
                                                          stream);

        verifyStreamMetadata(stream.metadata());

        extractPackage(packageArchive, stream.metadata().archive(), stream.metadata().compression());
    }

    @Test
    public void assembleSample1() throws Exception {
        assemblePackage(URI.create("fake:submission1"));
        verifyPackageStructure(getMetsXml(extractedPackageDir), extractedPackageDir, custodialResources);
    }

    @Test
    public void assembleSample2() throws Exception {
        assemblePackage(URI.create("fake:submission2"));
        verifyPackageStructure(getMetsXml(extractedPackageDir), extractedPackageDir, custodialResources);
    }

}
