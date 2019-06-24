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

package edu.jhu.library.pass.deposit.provider.dash;

import edu.jhu.library.pass.deposit.provider.shared.dspace.MultipleAssemblyDspaceMetsAssemblerIT;
import org.dataconservancy.pass.client.PassClient;
import org.dataconservancy.pass.client.PassClientFactory;
import org.dataconservancy.pass.deposit.assembler.shared.AbstractAssembler;
import org.dataconservancy.pass.deposit.builder.fs.FilesystemModelBuilder;
import org.junit.BeforeClass;

import javax.xml.parsers.DocumentBuilderFactory;

public class DashMultipleAssemblyIT extends MultipleAssemblyDspaceMetsAssemblerIT {

    private static PassClient passClient = PassClientFactory.getPassClient();

    @BeforeClass
    public static void initAssembler() {
        underTest = new DashDspaceMetsAssembler(metadataBuilderFactory(), resourceBuilderFactory(), DocumentBuilderFactory.newInstance(), passClient);

        // Normally set by Spring, but since this IT isn't using Spring, we need to set these properties here
        underTest.setFedoraBaseUrl(System.getProperty("pass.fedora.baseurl", System.getenv("PASS_FEDORA_BASEURL")));
        underTest.setFedoraPassword(System.getProperty("pass.fedora.password", System.getenv("PASS_FEDORA_PASSWORD")));
        underTest.setFedoraUser(System.getProperty("pass.fedora.user", System.getenv("PASS_FEDORA_USER")));
    }

    /**
     * DASH ITs require DepositSubmissions to have Fedora URIs, because the {@link DashMetadataDomWriter} resolves
     * Grant and Funder resources from the Submission, and uses information in those resources in the DSpace metadata
     * when building a package.
     * <p>
     * It is problematic to mock a {@code PassClient} for a the {@code DashMetadataDomWriter} during an IT.  Attempting
     * to resolve local resource URIs like {@code submission:4} or {@code funder:2} will fail.  Therefore, the DASH ITs
     * use a Builder which deposits all resources to Fedora, and builds a {@code DepositSubmission} based on those
     * resources.
     * </p>
     * <p>
     * The DASH DOM writer will receive a DepositSubmission with Fedora URIs that will resolve as expected.
     * </p>
     */
    @Override
    public void initBuilder() {
        builder = new FilesystemModelBuilder(true);
    }

    @Override
    protected AbstractAssembler assemblerUnderTest() {
        return underTest;
    }
}
