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
package edu.jhu.library.pass.deposit.provider.j10p;

import edu.jhu.library.pass.deposit.provider.shared.dspace.DspaceMetsAssembler;
import edu.jhu.library.pass.deposit.provider.shared.dspace.DspaceMetsPackageVerifier;
import org.dataconservancy.pass.deposit.assembler.PackageOptions.Archive;
import org.dataconservancy.pass.deposit.assembler.PackageOptions.Checksum;
import org.dataconservancy.pass.deposit.assembler.PackageOptions.Compression;
import org.dataconservancy.pass.deposit.assembler.PackageOptions.Spec;
import org.dataconservancy.pass.deposit.assembler.shared.AbstractAssembler;
import org.dataconservancy.pass.deposit.assembler.shared.PackageVerifier;
import org.dataconservancy.pass.deposit.assembler.shared.ThreadedAssemblyIT;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonList;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class J10PMetsThreadedAssemblyIT extends ThreadedAssemblyIT {

    @Override
    protected AbstractAssembler assemblerUnderTest() {
        return new J10PDspaceMetsAssembler(mbf, rbf, DocumentBuilderFactory.newInstance());
    }

    @Override
    protected Map<String, Object> packageOptions() {
        return new HashMap<String, Object>() {
            {
                put(Spec.KEY, DspaceMetsAssembler.SPEC_DSPACE_METS);
                put(Archive.KEY, Archive.OPTS.ZIP);
                put(Compression.KEY, Compression.OPTS.ZIP);
                put(Checksum.KEY, singletonList(Checksum.OPTS.SHA256));
            }
        };
    }

    @Override
    protected PackageVerifier packageVerifier() {
        return new DspaceMetsPackageVerifier();
    }

}
