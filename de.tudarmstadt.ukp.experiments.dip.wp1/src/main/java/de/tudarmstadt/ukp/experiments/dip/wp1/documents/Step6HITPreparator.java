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

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.experiments.dip.wp1.data.QueryResultContainer;
import org.apache.commons.io.FileUtils;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import sun.misc.BASE64Decoder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

/**
 * Loads containers with pre-annotated sentences and creates HITs for annotations
 *
 * @author Maria Sukhareva
 */
public class Step6HITPreparator
{

    // FIXME comment
    private static final int TOP_RESULTS_PER_GROUP = 93;

    static class generators
    {

        String query;
        String query_id;
        List<Sentence> sentences;
        List<String> relevantInformationExamples;
        List<String> irrelevantInformationExamples;
        int rank;

        generators(String query,
                List<String> relevantInformationExamples,
                List<String> irrelevantInformationExamples,
                List<Sentence> sentences,
                String query_id,
                int rank
        )
        {
            this.query = query;
            this.sentences = sentences;
            this.query_id = query_id;
            this.relevantInformationExamples = relevantInformationExamples;
            this.irrelevantInformationExamples = irrelevantInformationExamples;
            this.rank = rank;
        }

        static class Sentence
        {
            Sentence(String sentence, String sentence_id)
            {
                this.sentence = sentence;
                this.sentence_id = sentence_id;
            }

            String sentence;
            String sentence_id;
        }

    }

