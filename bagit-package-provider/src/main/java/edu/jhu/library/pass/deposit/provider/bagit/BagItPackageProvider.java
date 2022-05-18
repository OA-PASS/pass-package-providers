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

package edu.jhu.library.pass.deposit.provider.bagit;

import static java.net.URI.create;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.codec.binary.Hex.encodeHexString;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.dataconservancy.pass.client.PassClient;
import org.dataconservancy.pass.deposit.assembler.PackageOptions;
import org.dataconservancy.pass.deposit.assembler.PackageStream;
import org.dataconservancy.pass.deposit.assembler.shared.DepositFileResource;
import org.dataconservancy.pass.deposit.assembler.shared.PackageProvider;
import org.dataconservancy.pass.deposit.model.DepositSubmission;
import org.dataconservancy.pass.model.Submission;
import org.dataconservancy.pass.model.User;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

public class BagItPackageProvider implements PackageProvider {

    protected static final Logger LOG = LoggerFactory.getLogger(BagItPackageProvider.class);

    protected static final UnsupportedOperationException UOE =
        new UnsupportedOperationException("Representation only exists in-memory.");

    /**
     * Package options key that contains the classpath resource path of the {@code bag-info.txt} Handlebars template
     */
    protected static final String BAGINFO_TEMPLATE = "baginfo-template-resource";

    /**
     * Payload directory
     * https://www.rfc-editor.org/rfc/rfc8493.html#section-2.1.2
     */
    protected static final String PAYLOAD_DIR = "data";

    /**
     * Payload manifest (at least one)
     * https://www.rfc-editor.org/rfc/rfc8493.html#section-2.1.3
     */
    protected static final String PAYLOAD_MANIFEST_TMPL = "manifest-%s.txt";

    /**
     * Tagfile manifest (optional)
     * https://www.rfc-editor.org/rfc/rfc8493.html#section-2.2.1
     */
    protected static final String TAG_MANIFEST_TMPL = "tagmanifest-%s.txt";

    /**
     * bagit.txt file (required)
     * https://www.rfc-editor.org/rfc/rfc8493.html#section-2.1.1
     */
    protected static final String BAGIT_TXT = "bagit.txt";

    /**
     * bag-info.txt file (optional)
     * https://www.rfc-editor.org/rfc/rfc8493.html#section-2.2.2
     */
    protected static final String BAGINFO_TXT = "bag-info.txt";

    /**
     * fetch.txt (optional)
     * https://www.rfc-editor.org/rfc/rfc8493.html#section-2.2.3
     */
    protected static final String FETCH_TXT = "fetch.txt";

    /**
     * Tag file encoding
     * https://www.rfc-editor.org/rfc/rfc8493.html#section-2.1.1
     */
    protected Charset tagFileEncoding = UTF_8;

    /**
     * Supported BagIT version
     * https://www.rfc-editor.org/rfc/rfc8493.html#section-2.1.1
     */
    protected BagItVersion bagItVersion = BagItVersion.BAGIT_1_0;

    /**
     * Default checksum calculation algorithm when generating new Bags.
     * https://www.rfc-editor.org/rfc/rfc8493.html#section-2.4
     */
    protected BagAlgo defaultAlgo = BagAlgo.SHA512;

    /**
     * Whether or not this implementation will produce incomplete Bags.
     */
    protected FetchStrategy fetchStrategy = FetchStrategy.DISABLED;

    /**
     * Writer for Bag-related files
     */
    protected BagItWriter writer;

    /**
     * Runtime options provided to the Packager/Assembler
     */
    protected Map<String, Object> packageOpts;

    /**
     * PASS Repository client, used for resolving URI references in the Submission
     */
    protected PassClient passClient;

    /**
     * Handlebars parameterization of Bag metadata
     */
    protected Parameterizer parameterizer;

    public BagItPackageProvider(BagItWriter writer, Parameterizer parameterizer, PassClient passClient) {
        this.writer = writer;
        this.parameterizer = parameterizer;
        this.passClient = passClient;
    }

    @Override
    public void start(DepositSubmission submission, List<DepositFileResource> custodialResources,
                      Map<String, Object> packageOptions) {
        this.packageOpts = packageOptions;
    }

