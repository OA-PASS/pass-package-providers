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
package edu.jhu.library.pass.deposit.provider.shared.dspace;

import org.dataconservancy.pass.deposit.DepositTestUtil;
import org.dataconservancy.pass.deposit.assembler.Assembler;
import org.dataconservancy.pass.deposit.assembler.PackageStream;
import org.dataconservancy.pass.deposit.builder.fs.FilesystemModelBuilder;
import org.dataconservancy.pass.deposit.model.DepositSubmission;
import org.junit.Before;
import org.junit.Test;
import resources.SharedSubmissionUtil;

import java.io.File;
import java.net.URI;

import static edu.jhu.library.pass.deposit.provider.shared.dspace.DspaceDepositTestUtil.getMetsXml;
import static org.dataconservancy.pass.deposit.DepositTestUtil.packageFile;
import static org.junit.Assert.assertNotNull;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public abstract class MultipleAssemblyDspaceMetsAssemblerIT extends BaseDspaceMetsAssemblerIT {

    /**
     * Re-use the same assembler instance across tests.  This is to demonstrate that the collaborating objects,
     * including the DspaceMetsDomWriter, do not maintain state across invocations of {@link
     * Assembler#assemble(DepositSubmission, java.util.Map)}
     */
    protected static DspaceMetsAssembler underTest;

    @Override
    public void setUp() throws Exception {
        assertNotNull("Subclasses must set the static field " + this.getClass().getSimpleName() + ".underTest " +
                "with an instance of the DspaceMetsAssembler being tested.", underTest);
    }

    @Before
    public void initBuilder() {
        builder = new FilesystemModelBuilder();
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

        File packageArchive = DepositTestUtil.savePackage(packageFile(this.getClass(), testName, stream.metadata()), stream);

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