    public static void main(String[] args)
            throws Exception
    {
        // input dir - list of xml query containers
        // step5-linguistic-annotation/
        System.err.println("Starting step 6 HIT Preparation");

        File inputDir = new File(args[0]);

        // output dir
        File outputDir = new File(args[1]);
        if (outputDir.exists()) {
            outputDir.delete();
        }
        outputDir.mkdir();

        List<String> queries = new ArrayList<>();

        // iterate over query containers
        int countClueWeb = 0;
        int countSentence = 0;
        for (File f : FileUtils.listFiles(inputDir, new String[] { "xml" }, false)) {
            QueryResultContainer queryResultContainer = QueryResultContainer
                    .fromXML(FileUtils.readFileToString(f, "utf-8"));
            if (queries.contains(f.getName()) || queries.size() == 0) {
                // groups contain only non-empty documents
                Map<Integer, List<QueryResultContainer.SingleRankedResult>> groups = new HashMap<>();

                // split to groups according to number of sentences
                for (QueryResultContainer.SingleRankedResult rankedResult : queryResultContainer.rankedResults) {
                    if (rankedResult.originalXmi != null) {
                        byte[] bytes = new BASE64Decoder().decodeBuffer(
                                new ByteArrayInputStream(rankedResult.originalXmi.getBytes()));
                        JCas jCas = JCasFactory.createJCas();
                        XmiCasDeserializer
                                .deserialize(new ByteArrayInputStream(bytes), jCas.getCas());

                        Collection<Sentence> sentences = JCasUtil.select(jCas, Sentence.class);

                        int groupId = sentences.size() / 40;
                        if (rankedResult.originalXmi == null) {
                            System.err.println("Empty document: " + rankedResult.clueWebID);
                        }
                        else {
                            if (!groups.containsKey(groupId)) {
                                groups.put(groupId,
                                        new ArrayList<>());

                            }
                        }
                        //handle it
                        groups.get(groupId).add(rankedResult);
                        countClueWeb++;
                    }
                }

                for (Map.Entry<Integer, List<QueryResultContainer.SingleRankedResult>> entry : groups
                        .entrySet()) {
                    Integer groupId = entry.getKey();
                    List<QueryResultContainer.SingleRankedResult> rankedResults = entry.getValue();

                    // make sure the results are sorted
                    // DEBUG
                    //                for (QueryResultContainer.SingleRankedResult r : rankedResults) {
                    //                    System.out.print(r.rank + "\t");
                    //                }

                    Collections.sort(rankedResults, (o1, o2) -> o1.rank.compareTo(o2.rank));

                    // iterate over results for one query and group
                    for (int i = 0; i < rankedResults.size() && i < TOP_RESULTS_PER_GROUP; i++) {
                        QueryResultContainer.SingleRankedResult rankedResult = rankedResults.get(i);

                        QueryResultContainer.SingleRankedResult r = rankedResults.get(i);
                        int rank = r.rank;
                        MustacheFactory mf = new DefaultMustacheFactory();
                        Mustache mustache = mf.compile("template/template.html");
                        String queryId = queryResultContainer.qID;
                        String query = queryResultContainer.query;
                        // make the first letter uppercase
                        query = query.substring(0, 1).toUpperCase() + query.substring(1);

                        List<String> relevantInformationExamples = queryResultContainer.relevantInformationExamples;
                        List<String> irrelevantInformationExamples = queryResultContainer.irrelevantInformationExamples;
                        byte[] bytes = new BASE64Decoder().decodeBuffer(
                                new ByteArrayInputStream(rankedResult.originalXmi.getBytes()));

                        JCas jCas = JCasFactory.createJCas();
                        XmiCasDeserializer
                                .deserialize(new ByteArrayInputStream(bytes), jCas.getCas());

                        List<generators.Sentence> sentences = new ArrayList<>();
                        List<Integer> paragraphs = new ArrayList<>();
                        paragraphs.add(0);

                        for (WebParagraph webParagraph : JCasUtil
                                .select(jCas, WebParagraph.class)) {
                            for (Sentence s : JCasUtil
                                    .selectCovered(Sentence.class, webParagraph)) {

                                String sentenceBegin = String.valueOf(s.getBegin());
                                generators.Sentence sentence = new generators.Sentence(
                                        s.getCoveredText(), sentenceBegin);
                                sentences.add(sentence);
                                countSentence++;
                            }
                            int SentenceID = paragraphs.get(paragraphs.size() - 1);
                            if (sentences.size() > 120)
                                while (SentenceID < sentences.size()) {
                                    if (!paragraphs.contains(SentenceID))
                                        paragraphs.add(SentenceID);
                                    SentenceID = SentenceID + 120;
                                }
                            paragraphs.add(sentences.size());

                        }
                        System.err.println("Output dir: " + outputDir);
                        int startID = 0;
                        int endID;

                        for (int j = 0; j < paragraphs.size(); j++) {

                            endID = paragraphs.get(j);
                            int sentLength = endID - startID;
                            if (sentLength > 120 || j == paragraphs.size() - 1) {
                                if (sentLength > 120) {

                                    endID = paragraphs.get(j - 1);
                                    j--;
                                }
                                sentLength = endID - startID;
                                if (sentLength <= 40)
                                    groupId = 40;
                                else if (sentLength <= 80 && sentLength > 40)
                                    groupId = 80;
                                else if (sentLength > 80)
                                    groupId = 120;

                                File folder = new File(outputDir + "/" + groupId);
                                if (!folder.exists()) {
                                    System.err.println(
                                            "creating directory: " + outputDir + "/" + groupId);
                                    boolean result = false;

                                    try {
                                        folder.mkdir();
                                        result = true;
                                    }
                                    catch (SecurityException se) {
                                        //handle it
                                    }
                                    if (result) {
                                        System.out.println("DIR created");
                                    }
                                }

                                String newHtmlFile =
                                        folder.getAbsolutePath() + "/" + f.getName() + "_"
                                                + rankedResult.clueWebID + "_" + sentLength
                                                + ".html";
                                System.err.println("Printing a file: " + newHtmlFile);
                                File newHTML = new File(newHtmlFile);
                                int t = 0;
                                while (newHTML.exists()) {
                                    newHTML = new File(
                                            folder.getAbsolutePath() + "/" + f.getName() + "_"
                                                    + rankedResult.clueWebID + "_"
                                                    + sentLength + "." + t + ".html");
                                    t++;
                                }
                                mustache.execute(new PrintWriter(new FileWriter(newHTML)),
                                        new generators(query, relevantInformationExamples,
                                                irrelevantInformationExamples,
                                                sentences.subList(startID, endID), queryId, rank))
                                        .flush();
                                startID = endID;
                            }
                        }
                    }
                }

            }
        }
        System.out.println(
                "Printed " + countClueWeb + " documents with " + countSentence + " sentences");
    }

}
