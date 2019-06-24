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
import edu.jhu.library.pass.deposit.provider.shared.dspace.DspaceMetsPackageProviderFactory;
import org.dataconservancy.pass.deposit.assembler.shared.MetadataBuilderFactory;
import org.dataconservancy.pass.deposit.assembler.shared.ResourceBuilderFactory;
import org.springframework.stereotype.Component;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
@Component
public class J10PDspaceMetsAssembler extends DspaceMetsAssembler {

    private DocumentBuilderFactory dbf;

    public J10PDspaceMetsAssembler(MetadataBuilderFactory mbf,
                                   ResourceBuilderFactory rbf,
                                   DocumentBuilderFactory dbf) {
        super(mbf, rbf);
        this.dbf = dbf;
    }

    @Override
    protected DspaceMetsPackageProviderFactory getPackageProviderFactory() {
        return new DspaceMetsPackageProviderFactory(new J10PMetadataDomWriterFactory(dbf));
    }
}
