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
import java.util.HashMap;
import java.util.Map;

/**
 * Takes the output of IR (from Technion) and adds the retrieved results IDs, rank, and score
 * to the query containers.
 *
 * @author Ivan Habernal
 */
public class Step2FillWithRetrievedResults
{
    public static void main(String[] args)
            throws IOException
    {
        // input dir - list of xml query containers
        File inputDir = new File(args[0]);

        // retrieved results from Technion
        // ltr-50queries-100docs.txt
        File ltr = new File(args[1]);

        // output dir
        File outputDir = new File(args[2]);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // load the query containers first (into map: id + container)
        Map<String, QueryResultContainer> queryResults = new HashMap<>();
        for (File f : FileUtils.listFiles(inputDir, new String[] { "xml" }, false)) {
            System.out.println(f);
            QueryResultContainer queryResultContainer = QueryResultContainer
                    .fromXML(FileUtils.readFileToString(f, "utf-8"));
            queryResults.put(queryResultContainer.qID, queryResultContainer);
        }

        // iterate over IR results
        for (String line : FileUtils.readLines(ltr)) {
            String[] split = line.split("\\s+");
            Integer origQueryId = Integer.valueOf(split[0]);
            String clueWebID = split[2];
            Integer rank = Integer.valueOf(split[3]);
            double score = Double.valueOf(split[4]);
            String additionalInfo = split[5];

            // get the container for this result
            QueryResultContainer container = queryResults.get(origQueryId.toString());

            if (container != null) {
                // add new result
                QueryResultContainer.SingleRankedResult result = new QueryResultContainer.SingleRankedResult();
                result.clueWebID = clueWebID;
                result.rank = rank;
                result.score = score;
                result.additionalInfo = additionalInfo;

                if (container.rankedResults == null) {
                    container.rankedResults = new ArrayList<>();
                }
                container.rankedResults.add(result);
            }
        }

        // save all containers to the output dir
        for (QueryResultContainer queryResultContainer : queryResults.values()) {
            File outputFile = new File(outputDir, queryResultContainer.qID + ".xml");
            FileUtils.writeStringToFile(outputFile, queryResultContainer.toXML(), "utf-8");
            System.out.println("Finished " + outputFile);
        }
    }
}
