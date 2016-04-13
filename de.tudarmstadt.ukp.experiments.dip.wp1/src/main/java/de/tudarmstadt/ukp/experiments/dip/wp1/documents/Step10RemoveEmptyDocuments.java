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
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * There are some documents with zero length: we wan to remove them. Further, each query must
 * be cutoff at the same size.
 *
 * @author Ivan Habernal
 */
public class Step10RemoveEmptyDocuments
{

    public static void main(String[] args)
            throws IOException
    {
        // input dir - list of xml query containers
        File inputDir = new File(args[0]);

        // output dir
        File outputDir = new File(args[1]);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        boolean crop = args.length >= 3 && "crop".equals(args[2]);

        // first find the maximum of zero-sized documents
        int maxMissing = 7;

        /*
        // iterate over query containers
        for (File f : FileUtils.listFiles(inputDir, new String[] { "xml" }, false)) {
            QueryResultContainer queryResultContainer = QueryResultContainer
                    .fromXML(FileUtils.readFileToString(f, "utf-8"));

            // first find the maximum of zero-sized documents in a query
            int missingInQuery = 0;

            for (QueryResultContainer.SingleRankedResult rankedResults : queryResultContainer.rankedResults) {
                // boilerplate removal
                if (rankedResults.plainText == null || rankedResults.plainText.isEmpty()) {
                    missingInQuery++;
                }
            }

            maxMissing = Math.max(missingInQuery, maxMissing);
        }
        */

        System.out.println("Max zeroLengthDocuments in query: " + maxMissing);
        // max is 7 = we're cut-off at 93

        // iterate over query containers
        for (File f : FileUtils.listFiles(inputDir, new String[] { "xml" }, false)) {
            QueryResultContainer queryResultContainer = QueryResultContainer
                    .fromXML(FileUtils.readFileToString(f, "utf-8"));

            List<QueryResultContainer.SingleRankedResult> nonEmptyDocsList = new ArrayList<>();

            for (QueryResultContainer.SingleRankedResult rankedResults : queryResultContainer.rankedResults) {
                // collect non-empty documents
                if (rankedResults.plainText != null && !rankedResults.plainText.isEmpty()) {
                    nonEmptyDocsList.add(rankedResults);
                }
            }

            System.out.println("Non-empty docs coune: " + nonEmptyDocsList.size());

            if (crop) {
                // now cut at 93
                nonEmptyDocsList = nonEmptyDocsList.subList(0, (100 - maxMissing));
                System.out.println("After cropping: " + nonEmptyDocsList.size());
            }
            System.out.println("After cleaning: " + nonEmptyDocsList.size());

            queryResultContainer.rankedResults.clear();
            queryResultContainer.rankedResults.addAll(nonEmptyDocsList);

            // and save the query to output dir
            File outputFile = new File(outputDir, queryResultContainer.qID + ".xml");
            FileUtils.writeStringToFile(outputFile, queryResultContainer.toXML(), "utf-8");
            System.out.println("Finished " + outputFile);
        }

    }
}
