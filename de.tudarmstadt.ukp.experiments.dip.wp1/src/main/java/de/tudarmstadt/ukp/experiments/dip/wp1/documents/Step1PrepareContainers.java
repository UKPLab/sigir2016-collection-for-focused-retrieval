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
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Prepares the data structure for holding queries, information about relevant segments, and
 * retrieved results, etc.
 *
 * @author Ivan Habernal
 */
public class Step1PrepareContainers
{
    public static void main(String[] args)
            throws IOException
    {
        // queries with narratives in CSV
        File queries = new File(args[0]);
        File relevantInformationExamplesFile = new File(args[1]);

        Map<Integer, Map<Integer, List<String>>> relevantInformationMap = parseRelevantInformationFile(
                relevantInformationExamplesFile);

        // output dir
        File outputDir = new File(args[2]);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // iterate over queries
        CSVParser csvParser = CSVParser.parse(queries, Charset.forName("utf-8"), CSVFormat.DEFAULT);
        for (CSVRecord record : csvParser) {
            // create new container, fill, and store
            QueryResultContainer container = new QueryResultContainer();
            container.qID = record.get(0);
            container.query = record.get(1);

            // Fill some dummy text first
            container.relevantInformationExamples.addAll(Collections.singletonList(
                    "ERROR. Information missing."
            ));
            container.irrelevantInformationExamples.addAll(Collections.singletonList(
                    "ERROR. Information missing."
            ));

            // and now fill it with existing information if available
            Integer queryID = Integer.valueOf(container.qID);
            if (relevantInformationMap.containsKey(queryID)) {
                if (relevantInformationMap.get(queryID).containsKey(0)) {
                    container.irrelevantInformationExamples = new ArrayList<>(
                            relevantInformationMap.get(queryID).get(0));
                }

                if (relevantInformationMap.get(queryID).containsKey(1)) {
                    container.relevantInformationExamples = new ArrayList<>(
                            relevantInformationMap.get(queryID).get(1));
                }
            }

            File outputFile = new File(outputDir, container.qID + ".xml");
            FileUtils.writeStringToFile(outputFile, container.toXML());
            System.out.println("Finished " + outputFile);
        }
    }

    /**
     * Parses the file with relevance/non-relevance information to each query, i.e.
     * <pre>
     * QID: 1021
     * Query: School dealing with bullying.
     * 1 Physical harm that occurs in an educational setting, performed intentionally by kids to other kids.
     * 1 Sexual harm that occurs in an educational setting, performed intentionally by kids to other kids.
     * 0 Bullying outside school.
     * 0 Bullying by adults.
     *
     * QID: 1022
     * Query: Signs of bullied kids.
     * 1 How to identify kids suffering from bulling by other kids.
     * [...]
     * </pre>
     *
     * @param file file
     * @return map (queryID, (0 = non-relevant list, 1 = relevant list))
     * @throws IOException exception
     */
    public static Map<Integer, Map<Integer, List<String>>> parseRelevantInformationFile(File file)
            throws IOException
    {
        LineIterator lineIterator = FileUtils.lineIterator(file, "utf-8");

        Map<Integer, Map<Integer, List<String>>> result = new HashMap<>();

        int currentQueryId = Integer.MIN_VALUE;

        while (lineIterator.hasNext()) {
            String line = lineIterator.next().trim();

            if (line.isEmpty()) {
                currentQueryId = Integer.MIN_VALUE;
            }

            // new query id
            if (line.startsWith("QID: ")) {
                currentQueryId = Integer.valueOf(line.split("\\s", 2)[1]);

                // add to the result
                if (!result.containsKey(currentQueryId)) {
                    result.put(currentQueryId, new HashMap<>());
                    // and two empty lists
                    result.get(currentQueryId).put(0, new ArrayList<>());
                    result.get(currentQueryId).put(1, new ArrayList<>());
                }
            }

            if (line.startsWith("0 ")) {
                String nonRelevantInformation = line.split("\\s", 2)[1];

                if (currentQueryId == Integer.MIN_VALUE) {
                    throw new IOException("Input file has a wrong format, no QUI provided");
                }

                // add to the result
                result.get(currentQueryId).get(0).add(nonRelevantInformation);
            }

            if (line.startsWith("1 ")) {
                String relevantInformation = line.split("\\s", 2)[1];

                if (currentQueryId == Integer.MIN_VALUE) {
                    throw new IOException("Input file has a wrong format, no QUI provided");
                }

                // add to the result
                result.get(currentQueryId).get(1).add(relevantInformation);
            }
        }

        // debug
        for (Map.Entry<Integer, Map<Integer, List<String>>> entry : result.entrySet()) {
            System.out.println("QID: " + entry.getKey());
            for (Map.Entry<Integer, List<String>> innerEntry : entry.getValue().entrySet()) {
                for (String info : innerEntry.getValue()) {
                    System.out.printf("%d %s%n", innerEntry.getKey(), info);
                }
            }
            System.out.println();
        }

        return result;
    }
}
