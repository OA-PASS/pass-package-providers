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

import static edu.jhu.library.pass.deposit.provider.bagit.TestUtil.randomUri;
import static java.lang.Math.pow;
import static java.lang.Math.round;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import com.github.jknack.handlebars.Handlebars;
import org.apache.commons.io.IOUtils;
import org.dataconservancy.pass.client.PassClient;
import org.dataconservancy.pass.deposit.model.DepositMetadata;
import org.dataconservancy.pass.deposit.model.DepositSubmission;
import org.dataconservancy.pass.model.Submission;
import org.dataconservancy.pass.model.User;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class HandlebarsParameterizerTest {

    private static final String HBM_TEMPLATE = "" +
                                               "Source-Organization: {{sourceOrganization}}\n" +
                                               "Organization-Address: {{organizationAddress}}\n" +
                                               "Contact-Name: {{contactName}}\n" +
                                               "Contact-Phone: {{contactEmail}}\n" +
                                               "Contact-Email: {{contactPhone}}\n" +
                                               "External-Description: Submitted as {{submissionUri}} to PASS on " +
                                               "{{submissionDate}} by {{submissionUserFullName}} " +
                                               "({{submissionUserEmail}}), published as {{publisherId}}\n" +
                                               "Bagging-Date: {{currentDate}}\n" +
                                               "External-Identifier: {{publisherId}}\n" +
                                               "Bag-Size: {{bagSizeHumanReadable}}\n" +
                                               "Payload-Oxum: {{custodialFileCount}}.{{bagSizeBytes}}\n" +
                                               "Internal-Sender-Identifier: {{submissionUri}}\n" +
                                               "Internal-Sender-Description: Submitted as {{submissionUri}} to PASS " +
                                               "on {{submissionDate}} by {{submissionUserFullName}} " +
                                               "({{submissionUserEmail}}), published as {{publisherId}}";

    private static final String PUBLICATION_DOI = "10.1039/c7fo01251a";

    private static final String SUBMISSION_METADATA = "{\"doi\": \"" + PUBLICATION_DOI + "\"}";

    private Submission passSubmission;

    private DepositSubmission depositSubmission;

    private User submitter;

    private BagModel model;

    private PassClient passClient;

    private HandlebarsParameterizer underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new HandlebarsParameterizer(new Handlebars());

        passSubmission = new Submission();
        depositSubmission = new DepositSubmission();
        submitter = new User();

        passSubmission.setId(randomUri());
        depositSubmission.setId(passSubmission.getId().toString());
        submitter.setId(randomUri());

        passSubmission.setSubmitter(submitter.getId());
        passSubmission.setSubmittedDate(DateTime.now());
        passSubmission.setMetadata(SUBMISSION_METADATA);

        DepositMetadata depositMetadata = mock(DepositMetadata.class);
        DepositMetadata.Article articleMetadata = mock(DepositMetadata.Article.class);
        when(depositMetadata.getArticleMetadata()).thenReturn(articleMetadata);
        when(articleMetadata.getDoi()).thenReturn(URI.create(PUBLICATION_DOI));
        depositSubmission.setMetadata(depositMetadata);

        submitter.setDisplayName("Joe User");
        submitter.setEmail("joeuser@user.com");

        model = new BagModel();
        model.setSourceOrganization("Johns Hopkins");
        model.setOrganizationAddress("3400 N. Charles St, Baltimore, MD 21218");
        model.setContactName("Joe Contact");
        model.setContactPhone("555-555-5555");
        model.setContactEmail("joecontact@jhu.edu");
        model.setSubmissionUser(submitter);
        model.setSubmission(passSubmission);
        model.setSubmissionDate(
            ISODateTimeFormat.basicDateTimeNoMillis().withZoneUTC().print(passSubmission.getSubmittedDate()));
        model.setSubmissionUserEmail(submitter.getEmail());
        model.setSubmissionUserUri(submitter.getId().toString());
        model.setSubmissionUserFullName("Joe User");
        model.setPublisherId(PUBLICATION_DOI);
        model.setSubmissionUri(passSubmission.getId().toString());
        model.setBagSizeHumanReadable("15 GiB");
        model.setBagSizeBytes(15 * (round(pow(2, 30))));
        model.setCustodialFileCount(300);
        model.setBagItVersion("1.0");
        model.setDepositSubmission(depositSubmission);
        model.setSubmissionUserFullName("Joe User");
        model.setSubmissionUserEmail("joeuser@user.com");
        model.setCurrentDate("2019-05-07");
    }

    @Test
    public void simple() {
        String result = underTest.parameterize(IOUtils.toInputStream(HBM_TEMPLATE, StandardCharsets.UTF_8), model);
        System.err.println(result);
    }
}