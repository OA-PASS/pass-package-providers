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
package org.dataconservancy.pass.deposit.provider.nihms;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.filefilter.FileFilterUtils.nameFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.notFileFilter;
import static org.apache.commons.io.filefilter.FileFilterUtils.or;
import static org.dataconservancy.pass.deposit.provider.nihms.NihmsAssembler.BULK_META_FILENAME;
import static org.dataconservancy.pass.deposit.provider.nihms.NihmsAssembler.MANIFEST_FILENAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.dataconservancy.pass.deposit.assembler.shared.ExplodedPackage;
import org.dataconservancy.pass.deposit.assembler.shared.PackageVerifier;
import org.dataconservancy.pass.deposit.model.DepositSubmission;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class NihmsPackageVerifier implements PackageVerifier {

    @Override
    public void verify(DepositSubmission submission, ExplodedPackage explodedPackage, Map<String, Object> map)
        throws Exception {
        FileFilter custodialFiles = notFileFilter(or
                                                      (nameFileFilter(BULK_META_FILENAME),
                                                       nameFileFilter(MANIFEST_FILENAME)));

        verifyCustodialFiles(submission, explodedPackage.getExplodedDir(), custodialFiles, (packageDirectory, file) -> {
            return submission.getFiles()
                             .stream()
                             .filter(df -> df.getName()
                                             .equals(file.getName())).findAny()
                             .orElseThrow(
                                 () -> new RuntimeException("Unable to find file " + file + " in the Submission"));
        });

        // Verify supplemental files (i.e. non-custodial content like metadata) exist and have expected content
        File bulk_meta = new File(explodedPackage.getExplodedDir(), NihmsAssembler.BULK_META_FILENAME);
        File manifest = new File(explodedPackage.getExplodedDir(), NihmsAssembler.MANIFEST_FILENAME);

        assertTrue(bulk_meta.exists() && bulk_meta.length() > 0);
        assertTrue(manifest.exists() && manifest.length() > 0);

        String expectedManifest = IOUtils.toString(
            new NihmsManifestSerializer(submission.getManifest()).serialize().getInputStream(), UTF_8);
        String expectedBulkMeta = IOUtils.toString(
            new NihmsMetadataSerializer(submission.getMetadata()).serialize().getInputStream(), UTF_8);

        assertEquals(expectedManifest, IOUtils.toString(new FileInputStream(manifest), UTF_8));
        assertEquals(expectedBulkMeta, IOUtils.toString(new FileInputStream(bulk_meta), UTF_8));
    }

}
