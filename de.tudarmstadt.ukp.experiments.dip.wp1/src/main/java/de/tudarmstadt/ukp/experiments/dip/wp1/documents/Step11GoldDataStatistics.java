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
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * @author Ivan Habernal
 */
public class Step11GoldDataStatistics
{

    /**
     * (1) Plain text with 4 columns: (1) the rank of the document in the list
     * (2) average agreement rate over queries (3) standard deviation of
     * agreement rate over queries. (4) average length of the document in the
     * rank.
     */
    public static void statistics1(File inputDir, File outputDir)
            throws Exception
    {
        SortedMap<Integer, DescriptiveStatistics> mapDocumentRankObservedAgreement = new TreeMap<>();
        SortedMap<Integer, DescriptiveStatistics> mapDocumentRankDocLength = new TreeMap<>();

        // iterate over query containers
        for (File f : FileUtils.listFiles(inputDir, new String[] { "xml" }, false)) {
            QueryResultContainer queryResultContainer = QueryResultContainer
                    .fromXML(FileUtils.readFileToString(f, "utf-8"));

            for (QueryResultContainer.SingleRankedResult rankedResult : queryResultContainer.rankedResults) {
                // add new entries
                if (!mapDocumentRankObservedAgreement.containsKey(rankedResult.rank)) {
                    mapDocumentRankObservedAgreement
                            .put(rankedResult.rank, new DescriptiveStatistics());
                }
                if (!mapDocumentRankDocLength.containsKey(rankedResult.rank)) {
                    mapDocumentRankDocLength.put(rankedResult.rank, new DescriptiveStatistics());
                }

                Double observedAgreement = rankedResult.observedAgreement;

                if (observedAgreement == null) {
                    System.err.println("Observed agreement is null; " + f.getName() + ", "
                            + rankedResult.clueWebID);
                }
                else {
                    // update value
                    mapDocumentRankObservedAgreement.get(rankedResult.rank)
                            .addValue(observedAgreement);
                    mapDocumentRankDocLength.get(rankedResult.rank)
                            .addValue(rankedResult.plainText.length());
                }
            }
        }

        PrintWriter pw = new PrintWriter(new FileWriter(new File(outputDir, "stats1.csv")));
        for (Map.Entry<Integer, DescriptiveStatistics> entry : mapDocumentRankObservedAgreement
                .entrySet()) {
            pw.printf(Locale.ENGLISH, "%d\t%.4f\t%.4f\t%.4f\t%.4f%n",
                    entry.getKey(), entry.getValue().getMean(),
                    entry.getValue().getStandardDeviation(),
                    mapDocumentRankDocLength.get(entry.getKey()).getMean(),
                    mapDocumentRankDocLength.get(entry.getKey()).getStandardDeviation()
            );
        }
        pw.close();
    }

    /**
     * (2) which documents were eventually judged as relevant ones.
     * So basically a plain text file with 3 columns:
     * query_id, document_id, binary_relevance
     * This will allow us to judge the document lists and give some performance, at least of
     * document level. This will allow us to identify "hard" and "easy" queries and having an agreement
     * rate per query we can see whether the two correlate. Then we potentially can "blame" the harder
     * queries for low agreement.
     */
    public static void statistics2(File inputDir, File outputDir)
            throws IOException
    {
        PrintWriter pw = new PrintWriter(new FileWriter(new File(outputDir, "stats2.csv")));
        pw.println(
                "qID\tclueWebID\tpartiallyRelevant\tfullyRelevant\trankedResult.rank\trankedResult.score");

        // iterate over query containers
        for (File f : FileUtils.listFiles(inputDir, new String[] { "xml" }, false)) {
            QueryResultContainer queryResultContainer = QueryResultContainer
                    .fromXML(FileUtils.readFileToString(f, "utf-8"));

            for (QueryResultContainer.SingleRankedResult rankedResult : queryResultContainer.rankedResults) {
                List<QueryResultContainer.SingleSentenceRelevanceVote> goldEstimatedLabels = rankedResult.goldEstimatedLabels;
                if (goldEstimatedLabels != null && !goldEstimatedLabels.isEmpty()) {
                    boolean fullyRelevant = true;
                    boolean partiallyRelevant = false;

                    // all must be relevant
                    for (QueryResultContainer.SingleSentenceRelevanceVote s : goldEstimatedLabels) {
                        fullyRelevant &= Boolean.valueOf(s.relevant);
                        partiallyRelevant |= Boolean.valueOf(s.relevant);
                    }

                    pw.printf(Locale.ENGLISH, "%s\t%s\t%s\t%s\t%d\t%.8f%n",
                            queryResultContainer.qID,
                            rankedResult.clueWebID,
                            partiallyRelevant,
                            fullyRelevant,
                            rankedResult.rank,
                            rankedResult.score
                    );
                }
            }
        }
        pw.close();
    }

