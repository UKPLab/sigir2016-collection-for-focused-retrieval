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

package de.tudarmstadt.ukp.experiments.dip.wp1.documents;

import de.tudarmstadt.ukp.experiments.dip.wp1.data.QueryResultContainer;
import de.tudarmstadt.ukp.experiments.dip.wp1.documents.helpers.boilerplateremoval.BoilerPlateRemoval;
import de.tudarmstadt.ukp.experiments.dip.wp1.documents.helpers.boilerplateremoval.impl.JusTextBoilerplateRemoval;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Boilerplate removal implementation of JusText (Pomikalek, 2011) developed as part of C4Corpus
 * AIPHES (Omnia Zayed). Process each retrieved results for each container.
 * <p/>
 * It keeps a minimal html markup (headers, paragraphs) in the text
 * <p/>
 * The third cmd parameter sets whether the original HTML should retain or be deleted
 *
 * @author Ivan Habernal
 */
public class Step4BoilerPlateRemoval
{
    public static void main(String[] args)
            throws IOException
    {
        // input dir - list of xml query containers
        // step3-filled-raw-html
        File inputDir = new File(args[0]);

        // output dir
        File outputDir = new File(args[1]);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // keep original html? (true == default)
        boolean keepOriginalHTML = !(args.length > 2 && "false".equals(args[2]));

        System.out.println(keepOriginalHTML);

        BoilerPlateRemoval boilerPlateRemoval = new JusTextBoilerplateRemoval();

        // iterate over query containers
        for (File f : FileUtils.listFiles(inputDir, new String[] { "xml" }, false)) {
            QueryResultContainer queryResultContainer = QueryResultContainer
                    .fromXML(FileUtils.readFileToString(f, "utf-8"));

            for (QueryResultContainer.SingleRankedResult rankedResults : queryResultContainer.rankedResults) {
                // boilerplate removal

                // there are some empty (corrupted) documents in ClueWeb, namely 0308wb-83.warc.gz
                if (rankedResults.originalHtml != null) {

                rankedResults.plainText = boilerPlateRemoval
                        .getMinimalHtml(rankedResults.originalHtml, null);
                }

                if (!keepOriginalHTML) {
                    rankedResults.originalHtml = null;
                }
            }

            // and save the query to output dir
            File outputFile = new File(outputDir, queryResultContainer.qID + ".xml");
            FileUtils.writeStringToFile(outputFile, queryResultContainer.toXML(), "utf-8");
            System.out.println("Finished " + outputFile);
        }

    }
}
