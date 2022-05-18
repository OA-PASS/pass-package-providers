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
import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.MessageDigestCalculatingInputStream;
import org.apache.commons.io.output.NullOutputStream;
import org.dataconservancy.pass.deposit.assembler.PackageOptions.Checksum;
import org.dataconservancy.pass.deposit.assembler.shared.ExplodedPackage;
import org.dataconservancy.pass.deposit.assembler.shared.PackageVerifier;
import org.dataconservancy.pass.deposit.model.DepositFile;
import org.dataconservancy.pass.deposit.model.DepositSubmission;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class BagItPackageVerifier implements PackageVerifier {

    private BagItReader reader;

    private Charset expectedEncoding = UTF_8;

    /**
     * Verifies packages with a UTF-8 reader.
     */
    public BagItPackageVerifier() {
        this.reader = new BagItReader(expectedEncoding);
    }

    /**
     * Verifies packages with the supplied reader.
     *
     * @param reader the reader, which may use an encoding other than UTF-8
     */
    public BagItPackageVerifier(BagItReader reader) {
        this.reader = reader;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Implementation note: the supplied options {@code map} is empty.  See Javadoc for {@code
     * SubmitAndValidatePackagesIT#verifyPackages}.  Basically, the {@code SubmitAndValidatePackagesIT} does not have
     * access to the DS runtime configuration, so it cannot introspect the configuration, parse the assembler config,
     * and provide the options.
     * </p>
     * <p>
     * TODO: put the runtime configuration of Deposit Services in a well-known location (e.g. as a binary in Fedora)
     *   whereby the {@code SubmitAndValidatePackagesIT} can find it and supply it to test methods.
     * </p>
     * <p>
     * Until a workaround is implemented, this method will have to read the runtime configuration and parse the options
     * for itself.
     * </p>
     *
     * @param depositSubmission
     * @param explodedPackage
     * @param map
     * @throws Exception
     */
    @Override
    @SuppressWarnings("unchecked")
    public void verify(DepositSubmission depositSubmission, ExplodedPackage explodedPackage, Map<String, Object> map)
        throws Exception {

        // Directory under which the payload (i.e. custodial content of the submission) will be found
        final File payloadDir = new File(explodedPackage.getExplodedDir(), BagItPackageProvider.PAYLOAD_DIR);
        assertTrue("Missing payload directory: " + payloadDir, payloadDir.exists());

        // Maps payload file to a DepositFile
        final BiFunction<File, File, DepositFile> MAPPER = (packageDir, payloadFile) -> {
            return depositSubmission.getFiles()
                                    .stream()
                                    .filter(df -> BagItWriter.encodePath(df.getName()).endsWith(payloadFile.getName()))
                                    .findAny()
                                    .orElseThrow(
                                        () -> new RuntimeException("Missing custodial file '" + payloadFile + "'"));
        };

        // Filters for all files that have the payload directory as an ancestor (i.e. all files under "data/")
        final FileFilter payloadFilter = (file) -> {
            if (!file.isFile()) {
                return false;
            }
            File parent = (file.getParentFile() != null) ? file.getParentFile() : file;
            do {
                if (parent.equals(payloadDir)) {
                    return true;
                }
            } while ((parent = parent.getParentFile()) != null);

            return false;
        };

        // Insure that every file in the DepositSubmission is present in the payload, and that every payload file is
        // present in the DepositSubmission
        verifyCustodialFiles(depositSubmission, explodedPackage.getExplodedDir(), payloadFilter, MAPPER);

        // Verify the payload manifest for each checksum in the package options:
        //   Every file in the payload is found in the manifest
        //   Every entry in the manifest is present in the payload
        //   The checksum in the manifest matches the calculated checksum
        List<Checksum.OPTS> checksums = null;
        if (map == null || map.isEmpty()) {
            checksums = Arrays.asList(Checksum.OPTS.SHA512, Checksum.OPTS.MD5);
        } else {
            checksums = (List<Checksum.OPTS>) map.get(Checksum.KEY);
        }

        // must be at least one checksum specified in the package options
        assertTrue("Package options must specify at least one checksum.", checksums.size() > 0);
        checksums.forEach(algorithm -> {
            File manifest = new File(explodedPackage.getExplodedDir(),
                                     String.format(BagItPackageProvider.PAYLOAD_MANIFEST_TMPL,
                                                   algorithm.name().toLowerCase()));
            File tagManifest = new File(explodedPackage.getExplodedDir(),
                                        String.format(BagItPackageProvider.TAG_MANIFEST_TMPL,
                                                      algorithm.name().toLowerCase()));
            verifyManifest(depositSubmission.getFiles(), explodedPackage.getExplodedDir(), manifest, algorithm);
            verifyTagManifest(explodedPackage.getExplodedDir(), tagManifest, algorithm);
        });

        // Bag Decl
        File bagDecl = new File(explodedPackage.getExplodedDir(), BagItPackageProvider.BAGIT_TXT);
        verifyBagDecl(bagDecl, BagItVersion.BAGIT_1_0.getVersionString());

        // Bag Info
        File bagInfo = new File(explodedPackage.getExplodedDir(), BagItPackageProvider.BAGINFO_TXT);
        verifyBagInfo(bagInfo);

    }

    /**
     * Simply asserts that the {@code bag-info.txt} is present and contains at least one entry.
     *
     * @param bagInfo the {@code bag-info.txt} file
     * @see <a href="https://tools.ietf.org/html/rfc8493#section-2.2.2">RFC 8439 §2.2.2</a>
     */
    protected void verifyBagInfo(File bagInfo) {
        Map<String, List<String>> entries;

        try {
            entries = reader.readLabelsAndValues(new FileInputStream(bagInfo));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Missing Bag info file: '" + bagInfo + "'", e);
        }

        // TODO verify expected contents of bag info
        assertTrue(entries.size() > 0);
    }

    /**
     * Verifies:
     * <ul>
     *     <li>the bag declaration exists</li>
     *     <li>verifies the expected value of the BagIt-Version element</li>
     *     <li>verifies the expected value of the Tag-File-Character-Encoding element</li>
     * </ul>
     *
     * @param bagDecl         the bag declaration, {@code bagit.txt}
     * @param expectedVersion the expected version of BagIt to be found in the declaration
     * @see <a href="https://tools.ietf.org/html/rfc8493#section-2.1.1">RFC 8493 §2.1.1</a>
     */
    protected void verifyBagDecl(File bagDecl, String expectedVersion) {
        Map<String, String> entries;
        try {
            entries = reader.readBagDecl(new FileInputStream(bagDecl));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Missing Bag declaration file: '" + bagDecl + "'", e);
        }

        assertEquals(expectedVersion, entries.get(BagMetadata.BAGIT_VERSION));
        assertEquals(expectedEncoding.name(), entries.get(BagMetadata.TAG_FILE_ENCODING));
    }

    /**
     * Verifies:
     * <ul>
     *     <li>The tag manifest exists</li>
     *     <li>That the tag manifest does not exist under the payload directory</li>
     *     <li>That each tag file enumerated in the manifest is present in the package</li>
     *     <li>That the checksum for the tag file matches what is in the manifest</li>
     * </ul>
     *
     * @param packageDir      the base directory of the exploded package
     * @param tagManifestFile the tag manifest
     * @param algo            the checksum algorithm used to generate the manifest
     * @see <a href="https://tools.ietf.org/html/rfc8493#section-2.2.1">RFC 8439 §2.2.1</a>
     */
    protected void verifyTagManifest(File packageDir, File tagManifestFile, Checksum.OPTS algo) {
        assertTrue("Missing expected tag manifest '" + tagManifestFile + "'", tagManifestFile.exists());

        assertEquals(String.format(BagItPackageProvider.TAG_MANIFEST_TMPL, algo.name().toLowerCase()),
                     tagManifestFile.getName());

        // Read it in.
        Map<String, String> manifest;
        try {
            manifest = reader.readManifest(new FileInputStream(tagManifestFile));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Error reading tag manifest " + tagManifestFile + ": " + e.getMessage(), e);
        }

        // make sure each tag in the manifest is present
        // insure each file *is not* in the payload directory
        manifest.keySet().forEach(expectedTagFile -> {
            assertTrue(new File(packageDir, expectedTagFile).exists());
            assertFalse(expectedTagFile.startsWith(BagItPackageProvider.PAYLOAD_DIR));
        });

        // verify checksum of each tag file in the manifest
        manifest.forEach((key, value) -> {
            File payloadFile = new File(packageDir, key);
            try (NullOutputStream nullOut = new NullOutputStream();
                 FileInputStream fileIn = new FileInputStream(payloadFile);
                 MessageDigestCalculatingInputStream xsumCalculator = checksumCalculatorFor(fileIn, algo)) {
                IOUtils.copy(xsumCalculator, nullOut);
                assertEquals(value, encodeHexString(xsumCalculator.getMessageDigest().digest()));
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Missing expected tag manifest file: " + e.getMessage(), e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Verifies:
     * <ul>
     *     <li>the manifest file exists</li>
     *     <li>the name of the manifest file is correct</li>
     *     <li>each payload file is present in the manifest</li>
     *     <li>each file in the manifest is present in the payload</li>
     *     <li>the checksum of each file in the manifest</li>
     * </ul>
     *
     * @param payload      the custodial content of the submission, expected to be present in the BagIt payload
     * @param packageDir   the base directory of the exploded package
     * @param manifestFile the manifest file whose contents are to be verified
     * @param algo         the checksum algorithm used by the manifest file
     * @see <a href="https://tools.ietf.org/html/rfc8493#section-2.1.3">RFC 8439 §2.1.3</a>
     */
    protected void verifyManifest(List<DepositFile> payload, File packageDir, File manifestFile, Checksum.OPTS algo) {

        // Insure the manifest file exists
        assertTrue("Missing expected manifest file '" + manifestFile + "'", manifestFile.exists());

        // verify name of the manifest file conforms to the spec
        assertEquals(String.format(BagItPackageProvider.PAYLOAD_MANIFEST_TMPL,
                                   algo.name().toLowerCase()), manifestFile.getName());

        // Read it in.
        Map<String, String> manifest;
        try {
            manifest = reader.readManifest(new FileInputStream(manifestFile));
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Error reading manifest " + manifestFile + ": " + e.getMessage(), e);
        }

        // make sure each payload file is represented in the manifest
        payload.forEach(df -> {
            String encodedLocation = BagItWriter.encodePath(df.getName());
            String relative = null;
            if (encodedLocation.contains("/")) {
                relative = BagItPackageProvider.PAYLOAD_DIR +
                           encodedLocation.substring(encodedLocation.lastIndexOf("/"));
            } else {
                relative = BagItPackageProvider.PAYLOAD_DIR + "/" + encodedLocation;
            }
            File expectedPayloadFile = new File(packageDir, relative);
            assertTrue("Missing expected payload file '" + expectedPayloadFile + "'", expectedPayloadFile.exists());
            assertTrue(
                "Missing payload file '" + relative + "' from the manifest (package directory: " + packageDir + "')",
                manifest.containsKey(relative));
        });

        // make sure each file in the manifest is present in the payload
        // insure each file belongs to the payload directory
        manifest.keySet().forEach(expectedPayloadFile -> {
            assertTrue(new File(packageDir, expectedPayloadFile).exists());
            assertTrue(expectedPayloadFile.startsWith(BagItPackageProvider.PAYLOAD_DIR));
        });

        // verify checksum of each payload file in the manifest
        manifest.forEach((key, value) -> {
            File payloadFile = new File(packageDir, key);
            try (NullOutputStream nullOut = new NullOutputStream();
                 FileInputStream fileIn = new FileInputStream(payloadFile);
                 MessageDigestCalculatingInputStream xsumCalculator = checksumCalculatorFor(fileIn, algo)) {
                IOUtils.copy(xsumCalculator, nullOut);
                assertEquals(value, encodeHexString(xsumCalculator.getMessageDigest().digest()));
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Missing expected payload file: " + e.getMessage(), e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

    }

    /**
     * Returns a {@link MessageDigestCalculatingInputStream} by mapping the supplied checksum algorithm to a
     * {@link MessageDigest}.
     *
     * @param payloadFile  the payload file to calculate a checksum over
     * @param checksumAlgo the algorithm to use, must be mapped to a Java MessageDigest
     * @return an InputStream that will calculate a checksum for the supplied InputStream
     */
    private static MessageDigestCalculatingInputStream checksumCalculatorFor(InputStream payloadFile,
                                                                             Checksum.OPTS checksumAlgo) {
        MessageDigest md;

        try {
            switch (checksumAlgo) {
                case MD5:
                    md = MessageDigest.getInstance("MD5");
                    break;
                case SHA256:
                    md = MessageDigest.getInstance("SHA-256");
                    break;
                case SHA512:
                    md = MessageDigest.getInstance("SHA-512");
                    break;
                default:
                    throw new RuntimeException("No MessageDigest implementation found for " + checksumAlgo.name());

            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return new MessageDigestCalculatingInputStream(payloadFile, md);
    }
}
