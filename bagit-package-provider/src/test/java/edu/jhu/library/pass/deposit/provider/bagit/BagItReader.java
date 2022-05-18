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

import static edu.jhu.library.pass.deposit.provider.bagit.BagItWriter.CR;
import static edu.jhu.library.pass.deposit.provider.bagit.BagItWriter.CR_ENCODED;
import static edu.jhu.library.pass.deposit.provider.bagit.BagItWriter.LF;
import static edu.jhu.library.pass.deposit.provider.bagit.BagItWriter.LF_ENCODED;
import static edu.jhu.library.pass.deposit.provider.bagit.BagItWriter.PERCENT;
import static edu.jhu.library.pass.deposit.provider.bagit.BagItWriter.PERCENT_ENCODED;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;

/**
 * Reads BagIt metadata - bag declaration ({@code bagit.txt}), manifests, and bag metadata ({@code bag-info.txt}) - well
 * enough to support testing.
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class BagItReader {

    private static final String SEP_SPACE = " ";

    private static final String SEP_TAB = "\t";

    private static final String LABEL_SEP_SPACE = ": ";

    private static final String LABEL_SEP_TAB = ":\t";

    private Charset charset;

    public BagItReader(Charset charset) {
        this.charset = charset;
    }

    /**
     * Reads the Bag declaration ({@code bagit.txt}) and returns a Map of key value pairs in encounter order.
     * <p>
     * Note: the reader will always use UTF-8 when reading the declaration InputStream, regardless of the character
     * set supplied on reader construction.
     * </p>
     *
     * @param bagDeclaration the InputStream to the Bag declaration ({@code bagit.txt})
     * @return a Map of key value pairs in encounter order
     * @see <a href="https://tools.ietf.org/html/rfc8493#section-2.1.1">RFC 8493 §2.1.1</a>
     */
    LinkedHashMap<String, String> readBagDecl(InputStream bagDeclaration) {
        List<String> lines = null;
        try {
            lines = IOUtils.readLines(bagDeclaration, UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        LinkedHashMap<String, List<String>> entriesAndValues = parseLines(lines);
        return entriesAndValues
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey,
                                      entry -> entry.getValue().get(0),
                                      (entry1, entry2) -> {
                                          // merge function should never be invoked
                                          throw new RuntimeException("Duplicate label: " + entry1);
                                      },
                                      LinkedHashMap::new));
    }

    /**
     * Reads a manifest (e.g. {@code manifest-sha512.txt}) and returns a Map of key value pairs, where the key is the
     * file path in the manifest, and values are checksums.
     *
     * @param manifest the manifest InputStream
     * @return Map of checksums keyed by the file path, in encounter order
     */
    LinkedHashMap<String, String> readManifest(InputStream manifest) {
        List<String> lines = null;
        try {
            lines = IOUtils.readLines(manifest, charset);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        LinkedHashMap<String, String> entriesAndValues = parseManifestLines(lines);
        return entriesAndValues;
    }

    /**
     * Answers a list of labels in encounter order from {@code bag-info.txt}.  Labels may repeat.
     * <p>
     * Parsing relies on a properly formatted {@code bag-info.txt}, specifically that labels terminate with a colon and
     * single whitespace character (tab or space).
     * </p>
     *
     * @param bagInfo InputStream to a {@code bag-info.txt}
     * @return the labels in encounter order
     * @see <a href="https://tools.ietf.org/html/rfc8493#section-2.2.2">RFC 8493 §2.2.2</a>
     */
    List<String> readLabels(InputStream bagInfo) {
        List<String> lines = null;
        try {
            lines = IOUtils.readLines(bagInfo, charset);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return lines.stream()
                    .filter(line -> line.contains(LABEL_SEP_SPACE) || line.contains(LABEL_SEP_TAB))
                    .map(line -> {
                        if (line.contains(LABEL_SEP_SPACE)) {
                            return line.substring(0, line.indexOf(LABEL_SEP_SPACE));
                        }

                        return line.substring(0, line.indexOf(LABEL_SEP_TAB));
                    })
                    .collect(Collectors.toList());
    }

    /**
     * Answers a list of labels and their values in encounter order from {@code bag-info.txt}.  Labels will not repeat,
     * but multiple values for a label will be collected in encounter order.
     * <p>
     * Parsing relies on a properly formatted {@code bag-info.txt}, specifically that labels terminate with a colon and
     * single whitespace character (tab or space).
     * </p>
     *
     * @param bagInfo InputStream to a {@code bag-info.txt}
     * @return the labels and their values in encounter order
     * @see <a href="https://tools.ietf.org/html/rfc8493#section-2.2.2">RFC 8493 §2.2.2</a>
     */
    LinkedHashMap<String, List<String>> readLabelsAndValues(InputStream bagInfo) {
        List<String> lines = null;
        try {
            lines = IOUtils.readLines(bagInfo, charset);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return parseLines(lines);
    }

    static String decodePath(String path) {
        // Decode CR, LF, and percents that appear in a file path.

        StringBuilder encodedPath = new StringBuilder(path);
        StringBuilder decodedPath = new StringBuilder(path);

        Map<String, String> DECODE_MAP = new HashMap<String, String>() {
            {
                put(LF_ENCODED, String.valueOf(LF));
                put(CR_ENCODED, String.valueOf(CR));
                put(PERCENT_ENCODED, String.valueOf(PERCENT));
            }
        };

        Stream.of(LF_ENCODED, CR_ENCODED, PERCENT_ENCODED)
              .forEach(encodedToken -> {

                  // Offset into the encodedPath where we start searching for an encoded token
                  int offset = 0;

                  // Offset into the encodedPath where an encoded token was found
                  int index = -1;

                  // Offset into the decodedPath StringBuilder where a replacement should start
                  int replacementOffset = 0;

                  while ((index = encodedPath.indexOf(encodedToken, offset)) > -1) {
                      // note the replacementOffset will be negative, so we add the index and the replacementOffset.
                      decodedPath.replace((index + replacementOffset),
                                          (index + replacementOffset) + encodedToken.length(),
                                          DECODE_MAP.get(encodedToken));
                      replacementOffset = replacementOffset -
                                          (encodedToken.length() - DECODE_MAP.get(encodedToken).length());
                      offset = index + encodedToken.length();
                  }

                  // the encodedPath and the decodedPath need to have the same state prior to entering the
                  // search/replace while loop
                  encodedPath.setLength(decodedPath.length());
                  for (int i = 0; i < decodedPath.length(); i++) {
                      encodedPath.setCharAt(i, decodedPath.charAt(i));
                  }
              });

        return decodedPath.toString();
    }

    private LinkedHashMap<String, List<String>> parseLines(List<String> lines) {
        return lines.stream()
                    .filter(line -> line.contains(LABEL_SEP_SPACE) || line.contains(LABEL_SEP_TAB))
                    .collect(Collectors.toMap(line -> {
                        if (line.contains(LABEL_SEP_SPACE)) {
                            return line.substring(0, line.indexOf(LABEL_SEP_SPACE));
                        }

                        return line.substring(0, line.indexOf(LABEL_SEP_TAB));
                    }, line -> {
                        String value;
                        if (line.contains(LABEL_SEP_SPACE)) {
                            value = line.substring(line.indexOf(LABEL_SEP_SPACE) + LABEL_SEP_SPACE.length());
                        } else {
                            value = line.substring(line.indexOf(LABEL_SEP_TAB) + LABEL_SEP_TAB.length());
                        }
                        ArrayList<String> list = new ArrayList(1);
                        list.add(value);
                        return list;
                    }, (value1, value2) -> {
                        value1.addAll(value2);
                        return value1;
                    }, LinkedHashMap::new));
    }

    private LinkedHashMap<String, String> parseManifestLines(List<String> lines) {
        return lines.stream()
                    .filter(line -> line.contains(SEP_SPACE) || line.contains(SEP_TAB))
                    .collect(Collectors.toMap(line -> {
                        String[] result = line.split("\\h");
                        return decodePath(result[result.length - 1]);
                    }, line -> {
                        return line.split("\\h")[0];
                    }, (value1, value2) -> {
                        // merge function should never be invoked, as each line should have a unique path
                        throw new RuntimeException("Duplicate file path in manifest: " + value1);
                    }, LinkedHashMap::new));
    }

}
