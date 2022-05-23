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

import java.io.IOException;
import java.io.InputStream;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import org.apache.commons.io.IOUtils;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class HandlebarsParameterizer implements Parameterizer {

    private Handlebars handlebars;

    public HandlebarsParameterizer(Handlebars handlebars) {
        this.handlebars = handlebars;
    }

    public String parameterize(InputStream template, BagModel model) {
        String parameterizedTemplate = null;
        try {
            String templateString = IOUtils.toString(template, "UTF-8");
            Template t = handlebars.compileInline(templateString);
            parameterizedTemplate = t.apply(model);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return parameterizedTemplate;
    }

}
