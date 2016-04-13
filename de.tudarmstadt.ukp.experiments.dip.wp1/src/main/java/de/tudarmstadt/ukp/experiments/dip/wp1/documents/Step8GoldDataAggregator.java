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
import edu.isi.MACE;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.util.*;

/**
 * @author Maria Sukhareva
 */

public class Step8GoldDataAggregator
{

    final static String PROPERTY = "java.io.tmpdir";
    final static String TEMP_DIR = System.getProperty(PROPERTY);
    final static String TEMP_CSV = "CSVFile";
    final static String EXT = ".tmp";

    static public class Annotations
    {
        List<Integer> trueAnnotations;
        List<Integer> falseAnnotations;

        public Annotations(List<Integer> trueAnnotations, List<Integer> falseAnnotations)
        {
            this.trueAnnotations = trueAnnotations;
            this.falseAnnotations = falseAnnotations;
        }
    }

    public static void main(String[] args)
            throws Exception
    {
        String inputDir = args[0] + "/";
        // output dir
        File outputDir = new File(args[1]);
        File turkersConfidence = new File(args[2]);
        if (outputDir.exists()) {
            outputDir.delete();
        }
        outputDir.mkdir();

        List<String> annotatorsIDs = new ArrayList<>();
        //        for (File f : FileUtils.listFiles(new File(inputDir), new String[] { "xml" }, false)) {
        //            QueryResultContainer queryResultContainer = QueryResultContainer
        //                    .fromXML(FileUtils.readFileToString(f, "utf-8"));
        //            for (QueryResultContainer.SingleRankedResult rankedResults : queryResultContainer.rankedResults) {
        //                for (QueryResultContainer.MTurkRelevanceVote relevanceVote : rankedResults.mTurkRelevanceVotes) {
        //                    if (!annotatorsIDs.contains(relevanceVote.turkID))
        //                        annotatorsIDs.add(relevanceVote.turkID);
        //                }
        //            }
        //        }
        HashMap<String, Integer> countVotesForATurker = new HashMap<>();
        // creates temporary file with format for mace
        // Hashmap annotations: key is the id of a document and a sentence
        // Value is an array votes[] of turkers decisions: true or false (relevant or not)
        // the length of this array equals the number of annotators in List<String> annotatorsIDs.
        // If an annotator worked on the task his decision is written in the array otherwise the value is NULL

        // key: queryID + clueWebID + sentenceID
        // value: true and false annotations
        TreeMap<String, Annotations> annotations = new TreeMap<>();

        for (File f : FileUtils.listFiles(new File(inputDir), new String[] { "xml" }, false)) {
            QueryResultContainer queryResultContainer = QueryResultContainer
                    .fromXML(FileUtils.readFileToString(f, "utf-8"));
            System.out.println("Reading " + f.getName());
            for (QueryResultContainer.SingleRankedResult rankedResults : queryResultContainer.rankedResults) {
                String documentID = rankedResults.clueWebID;
                for (QueryResultContainer.MTurkRelevanceVote relevanceVote : rankedResults.mTurkRelevanceVotes) {
                    Integer turkerID;
                    if (!annotatorsIDs.contains(relevanceVote.turkID)) {
                        annotatorsIDs.add(relevanceVote.turkID);
                        turkerID = annotatorsIDs.size() - 1;
                    }
                    else {
                        turkerID = annotatorsIDs.indexOf(relevanceVote.turkID);
                    }
                    Integer count = countVotesForATurker.get(relevanceVote.turkID);
                    if (count == null) {
                        count = 0;
                    }
                    count++;
                    countVotesForATurker.put(relevanceVote.turkID, count);

                    String id;
                    List<Integer> trueVotes;
                    List<Integer> falseVotes;
                    for (QueryResultContainer.SingleSentenceRelevanceVote singleSentenceRelevanceVote : relevanceVote.singleSentenceRelevanceVotes)
                        if (!"".equals(singleSentenceRelevanceVote.sentenceID)) {

                            id = f.getName() + "_" + documentID + "_"
                                    + singleSentenceRelevanceVote.sentenceID;
                            Annotations turkerVotes = annotations.get(id);
                            if (turkerVotes == null) {
                                trueVotes = new ArrayList<>();
                                falseVotes = new ArrayList<>();
                                turkerVotes = new Annotations(trueVotes, falseVotes);
                            }
                            trueVotes = turkerVotes.trueAnnotations;
                            falseVotes = turkerVotes.falseAnnotations;
                            if ("true".equals(singleSentenceRelevanceVote.relevant)) {
                                // votes[turkerID] = true;
                                trueVotes.add(turkerID);
                            }
                            else if ("false".equals(singleSentenceRelevanceVote.relevant)) {
                                //   votes[turkerID] = false;
                                falseVotes.add(turkerID);
                            }
                            else {
                                throw new IllegalStateException("Annotation value of sentence "
                                        + singleSentenceRelevanceVote.sentenceID +
                                        " in " + rankedResults.clueWebID +
                                        " equals " + singleSentenceRelevanceVote.relevant);
                            }
                            try {
                                int allVotesCount = trueVotes.size() + falseVotes.size();
                                if (allVotesCount > 5) {
                                    System.err.println(
                                            id + " doesn't have 5 annotators: true: " + trueVotes
                                                    .size() + " false: " + falseVotes.size());

                                    // nasty hack, we're gonna strip some data; true votes first
                                    /* we can't do that, it breaks something down the line
                                    int toRemove = allVotesCount - 5;
                                    if (trueVotes.size() >= toRemove) {
                                        trueVotes = trueVotes
                                                .subList(0, trueVotes.size() - toRemove);
                                    }
                                    else if (
                                            falseVotes.size() >= toRemove) {
                                        falseVotes = falseVotes
                                                .subList(0, trueVotes.size() - toRemove);
                                    }
                                    */
                                    System.err.println("Adjusted: " +
                                            id + " doesn't have 5 annotators: true: " + trueVotes
                                            .size() + " false: " + falseVotes.size());
                                }
                            }
                            catch (IllegalStateException e) {
                                e.printStackTrace();
                            }
                            turkerVotes.trueAnnotations = trueVotes;
                            turkerVotes.falseAnnotations = falseVotes;
                            annotations.put(id, turkerVotes);
                        }
                        else {
                            throw new IllegalStateException(
                                    "Empty Sentence ID in " + f.getName() + " for turker "
                                            + turkerID
                            );
                        }

                }
            }

        }
        File tmp = printHashMap(annotations, annotatorsIDs.size());

        String file = TEMP_DIR + "/" + tmp.getName();
        MACE.main(new String[] { "--prefix", file });

        //gets the keys of the documents and sentences
        ArrayList<String> lines = (ArrayList<String>) FileUtils
                .readLines(new File(file + ".prediction"));
        int i = 0;
        TreeMap<String, TreeMap<String, ArrayList<HashMap<String, String>>>> ids = new TreeMap<>();
        ArrayList<HashMap<String, String>> sentences;
        if (lines.size() != annotations.size()) {
            throw new IllegalStateException("The size of prediction file is " + lines.size() +
                    "but expected " + annotations.size());
        }
        for (Map.Entry entry : annotations.entrySet()) { //1001.xml_clueweb12-1905wb-13-07360_8783
            String key = (String) entry.getKey();
            String[] IDs = key.split("_");
            if (IDs.length > 2) {
                String queryID = IDs[0];
                String clueWebID = IDs[1];
                String sentenceID = IDs[2];
                TreeMap<String, ArrayList<HashMap<String, String>>> clueWebIDs = ids.get(queryID);
                if (clueWebIDs == null) {
                    clueWebIDs = new TreeMap<>();
                }
                sentences = clueWebIDs.get(clueWebID);
                if (sentences == null) {
                    sentences = new ArrayList<>();
                }
                HashMap<String, String> sentence = new HashMap<>();
                sentence.put(sentenceID, lines.get(i));
                sentences.add(sentence);
                clueWebIDs.put(clueWebID, sentences);
                ids.put(queryID, clueWebIDs);
            }
            else {
                throw new IllegalStateException("Wrong ID " + key
                );
            }

            i++;
        }

        for (Map.Entry entry : ids.entrySet()) {
            TreeMap<Integer, String> value = (TreeMap<Integer, String>) entry.getValue();
            String queryID = (String) entry.getKey();
            QueryResultContainer queryResultContainer = QueryResultContainer
                    .fromXML(FileUtils.readFileToString(new File(inputDir, queryID), "utf-8"));
            for (QueryResultContainer.SingleRankedResult rankedResults : queryResultContainer.rankedResults) {
                for (Map.Entry val : value.entrySet()) {
                    String clueWebID = (String) val.getKey();
                    if (clueWebID.equals(rankedResults.clueWebID)) {
                        List<QueryResultContainer.SingleSentenceRelevanceVote> goldEstimatedLabels = new ArrayList<>();
                        List<QueryResultContainer.SingleSentenceRelevanceVote> turkersVotes = new ArrayList<>();
                        int size = 0;
                        int hitSize = 0;
                        String hitID = "";
                        for (QueryResultContainer.MTurkRelevanceVote vote : rankedResults.mTurkRelevanceVotes) {
                            if (!hitID.equals(vote.hitID)) {
                                hitID = vote.hitID;
                                hitSize = vote.singleSentenceRelevanceVotes.size();
                                size = size + hitSize;
                                turkersVotes.addAll(vote.singleSentenceRelevanceVotes);
                            }
                            else {
                                if (vote.singleSentenceRelevanceVotes.size() != hitSize) {
                                    hitSize = vote.singleSentenceRelevanceVotes.size();
                                    size = size + hitSize;
                                    turkersVotes.addAll(vote.singleSentenceRelevanceVotes);
                                }
                            }
                        }
                        ArrayList<HashMap<String, String>> sentenceList = (ArrayList<HashMap<String, String>>) val
                                .getValue();
                        if (sentenceList.size() != turkersVotes.size()) {
                            try {
                                throw new IllegalStateException(
                                        "Expected size of annotations is " + turkersVotes.size() +
                                                "but found " + sentenceList.size()
                                                + " for document " + rankedResults.clueWebID
                                                + " in " + queryID);
                            }
                            catch (IllegalStateException ex) {
                                ex.printStackTrace();
                            }
                        }
                        for (QueryResultContainer.SingleSentenceRelevanceVote s : turkersVotes) {
                            String valSentence = null;
                            for (HashMap<String, String> anno : sentenceList) {
                                if (anno.keySet().contains(s.sentenceID)) {
                                    valSentence = anno.get(s.sentenceID);
                                }
                            }
                            QueryResultContainer.SingleSentenceRelevanceVote singleSentenceVote = new QueryResultContainer.SingleSentenceRelevanceVote();
                            singleSentenceVote.sentenceID = s.sentenceID;
                            if (("false").equals(valSentence)) {
                                singleSentenceVote.relevant = "false";
                            }
                            else if (("true").equals(valSentence)) {
                                singleSentenceVote.relevant = "true";
                            }
                            else {
                                throw new IllegalStateException("Annotation value of sentence "
                                        + singleSentenceVote.sentenceID + " equals " + val
                                        .getValue());
                            }
                            goldEstimatedLabels.add(singleSentenceVote);
                        }
                        rankedResults.goldEstimatedLabels = goldEstimatedLabels;
                    }
                }
            }
            File outputFile = new File(outputDir, queryID);
            FileUtils.writeStringToFile(outputFile, queryResultContainer.toXML(), "utf-8");
            System.out.println("Finished " + outputFile);
        }

        ArrayList<String> annotators = (ArrayList<String>) FileUtils
                .readLines(new File(file + ".competence"));
        FileWriter fileWriter;
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < annotatorsIDs.size(); j++) {
            String[] s = annotators.get(0).split("\t");
            Float score = Float.parseFloat(s[j]);
            String turkerID = annotatorsIDs.get(j);
            System.out.println(turkerID + " " + score + " " + countVotesForATurker.get(turkerID));
            sb.append(turkerID).append(" ").append(score).append(" ")
                    .append(countVotesForATurker.get(turkerID)).append("\n");
        }
        fileWriter = new FileWriter(turkersConfidence);
        fileWriter.append(sb.toString());
        fileWriter.close();

    }

    // debugging function as well as a function that outputs CSV File for MACE
    public static File printHashMap(Map<String, Annotations> annotations, int numberOfAnnotators)
    {

        File dir = new File(TEMP_DIR);
        CSVPrinter csvFilePrinter;
        FileWriter fileWriter;
        CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator('\n').withDelimiter(',')
                .withQuote(null);

        File filename = null;
        try {
            filename = File.createTempFile(TEMP_CSV, EXT, dir);
            fileWriter = new FileWriter(filename);
            csvFilePrinter = new CSVPrinter(fileWriter, csvFileFormat);
            int count = 0;
            for (Map.Entry entry : annotations.entrySet()) {
                Annotations votes = (Annotations) entry
                        .getValue();
                //Create the CSVFormat object with "\n" as a record delimiter
                if (votes == null) {
                    throw new IllegalStateException("There are no votes for " +
                            entry.getKey());
                }
                ArrayList<Integer> trueAnnotators = (ArrayList<Integer>) votes.trueAnnotations;
                ArrayList<Integer> falseAnnotators = (ArrayList<Integer>) votes.falseAnnotations;
                if (trueAnnotators.size() + falseAnnotators.size() < 5) {
                    try {
                        throw new IllegalStateException("There are " +
                                trueAnnotators.size() + " true and "
                                + falseAnnotators.size() + " false and annotations for "
                                + entry.getKey() + " element");
                    }
                    catch (IllegalStateException ex) {
                        ex.printStackTrace();
                    }
                }
                List<String> votesString = Arrays.asList(new String[numberOfAnnotators]);
                for (int i = 0; i < numberOfAnnotators; i++) {
                    if (trueAnnotators.contains(i)) {
                        votesString.set(i, "true");
                    }
                    else if (falseAnnotators.contains(i)) {
                        votesString.set(i, "false");
                    }
                    else
                        votesString.set(i, "");
                }

                if (votesString.size() != numberOfAnnotators) {
                    throw new IllegalStateException(
                            "Number of annotators is " + votesString.size() + " expected "
                                    + numberOfAnnotators);
                }
                else {
                    csvFilePrinter.printRecord(votesString);
                }

                if (count % 1000 == 0) {
                    System.out.println("Processed " + count + " instances");
                }
                count++;

            }
            fileWriter.flush();
            fileWriter.close();
            csvFilePrinter.close();

        }
        catch (Exception e) {
            System.out.println("Error in CsvFileWriter !!!");
            e.printStackTrace();
        }
        System.out.println("Wrote to temporary file " + filename);

        return filename;

    }

}
