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

import edu.jhu.library.pass.deposit.provider.shared.dspace.DspaceMetsAssembler;
import edu.jhu.library.pass.deposit.provider.shared.dspace.DspaceMetsPackageVerifier;
import org.dataconservancy.pass.client.PassClient;
import org.dataconservancy.pass.client.PassClientFactory;
import org.dataconservancy.pass.deposit.assembler.PackageOptions;
import org.dataconservancy.pass.deposit.assembler.shared.AbstractAssembler;
import org.dataconservancy.pass.deposit.assembler.shared.PackageVerifier;
import org.dataconservancy.pass.deposit.assembler.shared.ThreadedAssemblyIT;
import org.dataconservancy.pass.deposit.builder.fs.FilesystemModelBuilder;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonList;

public class DashMetsThreadedAssemblyIT extends ThreadedAssemblyIT {

    private PassClient passClient;

    @Override
    protected AbstractAssembler assemblerUnderTest() {
        builder = new FilesystemModelBuilder(true);
        passClient = PassClientFactory.getPassClient();
        DspaceMetsAssembler assembler = new DashDspaceMetsAssembler(mbf, rbf, DocumentBuilderFactory.newInstance(), passClient);

        // Normally set by Spring, but since this IT isn't using Spring, we need to set these properties here
        assembler.setFedoraBaseUrl(System.getProperty("pass.fedora.baseurl", System.getenv("PASS_FEDORA_BASEURL")));
        assembler.setFedoraPassword(System.getProperty("pass.fedora.password", System.getenv("PASS_FEDORA_PASSWORD")));
        assembler.setFedoraUser(System.getProperty("pass.fedora.user", System.getenv("PASS_FEDORA_USER")));

        return assembler;
    }

    @Override
    protected Map<String, Object> packageOptions() {
        return new HashMap<String, Object>() {
            {
                put(PackageOptions.Spec.KEY, DspaceMetsAssembler.SPEC_DSPACE_METS);
                put(PackageOptions.Archive.KEY, PackageOptions.Archive.OPTS.ZIP);
                put(PackageOptions.Compression.KEY, PackageOptions.Compression.OPTS.ZIP);
                put(PackageOptions.Checksum.KEY, singletonList(PackageOptions.Checksum.OPTS.SHA256));
            }
        };
    }

    @Override
    protected PackageVerifier packageVerifier() {
        return new DspaceMetsPackageVerifier();
    }
}
