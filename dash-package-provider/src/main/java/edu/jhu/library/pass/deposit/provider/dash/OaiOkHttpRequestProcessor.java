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
package edu.jhu.library.pass.deposit.provider.dash;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static edu.jhu.library.pass.deposit.provider.dash.OaiUrlBuilder.DIM_METADATA_PREFIX;
import static java.lang.String.format;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
@Component
class OaiOkHttpRequestProcessor implements OaiRequestProcessor {

    private OkHttpClient oaiClient;

    private OaiUrlBuilder urlBuilder;

    private OaiResponseBodyProcessor responseProcessor;

    @Autowired
    public OaiOkHttpRequestProcessor(OkHttpClient oaiClient,
                                     OaiUrlBuilder urlBuilder,
                                     OaiResponseBodyProcessor responseProcessor) {
        this.oaiClient = oaiClient;
        this.urlBuilder = urlBuilder;
        this.responseProcessor = responseProcessor;
    }

    public Stream<String> listIdentifiers(Instant from) {
        URL listRecords = null;
        String resumptionToken = null;
        List<String> recordIdentifiers = new ArrayList<>();

        do {
            listRecords = urlBuilder.listIdentifiers(DIM_METADATA_PREFIX, from, resumptionToken);

            // Headers like Accept, From, User-Agent, and authentication are added by the class that configures the
            // OkHttpClient as interceptors
            try (Response res = oaiClient.newCall(new Request.Builder()
                    .url(listRecords)
                    .build()).execute()) {
                if (res.code() != 200) {
                    throw new RuntimeException(
                            format("Error retrieving %s (code: %s): %s", listRecords, res.code(), res.message()));
                }

                resumptionToken = encode(responseProcessor.listIdentifiersResponse(res.body().byteStream(), recordIdentifiers));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } while (resumptionToken != null && resumptionToken.trim().length() > 0);

        return recordIdentifiers.stream();
    }

    public Optional<URL> analyzeRecords(URI submissionUri, Stream<String> oaiRecordIds) {
        return oaiRecordIds
                .map(oaiRecordId -> {
                    URL getRecord = urlBuilder.getRecord(oaiRecordId, DIM_METADATA_PREFIX);

                    try (Response res = oaiClient.newCall(new Request.Builder()
                            .url(getRecord)
                            .build()).execute()) {

                        if (res.code() != 200) {
                            throw new RuntimeException(
                                    format("Error retrieving %s (code: %s): %s", getRecord, res.code(), res.message()));
                        }

                        //  Parse request body for PASS Submission URI
                        //  Parse request body for Harvard DSpace Item URL (dc.identifier uri beginning 'http://nrs.harvard.edu')
                        return responseProcessor.getRecordResponse(res.body().byteStream(), submissionUri);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                })
                .filter(Objects::nonNull)
                .findAny();
    }

    static String encode(String token) {
        if (token == null || token.trim().length() == 0) {
            return token;
        }

        char[] illegal = new char[]{
                '/',
                '?',
                '#',
                '=',
                '&',
                ':',
                ';',
                ' ',
                '%',
                '+',
                '@',
                '$',
                ',',
                '"',
                '>',
                '<'};

        String[] encoding = {
                "%2F",
                "%3F",
                "%23",
                "%3D",
                "%26",
                "%3A",
                "%3B",
                "%20",
                "%25",
                "%2B",
                "%40",
                "%24",
                "%2C",
                "%22",
                "%3E",
                "%3C"};

        StringBuilder sb = new StringBuilder(token);

        int replacementOffset = 0;

        for (int i = 0, offset = 0; i < token.length(); i++, offset = (i + replacementOffset)) {
            char candidate = token.charAt(i);

            for (int j = 0; j < illegal.length; j++) {
                if (candidate == illegal[j]) {
                    sb.replace(offset, offset + 1, encoding[j]);
                    replacementOffset += encoding[j].length() - 1;
                    break;
                }
            }
        }

        return sb.toString();
    }

}