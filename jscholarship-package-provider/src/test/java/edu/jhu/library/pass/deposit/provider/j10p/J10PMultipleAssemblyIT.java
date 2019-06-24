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

import edu.jhu.library.pass.deposit.provider.shared.dspace.MultipleAssemblyDspaceMetsAssemblerIT;
import org.dataconservancy.pass.deposit.assembler.shared.AbstractAssembler;
import org.junit.BeforeClass;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class J10PMultipleAssemblyIT extends MultipleAssemblyDspaceMetsAssemblerIT {

    @BeforeClass
    public static void initAssembler() {
        underTest = new J10PDspaceMetsAssembler(metadataBuilderFactory(), resourceBuilderFactory(), DocumentBuilderFactory.newInstance());
    }

    @Override
    protected AbstractAssembler assemblerUnderTest() {
        return underTest;
    }


}