    public static void statistics3(File inputDir, File outputDir)
            throws IOException
    {
        PrintWriter pw = new PrintWriter(new FileWriter(new File(outputDir, "stats3.csv")));
        pw.println(
                "qID\tagreementMean\tagreementStdDev\tqueryText");

        // iterate over query containers
        for (File f : FileUtils.listFiles(inputDir, new String[] { "xml" }, false)) {
            QueryResultContainer queryResultContainer = QueryResultContainer
                    .fromXML(FileUtils.readFileToString(f, "utf-8"));

            DescriptiveStatistics statistics = new DescriptiveStatistics();

            for (QueryResultContainer.SingleRankedResult rankedResult : queryResultContainer.rankedResults) {
                Double observedAgreement = rankedResult.observedAgreement;

                if (observedAgreement != null) {
                    statistics.addValue(observedAgreement);
                }
            }

            pw.printf(Locale.ENGLISH, "%s\t%.3f\t%.3f\t%s%n",
                    queryResultContainer.qID, statistics.getMean(),
                    statistics.getStandardDeviation(),
                    queryResultContainer.query
            );
        }

        pw.close();
    }

    /**
     * The following (full) data:
     * query docID rank agreement score relevance length
     * (7 columns)
     * The above detailed data about all annotated documents will allow me to
     * calculate different aggregations, such as average agreement at top 20
     * documents and etc...
     * By "relevance" is meant partial relevance.
     */
    public static void statistics4(File inputDir, File outputDir)
            throws IOException
    {
        PrintWriter pw = new PrintWriter(new FileWriter(new File(outputDir, "stats4.csv")));
        pw.println("query\tdocID\trank\tagreement\tscore\trelevance\tlength");

        // iterate over query containers
        for (File f : FileUtils.listFiles(inputDir, new String[] { "xml" }, false)) {
            QueryResultContainer queryResultContainer = QueryResultContainer
                    .fromXML(FileUtils.readFileToString(f, "utf-8"));
            System.out.println("Processing " + f);

            for (QueryResultContainer.SingleRankedResult rankedResult : queryResultContainer.rankedResults) {
                double agreement =
                        rankedResult.observedAgreement != null ? rankedResult.observedAgreement : 0;

                boolean isRelevant = false;

                List<QueryResultContainer.SingleSentenceRelevanceVote> goldEstimatedLabels = rankedResult.goldEstimatedLabels;
                // all must be relevant
                if (goldEstimatedLabels != null) {
                    for (QueryResultContainer.SingleSentenceRelevanceVote s : goldEstimatedLabels) {
                        isRelevant |= Boolean.valueOf(s.relevant);
                    }
                }

                pw.printf(Locale.ENGLISH, "%s\t%s\t%d\t%.3f\t%.8f\t%b\t%d%n",
                        queryResultContainer.qID,
                        rankedResult.clueWebID,
                        rankedResult.rank,
                        agreement,
                        rankedResult.score,
                        isRelevant,
                        rankedResult.plainText != null ? rankedResult.plainText.length() : 0
                );
            }
        }

        pw.close();

    }

