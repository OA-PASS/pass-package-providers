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

import edu.jhu.library.pass.deposit.provider.shared.dspace.DspaceMetadataDomWriter;
import edu.jhu.library.pass.deposit.provider.shared.dspace.DspaceMetadataDomWriterFactory;
import org.dataconservancy.pass.client.PassClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
@Component
public class DashMetadataDomWriterFactory implements DspaceMetadataDomWriterFactory {

    private DocumentBuilderFactory dbf;

    private PassClient passClient;

    @Autowired
    public DashMetadataDomWriterFactory(DocumentBuilderFactory dbf, PassClient passClient) {
        this.dbf = dbf;
        this.passClient = passClient;
    }

    @Override
    public DspaceMetadataDomWriter newInstance() {
        return new DashMetadataDomWriter(dbf, passClient);
    }

}
