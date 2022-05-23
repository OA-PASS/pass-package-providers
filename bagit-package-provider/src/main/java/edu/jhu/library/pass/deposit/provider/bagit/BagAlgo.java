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
 * Normalized constants for BagIt checksum algorithms.
 *
 * https://www.rfc-editor.org/rfc/rfc8493.html#section-2.4
 */
public enum BagAlgo {

    MD5(new String[] {"md5", "MD5"}, "md5"),
    SHA1(new String[] {"sha-1", "sha1", "SHA1", "SHA-1"}, "sha1"),
    SHA256(new String[] {"sha-256", "sha256", "SHA256", "SHA-256"}, "sha256"),
    SHA512(new String[] {"sha-512", "sha512", "SHA512", "SHA-512"}, "sha512");

    private String[] variants;

    private String algo;

    BagAlgo(String[] variants, String algo) {
        this.variants = variants;
        this.algo = algo;
    }

    public String[] getVariants() {
        return variants;
    }

    public String getAlgo() {
        return algo;
    }
}
