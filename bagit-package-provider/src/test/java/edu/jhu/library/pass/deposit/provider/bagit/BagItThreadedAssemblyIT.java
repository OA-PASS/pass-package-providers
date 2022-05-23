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
package edu.jhu.library.pass.deposit.provider.bagit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.dataconservancy.pass.client.PassClient;
import org.dataconservancy.pass.client.PassJsonAdapter;
import org.dataconservancy.pass.client.adapter.PassJsonAdapterBasic;
import org.dataconservancy.pass.deposit.assembler.Assembler;
import org.dataconservancy.pass.deposit.assembler.PackageOptions;
import org.dataconservancy.pass.deposit.assembler.shared.PackageVerifier;
import org.dataconservancy.pass.deposit.assembler.shared.ThreadedAssemblyIT;
import org.dataconservancy.pass.deposit.builder.fs.FilesystemModelBuilder;
import org.dataconservancy.pass.model.Submission;
import org.dataconservancy.pass.model.User;
import org.junit.Before;
import submissions.SubmissionResourceUtil;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class BagItThreadedAssemblyIT extends ThreadedAssemblyIT {

    // needs to be initialized here so it is visible by {@link #setupAssembler())}
    private PassClient passClient = mock(PassClient.class);

    private PassJsonAdapter adapter;

    private Map<URI, Submission> submissionMap;

    private Map<URI, User> userMap;

    @Before
    public void setUp() {
        this.builder = new FilesystemModelBuilder();
        this.adapter = new PassJsonAdapterBasic();

        Collection<URI> submissionUris = SubmissionResourceUtil.submissionUris();
        this.submissionMap = submissionUris.stream()
                                           .peek(uri -> System.err.println("Processing " + uri))
                                           .map(SubmissionResourceUtil::asJson)
                                           .flatMap(SubmissionResourceUtil::asStream)
                                           .filter(node -> node.has("@id") &&
                                                           node.has("@type") &&
                                                           node.get("@type").asText().equals("Submission"))
                                           .collect(Collectors.toMap(node -> URI.create(node.get("@id").asText()),
                                                                     node -> {
                                                                         try {
                                                                             return adapter.toModel(
                                                                                 toInputStream(node.toString(), UTF_8),
                                                                                 Submission.class);
                                                                         } catch (Exception e) {
                                                                             throw new RuntimeException(e);
                                                                         }
                                                                     }));

        this.userMap = submissionUris.stream()
                                     .map(SubmissionResourceUtil::asJson)
                                     .flatMap(SubmissionResourceUtil::asStream)
                                     .filter(node -> node.has("@id") &&
                                                     node.has("@type") && node.get("@type").asText().equals("User"))
                                     .collect(Collectors.toMap(node -> URI.create(node.get("@id").asText()),
                                                               node -> {
                                                                   try {
                                                                       return adapter.toModel(
                                                                           toInputStream(node.toString(), UTF_8),
                                                                           User.class);
                                                                   } catch (Exception e) {
                                                                       throw new RuntimeException(e);
                                                                   }
                                                               },
                                                               (user1, user2) -> user2));

        when(passClient.readResource(any(URI.class), eq(Submission.class)))
            .then(inv -> submissionMap.get(inv.getArgument(0)));

        when(passClient.readResource(any(URI.class), eq(User.class)))
            .then(inv -> userMap.get(inv.getArgument(0)));
    }

    @Override
    protected Assembler assemblerUnderTest() {
        return new BagItAssembler(mbf, rbf, passClient);
    }

    @Override
    protected Map<String, Object> packageOptions() {
        return new HashMap<String, Object>() {
            {
                put(PackageOptions.Spec.KEY, "BagIt 1.0");
                put(PackageOptions.Archive.KEY, PackageOptions.Archive.OPTS.ZIP);
                put(PackageOptions.Compression.KEY, PackageOptions.Compression.OPTS.NONE);
                put(PackageOptions.Checksum.KEY, Arrays.asList(PackageOptions.Checksum.OPTS.SHA512));
                put(BagItPackageProvider.BAGINFO_TEMPLATE, "/bag-info.hbm");
            }
        };
    }

    @Override
    protected PackageVerifier packageVerifier() {
        return new BagItPackageVerifier();
    }
}