    /**
     * Other statistics: we want to report how many documents and sentences
     * were judged and how many were found relevant among those.
     * Having the total + standard deviation over queries is enough.
     */
    public static void statistics5(File inputDir, File outputDir)
            throws IOException
    {
        PrintWriter pw = new PrintWriter(new FileWriter(new File(outputDir, "stats5.csv")));

        SortedMap<String, DescriptiveStatistics> result = new TreeMap<>();
        result.put("annotatedDocumentsPerQuery", new DescriptiveStatistics());
        result.put("annotatedSentencesPerQuery", new DescriptiveStatistics());
        result.put("relevantSentencesPerQuery", new DescriptiveStatistics());

        // print header
        for (String mapKey : result.keySet()) {
            pw.printf(Locale.ENGLISH, "%s\t%sStdDev\t", mapKey, mapKey);
        }
        pw.println();

        // iterate over query containers
        for (File f : FileUtils.listFiles(inputDir, new String[] { "xml" }, false)) {
            QueryResultContainer queryResultContainer = QueryResultContainer
                    .fromXML(FileUtils.readFileToString(f, "utf-8"));
            System.out.println("Processing " + f);

            int annotatedDocuments = 0;
            int relevantSentences = 0;
            int totalSentences = 0;

            for (QueryResultContainer.SingleRankedResult rankedResult : queryResultContainer.rankedResults) {

                if (rankedResult.plainText != null && !rankedResult.plainText.isEmpty()) {
                    annotatedDocuments++;

                    if (rankedResult.goldEstimatedLabels != null) {
                        for (QueryResultContainer.SingleSentenceRelevanceVote sentenceRelevanceVote : rankedResult.goldEstimatedLabels) {
                            totalSentences++;

                            if (Boolean.valueOf(sentenceRelevanceVote.relevant)) {
                                relevantSentences++;
                            }
                        }
                    }
                }
            }

            result.get("annotatedDocumentsPerQuery").addValue(annotatedDocuments);
            result.get("annotatedSentencesPerQuery").addValue(totalSentences);
            result.get("relevantSentencesPerQuery").addValue(relevantSentences);
        }

        // print results
        // print header
        for (String mapKey : result.keySet()) {
            pw.printf(Locale.ENGLISH, "%.3f\t%.3f\t", result.get(mapKey).getMean(),
                    result.get(mapKey).getStandardDeviation());
        }

        pw.close();

    }

    /**
     * Relevant sentences per document (per query)
     */
    public static void statistics6(File inputDir, File outputDir)
            throws IOException
    {
        PrintWriter pw = new PrintWriter(new FileWriter(new File(outputDir, "stats6.csv")));

        SortedMap<String, DescriptiveStatistics> result = new TreeMap<>();
        result.put("relevantSentencesDocumentPercent", new DescriptiveStatistics());

        // print header
        for (String mapKey : result.keySet()) {
            pw.printf(Locale.ENGLISH, "%s\t%sStdDev\t", mapKey, mapKey);
        }
        pw.println();

        // iterate over query containers
        for (File f : FileUtils.listFiles(inputDir, new String[] { "xml" }, false)) {
            QueryResultContainer queryResultContainer = QueryResultContainer
                    .fromXML(FileUtils.readFileToString(f, "utf-8"));
            System.out.println("Processing " + f);



            for (QueryResultContainer.SingleRankedResult rankedResult : queryResultContainer.rankedResults) {

                if (rankedResult.plainText != null && !rankedResult.plainText.isEmpty()) {

                    int relevantSentences = 0;
                    int totalSentences = 0;

                    if (rankedResult.goldEstimatedLabels != null) {
                        for (QueryResultContainer.SingleSentenceRelevanceVote sentenceRelevanceVote : rankedResult.goldEstimatedLabels) {
                            totalSentences++;

                            if (Boolean.valueOf(sentenceRelevanceVote.relevant)) {
                                relevantSentences++;
                            }
                        }

                        // percent relevant

                        result.get("relevantSentencesDocumentPercent")
                                .addValue((double) relevantSentences / (double) totalSentences);
                    }
                }
            }
        }

        // print results
        // print header
        for (String mapKey : result.keySet()) {
            pw.printf(Locale.ENGLISH, "%.3f\t%.3f\t", result.get(mapKey).getMean(),
                    result.get(mapKey).getStandardDeviation());
        }

        pw.close();

    }

    public static void main(String[] args)
            throws Exception
    {
        // input dir - list of xml query containers
        // /home/user-ukp/research/data/dip/wp1-documents/step3-filled-raw-html
        File inputDir = new File(args[0]);

        // output dir
        File outputDir = new File(args[1]);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        statistics1(inputDir, outputDir);
        statistics2(inputDir, outputDir);
        statistics3(inputDir, outputDir);
        statistics4(inputDir, outputDir);
        statistics5(inputDir, outputDir);
        statistics6(inputDir, outputDir);
    }

}
