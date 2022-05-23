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

/**
 * Required metadata elements for inclusion in the Bag Declaration {@code bagit.txt}.
 * Reserved, but optional, metadata elements for inclusion in the Bag Metadata {@code bag-info.txt}.
 *
 * https://www.rfc-editor.org/rfc/rfc8493.html#section-2.1.1
 * https://www.rfc-editor.org/rfc/rfc8493.html#section-2.2.2
 */
public class BagMetadata {

    private BagMetadata() {
    }

    /**
     * Identifies the BagIt major (M) and minor (N) version numbers
     */
    public static final String BAGIT_VERSION = "BagIt-Version";

    /**
     * Identifies the character set encoding used by the remaining tag files.
     */
    public static final String TAG_FILE_ENCODING = "Tag-File-Character-Encoding";

    /**
     * Organization transferring the content.
     */
    public static final String SOURCE_ORGANIZATION = "Source-Organization";

    /**
     * Mailing address of the source organization.
     */
    public static final String ORGANIZATION_ADDRESS = "Organization-Address";

    /**
     * Person at the source organization who is responsible for the content transfer.
     */
    public static final String CONTACT_NAME = "Contact-Name";

    /**
     * International format telephone number of person or position responsible.
     */
    public static final String CONTACT_PHONE = "Contact-Phone";

    /**
     * Fully qualified email address of person or position responsible.
     */
    public static final String CONTACT_EMAIL = "Contact-Email";

    /**
     * A brief explanation of the contents and provenance.
     */
    public static final String EXTERNAL_DESCRIPTION = "External-Description";

    /**
     * Date (YYYY-MM-DD) that the content was prepared for transfer.  This metadata element SHOULD NOT be repeated.
     */
    public static final String BAGGING_DATE = "Bagging-Date";

    /**
     * A sender-supplied identifier for the bag.
     */
    public static final String EXTERNAL_IDENTIFIER = "External-Identifier";

    /**
     * The size or approximate size of the bag being transferred, followed by an abbreviation such as MB (megabytes), GB
     * (gigabytes), or TB (terabytes): for example, 42600 MB, 42.6 GB, or .043 TB.  Compared to Payload-Oxum (described
     * next), Bag-Size is intended for human consumption.  This metadata element SHOULD NOT be repeated.
     */
    public static final String BAG_SIZE = "Bag-Size";

    /**
     * The "octetstream sum" of the payload, which is intended for the purpose of quickly detecting incomplete bags
     * before performing checksum validation.  This is strictly an optimization, and implementations MUST perform the
     * standard checksum validation process before proclaiming a bag to be valid. This element MUST NOT be present more
     * than once and, if present, MUST be in the form "_OctetCount_._StreamCount_", where _OctetCount_ is the total
     * number of octets (8-bit bytes) across all payload file content and _StreamCount_ is the total number of payload
     * files.  This metadata element MUST NOT be repeated.
     */
    public static final String PAYLOAD_OXUM = "Payload-Oxum";

    /**
     * A sender-supplied identifier for the set, if any, of bags to which it logically belongs.  This identifier SHOULD
     * be unique across the sender's content, and if it is recognizable as belonging to a globally unique scheme, the
     * receiver SHOULD make an effort to honor the reference to it.  This metadata element SHOULD NOT be repeated.
     */
    public static final String BAG_GROUP_IDENTIFIER = "Bag-Group-Identifier";

    /**
     * Two numbers separated by "of", in particular, "N of T", where T is the total number of bags in a group of bags
     * and N is the ordinal number within the group.  If T is not known, specify it as "?" (question mark): for example,
     * 1 of 2, 4 of 4, 3 of ?, 89 of 145.  This metadata element SHOULD NOT be repeated.  If this metadata element is
     * present, it is RECOMMENDED to also include the Bag-Group-Identifier element.
     */
    public static final String BAG_COUNT = "Bag-Count";

    /**
     * An alternate sender-specific identifier for the content and/or bag.
     */
    public static final String INTERNAL_SENDER_IDENTIFIER = "Internal-Sender-Identifier";

    /**
     * A sender-local explanation of the contents and provenance.
     */
    public static final String INTERNAL_SENDER_DESCRIPTION = "Internal-Sender-Description";

    static final long ONE_KIBIBYTE = Math.round(Math.pow(2, 10));

    static final long ONE_MEBIBYTE = Math.round(Math.pow(2, 20));

    static final long ONE_GIBIBYTE = Math.round(Math.pow(2, 30));

    static final long ONE_TEBIBYTE = Math.round(Math.pow(2, 40));

}