    /**
     * Answers a path for the custodial resource subordinate to the BagIt payload directory.
     * <p>
     * The existing path of the custodial resource is preserved, and prefixed with "{@code data/}".
     * </p>
     *
     * @param custodialResource the custodial resource (i.e. a resource that is part of the Bag payload)
     * @return the path of the resource prefixed with "{@code data/}"
     * @throws RuntimeException if there is an error obtaining the path of the custodial resource
     */
    @Override
    public String packagePath(DepositFileResource custodialResource) {
        // payload directory: https://www.rfc-editor.org/rfc/rfc8493.html#section-2.1.2
        try {
            String encodedPath = BagItWriter.encodePath(custodialResource.getDepositFile().getName());
            LOG.debug("Encoded '{}' as '{}'", custodialResource.getDepositFile().getName(), encodedPath);
            return String.format("%s/%s", PAYLOAD_DIR, encodedPath);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public List<SupplementalResource> finish(DepositSubmission submission,
                                             List<PackageStream.Resource> packageResources) {

        List<SupplementalResource> supplementalResources =
            new ArrayList<>(writePayloadManifests(submission, packageResources, packageOpts));
        supplementalResources.add(writeBagDeclaration());
        supplementalResources.add(writeBagInfo(submission, packageResources,
                                               this.getClass()
                                                   .getResourceAsStream((String) packageOpts.get(BAGINFO_TEMPLATE))));
        supplementalResources.addAll(
            writeTagfileManifests(submission, packageResources, packageOpts, supplementalResources));

        return supplementalResources;
    }

    /**
     * Writes a payload manifest for each checksum supplied in the packager options.
     *
     * @param submission       the submission in the Deposit Services model
     * @param packageResources the custodial files being streamed in the package
     * @param packageOptions   the options supplied to the Assembler when creating the package
     * @return the BagIt payload manifests
     */
    @SuppressWarnings("unchecked")
    protected Collection<SupplementalResource> writePayloadManifests(DepositSubmission submission,
                                                                     List<PackageStream.Resource> packageResources,
                                                                     Map<String, Object> packageOptions) {

        // Generate a payload manifest for each checksum in the package options
        Collection<PackageOptions.Checksum.OPTS> checksums = (Collection<PackageOptions.Checksum.OPTS>)
            packageOptions.get(PackageOptions.Checksum.KEY);

        List<SupplementalResource> manifests = new ArrayList<>(checksums.size());

        checksums.forEach(checksum -> {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BagAlgo algo = BagAlgo.valueOf(checksum.name());

            packageResources.forEach(resource -> {

                PackageStream.Checksum resourceChecksum = resource.checksums().stream()
                                                                  .filter(
                                                                      candidate -> candidate.algorithm() == checksum)
                                                                  .findAny()
                                                                  .orElseThrow(() ->
                                                                                   new RuntimeException(
                                                                                       "Missing " + checksum.name() + " checksum for " + resource.name()));

                try {
                    writer.writeManifestLine(out, resourceChecksum.asHex(), resource.name());
                } catch (IOException e) {
                    throw new RuntimeException("Error writing manifest: " + e.getMessage(), e);
                }

            });

            String payloadManifestName = String.format(PAYLOAD_MANIFEST_TMPL, algo.getAlgo());
            manifests.add(new TagFile(payloadManifestName,
                                      payloadManifestName,
                                      out.toByteArray(),
                                      "Bag payload manifest for checksum algorithm " + algo.getAlgo()));
        });

        return manifests;

    }

    @SuppressWarnings("unchecked")
    protected Collection<SupplementalResource> writeTagfileManifests(DepositSubmission submission,
                                                                     List<PackageStream.Resource> packageResources,
                                                                     Map<String, Object> packageOptions,
                                                                     Collection<SupplementalResource> tagFiles) {

        // Generate a tag manifest for each checksum in the package options
        Collection<PackageOptions.Checksum.OPTS> checksums = (Collection<PackageOptions.Checksum.OPTS>)
            packageOptions.get(PackageOptions.Checksum.KEY);

        List<SupplementalResource> manifests = new ArrayList<>(checksums.size());

        checksums.forEach(checksumAlgo -> {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BagAlgo algo = BagAlgo.valueOf(checksumAlgo.name());

            tagFiles.stream()
                    .map(resource -> (TagFile) resource)
                    .forEach(tagFile -> {
                        MessageDigest md = resolveMessageDigest(checksumAlgo);
                        String checksum = encodeHexString(md.digest(tagFile.in));

                        try {
                            writer.writeManifestLine(out, checksum, tagFile.packagePath);
                        } catch (IOException e) {
                            throw new RuntimeException("Error writing manifest: " + e.getMessage(), e);
                        }

                    });

            String tagFileManifestName = String.format(TAG_MANIFEST_TMPL, algo.getAlgo());
            manifests.add(new TagFile(tagFileManifestName,
                                      tagFileManifestName,
                                      out.toByteArray(),
                                      "Bag payload manifest for checksum algorithm " + algo.getAlgo()));
        });

        return manifests;

    }

    protected SupplementalResource writeBagInfo(DepositSubmission submission,
                                                List<PackageStream.Resource> packageResources,
                                                InputStream bagInfoMustacheTemplate) {

        BagModel model = new BagModel();

        long streamCount = packageResources.size();
        long octetCount = packageResources.stream().mapToLong(PackageStream.Resource::sizeBytes).sum();

        Submission passSubmission = passClient.readResource(create(submission.getId()), Submission.class);
        model.setSubmission(passSubmission);
        model.setDepositSubmission(submission);
        model.setSubmissionUri(submission.getId());
        model.setBagItVersion(bagItVersion.getVersionString());
        model.setBagSizeBytes(octetCount);
        model.setCustodialFileCount(streamCount);
        model.setSubmissionMetadata(passSubmission.getMetadata());
        model.setSubmissionDate(ISODateTimeFormat.basicDateTimeNoMillis().print(passSubmission.getSubmittedDate()));

        User passUser = passClient.readResource(passSubmission.getSubmitter(), User.class);
        model.setSubmissionUser(passUser);
        model.setSubmissionUri(passUser.getId().toString());
        model.setSubmissionUserEmail(passUser.getEmail());
        model.setSubmissionUserFullName(passUser.getDisplayName());

        if (submission.getMetadata().getArticleMetadata().getDoi() != null) {
            model.setPublisherId(submission.getMetadata().getArticleMetadata().getDoi().toString());
        }

        String sizeTmpl = "%s %s";
        String size;
        String unit;
        if (octetCount < BagMetadata.ONE_KIBIBYTE) {
            size = String.valueOf(octetCount);
            unit = "bytes";
        } else if (octetCount < BagMetadata.ONE_MEBIBYTE) {
            size = String.valueOf(Math.floorDiv(octetCount, BagMetadata.ONE_KIBIBYTE));
            unit = "KiB";
        } else if (octetCount < BagMetadata.ONE_GIBIBYTE) {
            size = String.valueOf(Math.floorDiv(octetCount, BagMetadata.ONE_MEBIBYTE));
            unit = "MiB";
        } else if (octetCount < BagMetadata.ONE_TEBIBYTE) {
            size = String.valueOf(Math.floorDiv(octetCount, BagMetadata.ONE_GIBIBYTE));
            unit = "GiB";
        } else {
            size = String.valueOf(Math.floorDiv(octetCount, BagMetadata.ONE_TEBIBYTE));
            unit = "TiB";
        }
        model.setBagSizeHumanReadable(String.format(sizeTmpl, size, unit));

        String bagInfo = parameterizer.parameterize(bagInfoMustacheTemplate, model);

        return new TagFile(BAGINFO_TXT, BAGINFO_TXT, bagInfo.getBytes(tagFileEncoding), "Bag Metadata");

    }

    /**
     * Answers a Bag Declaration according to version 1.0 of the BagIt specification.
     *
     * @return the Bag Declaration
     * @throws RuntimeException if there is an error writing the declaration
     */
    protected SupplementalResource writeBagDeclaration() {

        ByteArrayOutputStream bagDecl = new ByteArrayOutputStream();

        try {
            writer.writeTagLine(bagDecl, BagMetadata.BAGIT_VERSION, bagItVersion.getVersionString());
            writer.writeTagLine(bagDecl, BagMetadata.TAG_FILE_ENCODING, tagFileEncoding.name());
        } catch (IOException e) {
            throw new RuntimeException("Error writing Bag Declaration: " + e.getMessage(), e);
        }

        return new TagFile(BAGIT_TXT, BAGIT_TXT, bagDecl.toByteArray(), "Bag Declaration");

    }

    protected static MessageDigest resolveMessageDigest(PackageOptions.Checksum.OPTS checksumAlgo) {

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

        return md;
    }

    /**
     * Encapsulates a BagIt tag file as a Deposit Services Package Provider Supplemental Resource
     */
    class TagFile implements SupplementalResource {

        private String filename;
        private String packagePath;
        private byte[] in;
        private long contentLength;
        private String description;

        TagFile() {

        }

        TagFile(String filename, String packagePath, byte[] content, String desc) {
            this.filename = filename;
            this.packagePath = packagePath;
            this.in = content;
            this.contentLength = content.length;
            this.description = desc;
        }

        long getContentLength() {
            return contentLength;
        }

        void setContentLength(long contentLength) {
            this.contentLength = contentLength;
        }

        void setFilename(String filename) {
            this.filename = filename;
        }

        void setPackagePath(String packagePath) {
            this.packagePath = packagePath;
        }

        byte[] getContent() {
            return in;
        }

        void setContent(byte[] content) {
            this.in = content;
        }

        void setDescription(String description) {
            this.description = description;
        }

        @Override
        public String getPackagePath() {
            return packagePath;
        }

        @Override
        public boolean exists() {
            return false;
        }

        @Override
        public long contentLength() throws IOException {
            return contentLength;
        }

        @Override
        public long lastModified() throws IOException {
            return System.currentTimeMillis();
        }

        @Override
        public String getFilename() {
            return filename;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(in);
        }

        @Override
        public Resource createRelative(String relativePath) throws IOException {
            throw UOE;
        }

        @Override
        public URL getURL() throws IOException {
            throw UOE;
        }

        @Override
        public URI getURI() throws IOException {
            throw UOE;
        }

        @Override
        public File getFile() throws IOException {
            throw UOE;
        }

    }


}
