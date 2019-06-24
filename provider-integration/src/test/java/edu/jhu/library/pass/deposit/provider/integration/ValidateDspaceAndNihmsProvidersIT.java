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
package edu.jhu.library.pass.deposit.provider.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.jhu.library.pass.deposit.provider.bagit.BagItPackageVerifier;
import edu.jhu.library.pass.deposit.provider.shared.dspace.DspaceMetsPackageVerifier;
import org.dataconservancy.pass.deposit.assembler.PackageOptions;
import org.dataconservancy.pass.deposit.assembler.shared.ExplodedPackage;
import org.dataconservancy.pass.deposit.assembler.shared.PackageVerifier;
import org.dataconservancy.pass.deposit.integration.shared.SubmitAndValidatePackagesIT;
import org.dataconservancy.pass.deposit.model.DepositSubmission;
import org.dataconservancy.pass.deposit.provider.nihms.NihmsPackageVerifier;
import org.dataconservancy.pass.model.Repository;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.fail;

/**
 * Verifies packages created by the DSpace and NIHMS assemblers.
 * <p>
 * The Package Providers image created in the {@code provider-integration} module uses a custom runtime configuration
 * for Deposit Services, present as a class path resource in {@code src/main/docker/repositories.json}.  Note that there
 * are two configured repositories, and each repository is configured with a Filesystem Transport as required by the
 * {@link SubmitAndValidatePackagesIT}.
 * </p>
 * <p>
 * There is one non-ideal coupling right now between this class and the {@code SubmitAndValidatePackagesIT}: the
 * submissions submitted by {@link SubmitAndValidatePackagesIT#performSubmissions()} must contain {@code Repository}
 * resources that match the repositories configured in {@code src/main/docker/repositories.json}.  This is really a
 * must-fix before this code is released to the wild for developers to use.  The quick fix is for this class to
 * override {@link SubmitAndValidatePackagesIT#performSubmissions() performSubmissions()}, thereby making this
 * connection between {@code Repository} in the submission graphs and the repository configuration in Deposit Services
 * explicit.  A more clever solution may be to create {@code Repository} resources dynamically based on the runtime
 * configuration present in {@code repositories.json}
 * </p>
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
@ContextConfiguration(classes = SpringContextConfiguration.class)
public class ValidateDspaceAndNihmsProvidersIT extends SubmitAndValidatePackagesIT {

    private static final Logger LOG = LoggerFactory.getLogger(ValidateDspaceAndNihmsProvidersIT.class);

    // Meta TODO: review below TODOs for accuracy/need

    // TODO: Submit to NIHMS Repository using Filesystem Transport and verify package

    // TODO: Submit to J10P Repository using Filesystem Transport and verify package

    // TODO: Submit to J10P Repository using SWORD Transport and verify logical success (separate IT)

    // TODO: Submit to NIHMS Repository using FTP Transport and verify logical success (separate IT)

    private DspaceMetsPackageVerifier dspaceVerifier;

    private NihmsPackageVerifier nihmsVerifier;

    private BagItPackageVerifier bagitVerifier;

    /**
     * Initializes the package verifiers.
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        dspaceVerifier = new DspaceMetsPackageVerifier(PackageOptions.Checksum.OPTS.SHA512);
        nihmsVerifier = new NihmsPackageVerifier();
        bagitVerifier = new BagItPackageVerifier();

        Set<String> existingRepos = passClient
                .findAllByAttribute(Repository.class, "repositoryKey", "*")
                .stream()
                .map(uri -> passClient.readResource(uri, Repository.class))
                .map(Repository::getRepositoryKey)
                .collect(Collectors.toSet());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(this.getClass().getResourceAsStream("/repositories.json"));
        node.fieldNames().forEachRemaining(repoKey -> {
            if (!existingRepos.contains(repoKey)) {
                System.out.println("Dynamically creating Repository with key '" + repoKey + "'");
                Repository repo = new Repository();
                repo.setRepositoryKey(repoKey);
                repo.setDescription("Dynamically created by " + this.getClass().getName());
                repo.setName("Dynamic " + repoKey);
                existingRepos.add(repoKey);
            }
        });
    }

    /**
     * {@inheritDoc}
     * <h4>Implementation note</h4>
     * Uses the name of the package archive to choose which {@code PackageVerifier} to return.  The runtime
     * configuration of Deposit Services places NIH/PMC packages in one directory, and DSpace packages in another.
     *
     * @param depositSubmission {@inheritDoc}
     * @param explodedPackage {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    protected PackageVerifier getVerifier(DepositSubmission depositSubmission, ExplodedPackage explodedPackage) {
        if (explodedPackage.getPackageFile().toString().contains("pmc")) {
            return nihmsVerifier;
        }

        if (explodedPackage.getPackageFile().toString().contains("jscholarship")) {
            return dspaceVerifier;
        }

        if (explodedPackage.getPackageFile().toString().contains("bagit")) {
            return bagitVerifier;
        }

        if (explodedPackage.getPackageFile().toString().contains("dash")) {
            return dspaceVerifier;
        }

        fail("Unable to select PackageVerifier");
        return null;
    }

    /**
     * {@inheritDoc}
     * <h4>Implementation note</h4>
     * Hard-codes the return because this class is knowledgeable of the runtime configuration used by Docker for
     * Deposit Services.
     *
     * @param packageFile
     * @return
     */
    @Override
    protected PackageOptions.Compression.OPTS sniffCompression(File packageFile) {
        return PackageOptions.Compression.OPTS.NONE;
    }

    /**
     * {@inheritDoc}
     * <h4>Implementation note</h4>
     * Hard-codes the return because this class is knowledgeable of the runtime configuration used by Docker for
     * Deposit Services.
     *
     * @param packageFile
     * @return
     */
    @Override
    protected PackageOptions.Archive.OPTS sniffArchive(File packageFile) {
        return PackageOptions.Archive.OPTS.ZIP;
    }

}
