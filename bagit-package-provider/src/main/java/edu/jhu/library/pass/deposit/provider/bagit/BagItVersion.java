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

public enum BagItVersion {

    BAGIT_1_0("1.0", "1", "0", "");

    private static final String VERSION_STRING = "%s.%s";

    private static final String VERSION_STRING_WITH_DOT = "%s.%s.%s";

    private String versionString;

    private String major;

    private String minor;

    private String dot;

    BagItVersion(String versionString, String major, String minor, String dot) {
        this.major = major;
        this.minor = minor;
        this.dot = dot;
        this.versionString = toString();
    }

    public String getVersionString() {
        return versionString;
    }

    public String toString() {
        if (null == dot || dot.trim().length() == 0) {
            return String.format(VERSION_STRING, major, minor);
        }

        return String.format(VERSION_STRING_WITH_DOT, major, minor, dot);
    }

}
