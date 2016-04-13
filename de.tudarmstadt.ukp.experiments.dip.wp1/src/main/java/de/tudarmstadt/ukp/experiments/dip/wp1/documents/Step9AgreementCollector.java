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

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.experiments.dip.wp1.data.QueryResultContainer;
import org.apache.commons.io.FileUtils;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.dkpro.statistics.agreement.InsufficientDataException;
import org.dkpro.statistics.agreement.coding.CodingAnnotationStudy;
import org.dkpro.statistics.agreement.coding.FleissKappaAgreement;
import sun.misc.BASE64Decoder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.*;

/**
 * Computes agreement statistics for collected data from HITs
 *
 * @author Ivan Habernal
 */
public class Step9AgreementCollector
{
    private static final int NUMBER_OF_TURKERS_PER_HIT = 5;

    @SuppressWarnings("unchecked")
    public static void computeObservedAgreement(File goldDataFolder, File outputDir)
            throws Exception
    {
        // iterate over query containers
        for (File f : FileUtils.listFiles(goldDataFolder, new String[] { "xml" }, false)) {
            QueryResultContainer queryResultContainer = QueryResultContainer
                    .fromXML(FileUtils.readFileToString(f, "utf-8"));

            for (QueryResultContainer.SingleRankedResult rankedResult : queryResultContainer.rankedResults) {

                // only non-empty and annotated results
                // No annotations found for document: clueWebID: clueweb12-1407wb-22-10643, queryID: 1006
                // <clueWebID>clueweb12-1407wb-22-10643</clueWebID>
                // <score>5.93809186</score>
                // <additionalInfo>indri</additionalInfo>
                // <plainText></plainText>

                if (rankedResult.plainText != null && !rankedResult.plainText.isEmpty()) {
                    if (rankedResult.mTurkRelevanceVotes.isEmpty()) {
                        //                        throw new IllegalStateException("No annotations found for document: "
                        System.err.println("No annotations found for document: "
                                + "clueWebID: "
                                + rankedResult.clueWebID + ", queryID: "
                                + queryResultContainer.qID);
                    }
                    else {

                        // first, get all the sentence IDs
                        byte[] bytes = new BASE64Decoder().decodeBuffer(
                                new ByteArrayInputStream(rankedResult.originalXmi.getBytes()));

                        JCas jCas = JCasFactory.createJCas();
                        XmiCasDeserializer
                                .deserialize(new ByteArrayInputStream(bytes), jCas.getCas());

                        // for each sentence, we'll collect all its annotations
                        TreeMap<Integer, SortedMap<String, String>> sentencesAndRelevanceAnnotations = collectSentenceIDs(
                                jCas);

                        // now we will the map with mturk annotations
                        // the list of true/false for each sentence will be consistent (the annotator ordering remains)
                        for (QueryResultContainer.MTurkRelevanceVote mTurkRelevanceVote : rankedResult.mTurkRelevanceVotes) {
                            for (QueryResultContainer.SingleSentenceRelevanceVote sentenceRelevanceVote : mTurkRelevanceVote.singleSentenceRelevanceVotes) {

                                String sentenceIDString = sentenceRelevanceVote.sentenceID;
                                if (sentenceIDString == null || sentenceIDString.isEmpty()) {
                                    throw new IllegalStateException(
                                            "Empty sentence ID for turker "
                                                    + mTurkRelevanceVote.turkID
                                                    + ", HIT: " +
                                                    mTurkRelevanceVote.hitID + ", clueWebID: "
                                                    + rankedResult.clueWebID + ", queryID: "
                                                    + queryResultContainer.qID);
                                }
                                else {

                                    Integer sentenceIDInt = Integer.valueOf(sentenceIDString);
                                    String value = sentenceRelevanceVote.relevant;

                                    // add to the list

                                    // sanity check first
                                    if (sentencesAndRelevanceAnnotations.get(sentenceIDInt)
                                            .containsKey(mTurkRelevanceVote.turkID)) {
                                        System.err
                                                .println("Annotations for sentence " + sentenceIDInt
                                                        + " for turker " + mTurkRelevanceVote.turkID
                                                        + " are duplicate");
                                    }

                                    sentencesAndRelevanceAnnotations.get(sentenceIDInt)
                                            .put(mTurkRelevanceVote.turkID, value);
                                }
                            }
                        }

                        //                    for (Map.Entry<Integer, SortedMap<String, String>> entry : sentencesAndRelevanceAnnotations
                        //                            .entrySet()) {
                        //                        System.out.println(entry.getKey() + ": " + entry.getValue());
                        //                    }

                        // we collect only the "clean" ones
                        Map<Integer, SortedMap<String, String>> cleanSentencesAndRelevanceAnnotations = new HashMap<>();

                        // sanity check -- all sentences are covered with the same number of annotations
                        for (Map.Entry<Integer, SortedMap<String, String>> entry : sentencesAndRelevanceAnnotations
                                .entrySet()) {
                            SortedMap<String, String> singleSentenceAnnotations = entry.getValue();

                            // remove empty sentences
                            if (singleSentenceAnnotations.values().isEmpty()) {
                                //                                throw new IllegalStateException(
                                System.err.println(
                                        "Empty annotations for sentence, " +
                                                "sentenceID: " + entry.getKey() + ", "
                                                + "clueWebID: "
                                                + rankedResult.clueWebID + ", queryID: "
                                                + queryResultContainer.qID
                                                + "; number of assignments: "
                                                + singleSentenceAnnotations.values().size()
                                                + ", expected: " + NUMBER_OF_TURKERS_PER_HIT
                                                + ". Sentence will be skipped in evaluation");
                            }
                            else if (singleSentenceAnnotations.values().size()
                                    != NUMBER_OF_TURKERS_PER_HIT) {
                                System.err.println(
                                        "Inconsistent annotations for sentences, " +
                                                "sentenceID: " + entry.getKey() + ", "
                                                + "clueWebID: "
                                                + rankedResult.clueWebID + ", queryID: "
                                                + queryResultContainer.qID
                                                + "; number of assignments: "
                                                + singleSentenceAnnotations.values().size()
                                                + ", expected: " + NUMBER_OF_TURKERS_PER_HIT
                                                + ". Sentence will be skipped in evaluation");
                            }
                            else {
                                cleanSentencesAndRelevanceAnnotations
                                        .put(entry.getKey(), entry.getValue());
                            }
                        }

                        // fill the annotation study

                        CodingAnnotationStudy study = new CodingAnnotationStudy(
                                NUMBER_OF_TURKERS_PER_HIT);
                        study.addCategory("true");
                        study.addCategory("false");

                        for (SortedMap<String, String> singleSentenceAnnotations : cleanSentencesAndRelevanceAnnotations
                                .values()) {
                            // only non-empty sentences
                            Collection<String> values = singleSentenceAnnotations.values();
                            if (!values.isEmpty() && values.size() == NUMBER_OF_TURKERS_PER_HIT) {
                                study.addItemAsArray(values.toArray());
                            }

                        }

                        //                    System.out.println(study.getCategories());

                        // Fleiss' multi-pi.
                        FleissKappaAgreement fleissKappaAgreement = new FleissKappaAgreement(study);

                        double percentage;
                        try {
                            percentage = fleissKappaAgreement.calculateObservedAgreement();
                        }
                        catch (InsufficientDataException ex) {
                            // dkpro-statistics feature, see https://github.com/dkpro/dkpro-statistics/issues/24
                            percentage = 1.0;
                        }

                        if (!Double.isNaN(percentage)) {
                            rankedResult.observedAgreement = percentage;
                            //                        System.out.println(sentencesAndRelevanceAnnotations.values());
                        }
                        else {
                            System.err.println("Observed agreement is NaN.");
                        }
                    }
                }
            }

            // and save the query to output dir
            File outputFile = new File(outputDir, queryResultContainer.qID + ".xml");
            FileUtils.writeStringToFile(outputFile, queryResultContainer.toXML(), "utf-8");
            System.out.println("Finished " + outputFile);
        }
    }

    public static TreeMap<Integer, SortedMap<String, String>> collectSentenceIDs(JCas jCas)
    {
        // for each sentence, we'll collect all its annotations
        TreeMap<Integer, SortedMap<String, String>> result = new TreeMap<>();
        for (Sentence sentence : JCasUtil.select(jCas, Sentence.class)) {
            int sentenceID = sentence.getBegin();

            // sentence begin is its ID
            result.put(sentenceID, new TreeMap<>());
        }

        return result;
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

        computeObservedAgreement(inputDir, outputDir);
    }

}