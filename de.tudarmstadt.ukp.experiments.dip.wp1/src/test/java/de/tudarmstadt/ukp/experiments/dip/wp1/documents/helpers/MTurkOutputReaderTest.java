/*
 * Copyright 2016
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.tudarmstadt.ukp.experiments.dip.wp1.documents.helpers;

import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Ivan Habernal
 */
public class MTurkOutputReaderTest
{

    @Test
    public void testMalformedInput()
            throws Exception
    {

        URL resource = this.getClass().getClassLoader()
                .getResource("weird-mturk-output-csv.result");
        assertNotNull(resource);
        File file = new File(resource.getFile());
        assertTrue(file.exists());

        MTurkOutputReader outputReader = new MTurkOutputReader(
                new HashSet<>(Arrays.asList("annotation", "workerid")),
                file);
    }
}