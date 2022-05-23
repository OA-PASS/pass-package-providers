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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class BagItWriter {

    static final char COLON = 0x3a;

    static final char LF = 0x0a;

    static final char CR = 0x0d;

    static final char PERCENT = 0x25;

    static final char SPACE = 0x20;

    static final char TAB = 0x09;

    static final String CR_ENCODED = "%0D";

    static final String LF_ENCODED = "%0A";

    static final String PERCENT_ENCODED = "%25";

    private static final String TAG_LINE = "%s: %s\n";

    private static final String MANIFEST_LINE = "%s %s\n";

    private Charset charset;

    public BagItWriter(Charset charset) {
        this.charset = charset;
    }

    public void writeTagLine(OutputStream out, String label, String value) throws IOException {
        validateLabel(label);
        String line = String.format(TAG_LINE, label, value);
        // todo: format value for lines longer than 79 chars?
        out.write(line.getBytes(charset));
    }

    /**
     * Each line of a payload manifest file MUST be of the form
     *
     * checksum filepath
     *
     * where _filepath_ is the pathname of a file relative to the base
     * directory, and _checksum_ is a hex-encoded checksum calculated by
     * applying _algorithm_ over the file.
     *
     * o  The hex-encoded checksum MAY use uppercase and/or lowercase
     * letters.
     *
     * o  The slash character ('/') MUST be used as a path separator in
     * _filepath_.
     *
     * o  One or more linear whitespace characters (spaces or tabs) MUST
     * separate _checksum_ from _filepath_.
     *
     * o  There is no limitation on the length of a pathname.
     *
     * o  The payload manifest MUST NOT reference files outside the payload
     * directory.
     *
     * o  If a _filepath_ includes a Line Feed (LF), a Carriage Return (CR),
     * a Carriage-Return Line Feed (CRLF), or a percent sign (%), those
     * characters (and only those) MUST be percent-encoded following
     * [RFC3986].
     *
     * A manifest MUST NOT reference directories.  Bag creators who wish to
     * create an otherwise empty directory have typically done so by
     * creating an empty placeholder file with a name such as ".keep".
     *
     * @param out
     * @param checksum
     * @param filepath
     * @throws IOException
     */
    public void writeManifestLine(OutputStream out, String checksum, String filepath) throws IOException {
        String encodedPath = encodeLine(filepath);
        String line = String.format(MANIFEST_LINE, checksum, encodedPath);
        out.write(line.getBytes(charset));
    }

    /**
     * Insures that a <em>line</em> (e.g. in a payload or tag manifest) properly encodes line feeds, carriage returns,
     * and percent.  The supplied {@code line} may end with a CR, LF, or CRLF.  In that case, the ending character will
     * <em>not</em> be encoded.
     * <p>
     * Note: differs from {@link #encodePath(String)} with respect to treatment of line endings.
     * </p>
     *
     * @param line the line to be encoded
     * @return the encoded line, preserving the ending CR, LF, or CRLF
     */
    static String encodeLine(String line) {

        StringBuilder sb = new StringBuilder(line);

        int replacementOffset = 0;

        for (int i = 0, offset = 0; i < line.length(); i++, offset = (i + replacementOffset)) {
            char candidate = line.charAt(i);

            // Check to see if the line ends with CRLF; if so, return
            if (i == line.length() - 2) {
                if (candidate == CR && line.charAt(i + 1) == LF) {
                    return sb.toString();
                }
            }

            // Only encode CR and LF if they are not the last character in the line
            if (candidate == CR && i < line.length() - 1) {
                sb.replace(offset, offset + 1, CR_ENCODED);
                replacementOffset += CR_ENCODED.length() - 1;
            }

            if (candidate == LF && i < line.length() - 1) {
                sb.replace(offset, offset + 1, LF_ENCODED);
                replacementOffset += LF_ENCODED.length() - 1;
            }

            // Always encode PERCENT
            if (candidate == PERCENT) {
                sb.replace(offset, offset + 1, PERCENT_ENCODED);
                replacementOffset += PERCENT_ENCODED.length() - 1;
            }
        }

        return sb.toString();
    }

    /**
     * Insures that a <em>file path</em> (e.g. in a payload or tag manifest) properly encodes line feeds, carriage
     * returns, and percent.  The supplied {@code path} may end with a CR, LF, or CRLF.  In that case, the ending
     * character <em>will be</em> encoded.
     * <p>
     * Note: differs from {@link #encodeLine(String)} with respect to treatment of line endings.
     * </p>
     *
     * @param path the file path to be encoded
     * @return the encoded path
     */
    static String encodePath(String path) {
        StringBuilder sb = new StringBuilder(path);

        int replacementOffset = 0;

        for (int i = 0, offset = 0; i < path.length(); i++, offset = (i + replacementOffset)) {
            char candidate = path.charAt(i);

            if (candidate == CR) {
                sb.replace(offset, offset + 1, CR_ENCODED);
                replacementOffset += CR_ENCODED.length() - 1;
            }

            if (candidate == LF) {
                sb.replace(offset, offset + 1, LF_ENCODED);
                replacementOffset += LF_ENCODED.length() - 1;
            }

            if (candidate == PERCENT) {
                sb.replace(offset, offset + 1, PERCENT_ENCODED);
                replacementOffset += PERCENT_ENCODED.length() - 1;
            }
        }

        return sb.toString();
    }

    /**
     * Insures that a label (e.g. in {@code bag-info.txt}) does not contain a colon, line feed, or carriage return, and
     * that the label does not begin or end with whitespace.
     *
     * @param label the label
     * @see <a href="https://tools.ietf.org/html/rfc8493#section-2.2.2">RFC 8493 §2.2.2</a>
     */
    static void validateLabel(String label) {
        // insure label does not contain invalid characters:
        //   colon, CR, LF, or CRLF
        // insure label does not begin or end with whitespace (we check tab and space)
        if (label.charAt(0) == SPACE || label.charAt(0) == TAB ||
            label.charAt(label.length() - 1) == SPACE || label.charAt(label.length() - 1) == TAB) {
            throw new RuntimeException("Label must not start or end with whitespace.");
        }

        label.chars()
             .filter(candidate -> candidate == COLON || candidate == LF || candidate == CR)
             .findAny()
             .ifPresent(illegalChar -> {
                 throw new RuntimeException("Illegal character present in label string '" + label + "'");
             });
    }
}
