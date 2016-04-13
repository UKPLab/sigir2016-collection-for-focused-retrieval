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
import de.tudarmstadt.ukp.experiments.dip.wp1.documents.helpers.MTurkOutputReader;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.*;

/**
 * Collects the MTurk annotation results and fills the annotations back to the
 * query containers
 *
 * @author Maria Sukhareva
 */
public class Step7CollectMTurkResults
{

    /**
     * Container for a single MTurk vote parsed from CSV output
     */
    static public class MTurkAnnotation
    {
        private final String hitID;
        private final String annotatorID;
        private final String acceptTime;
        private final String submitTime;
        private final String comment;
        private final String clueWeb;
        private final String[] relevant;
        private final String[] irrelevant;

        public MTurkAnnotation(String hitID, String annotatorID, String acceptTime,
                String submitTime, String comment, String clueWeb,
                String[] relevant, String[] irrelevant)
        {

            // make sure all required parameters are set
            if (hitID.isEmpty()) {
                throw new IllegalArgumentException("Parameter hitID is empty");
            }
            this.hitID = hitID;

            if (annotatorID.isEmpty()) {
                throw new IllegalArgumentException("Parameter annotatorID is empty");
            }
            this.annotatorID = annotatorID;

            if (acceptTime.isEmpty()) {
                throw new IllegalArgumentException("Parameter acceptTime is empty");
            }
            this.acceptTime = acceptTime;

            if (submitTime.isEmpty()) {
                throw new IllegalArgumentException("Parameter submitTime is empty");
            }
            this.submitTime = submitTime;

            if (clueWeb.isEmpty()) {
                throw new IllegalArgumentException("Parameter clueWeb is empty");
            }
            this.clueWeb = clueWeb;

            if (relevant == null) {
                throw new IllegalArgumentException("Parameter relevant is null");
            }
            this.relevant = relevant;

            if (irrelevant == null) {
                throw new IllegalArgumentException("Parameter irrelevant is null");
            }
            this.irrelevant = irrelevant;

            // optional fields
            if (comment != null && !comment.isEmpty()) {
                this.comment = comment;
            }
            else {
                this.comment = null;
            }

            // now check consistence: either relevant or irrelevant sentences must be set
            // TODO don't do it, they might be both empty :(
            if (relevant.length == 0 && irrelevant.length == 0) {
                throw new IllegalArgumentException(
                        "Both relevant and irrelevant sentences are empty");
            }

        }

    }

    public static void main(String[] args)
            throws Exception
    {
        // input dir - list of xml query containers
        // /home/user-ukp/research/data/dip/wp1-documents/step4-boiler-plate/
        File inputDir = new File(args[0] + "/");

        // MTurk result file

        // output dir
        File outputDir = new File(args[2]);
        if (!outputDir.exists()) {
            outputDir.mkdirs();

        }

        // Folder with success files
        File mturkSuccessDir = new File(args[1]);

        Collection<File> files = FileUtils
                .listFiles(mturkSuccessDir, new String[] { "result" }, false);
        if (files.isEmpty()) {
            throw new IllegalArgumentException("Input folder is empty. " + mturkSuccessDir);
        }

        HashMap<String, List<MTurkAnnotation>> mturkAnnotations = new HashMap<>();

        // parsing all CSV files
        for (File mturkCSVResultFile : files) {
            System.out.println("Parsing " + mturkCSVResultFile.getName());

            MTurkOutputReader outputReader = new MTurkOutputReader(
                    new HashSet<>(Arrays.asList("annotation", "workerid")),
                    mturkCSVResultFile);

            // for fixing broken data input: for each hit, collect all sentence IDs
            Map<String, SortedSet<String>> hitSentences = new HashMap<>();

            // first iteration: collect the sentences
            for (Map<String, String> record : outputReader) {
                String hitID = record.get("hitid");
                if (!hitSentences.containsKey(hitID)) {
                    hitSentences.put(hitID, new TreeSet<>());
                }

                String relevantSentences = record.get("Answer.relevant_sentences");
                String irrelevantSentences = record.get("Answer.irrelevant_sentences");

                if (relevantSentences != null) {
                    hitSentences.get(hitID).addAll(Arrays.asList(relevantSentences.split(",")));
                }

                if (irrelevantSentences != null) {
                    hitSentences.get(hitID).addAll(Arrays.asList(irrelevantSentences.split(",")));
                }
            }

            // and now second iteration
            for (Map<String, String> record : outputReader) {
                String hitID = record.get("hitid");
                String annotatorID = record.get("workerid");
                String acceptTime = record.get("assignmentaccepttime");
                String submitTime = record.get("assignmentsubmittime");
                String relevantSentences = record.get("Answer.relevant_sentences");
                String irrelevantSentences = record.get("Answer.irrelevant_sentences");
                String reject = record.get("reject");
                String filename[];
                String comment;
                String clueWeb;
                String[] relevant = {};
                String[] irrelevant = {};

                filename = record.get("annotation").split("_");
                String fileXml = filename[0];
                clueWeb = filename[1].trim();
                comment = record.get("Answer.comment");

                if (relevantSentences != null) {
                    relevant = relevantSentences.split(",");
                }

                if (irrelevantSentences != null) {
                    irrelevant = irrelevantSentences.split(",");
                }

                // sanitizing data: if both relevant and irrelevant are empty, that's a bug
                // we're gonna look up all sentences from this HIT and treat this assignment
                // as if there were only irrelevant ones
                if (relevant.length == 0 && irrelevant.length == 0) {
                    SortedSet<String> strings = hitSentences.get(hitID);
                    irrelevant = new String[strings.size()];
                    strings.toArray(irrelevant);
                }

                if (reject != null) {
                    System.out.println(
                            " HIT " + hitID + " annotated by " + annotatorID + " was rejected ");
                }
                else {
                    /*
                    // relevant sentences is a comma-delimited string,
                    // this regular expression is rather strange
                    // it must contain digits, it might be that there is only one space or a comma or some other char
                    // digits are the sentence ids. if relevant sentences do not contain digits then it is wrong
                    if (relevantSentences.matches("^\\D*$") &&
                            irrelevantSentences.matches("^\\D*$")) {
                        try {
                            throw new IllegalStateException(
                                    "No annotations found for HIT " + hitID + " in " +
                                            fileXml + " for document " + clueWeb);
                        }
                        catch (IllegalStateException ex) {
                            ex.printStackTrace();
                        }

                    }
                    */
                    MTurkAnnotation mturkAnnotation;
                    try {
                        mturkAnnotation = new MTurkAnnotation(hitID, annotatorID, acceptTime,
                                submitTime, comment, clueWeb, relevant, irrelevant);
                    }
                    catch (IllegalArgumentException ex) {
                        throw new IllegalArgumentException("Record: " + record, ex);
                    }

                    List<MTurkAnnotation> listOfAnnotations = mturkAnnotations.get(fileXml);

                    if (listOfAnnotations == null) {
                        listOfAnnotations = new ArrayList<>();
                    }
                    listOfAnnotations.add(mturkAnnotation);
                    mturkAnnotations.put(fileXml, listOfAnnotations);
                }

            }
            //            parser.close();
        }

        // Debugging: output number of HITs of a query
        System.out.println("Accepted HITs for a query:");
        for (Map.Entry e : mturkAnnotations.entrySet()) {
            ArrayList<MTurkAnnotation> a = (ArrayList<MTurkAnnotation>) e.getValue();
            System.out.println(e.getKey() + " " + a.size());
        }

        for (File f : FileUtils.listFiles(inputDir, new String[] { "xml" }, false)) {
            QueryResultContainer queryResultContainer = QueryResultContainer
                    .fromXML(FileUtils.readFileToString(f, "utf-8"));
            String fileName = f.getName();
            List<MTurkAnnotation> listOfAnnotations = mturkAnnotations.get(fileName);

            if (listOfAnnotations == null || listOfAnnotations.isEmpty()) {
                throw new IllegalStateException(
                        "No annotations for " + f.getName()
                );
            }

            for (QueryResultContainer.SingleRankedResult rankedResults : queryResultContainer.rankedResults) {
                for (MTurkAnnotation mtAnnotation : listOfAnnotations) {
                    String clueWeb = mtAnnotation.clueWeb;
                    if (rankedResults.clueWebID.equals(clueWeb)) {
                        List<QueryResultContainer.MTurkRelevanceVote> mTurkRelevanceVotes = rankedResults.mTurkRelevanceVotes;
                        QueryResultContainer.MTurkRelevanceVote relevanceVote = new QueryResultContainer.MTurkRelevanceVote();
                        String annotatorID = mtAnnotation.annotatorID;
                        String hitID = mtAnnotation.hitID;
                        String acceptTime = mtAnnotation.acceptTime;
                        String submitTime = mtAnnotation.submitTime;
                        String comment = mtAnnotation.comment;
                        String[] relevant = mtAnnotation.relevant;
                        String[] irrelevant = mtAnnotation.irrelevant;
                        relevanceVote.turkID = annotatorID.trim();
                        relevanceVote.hitID = hitID.trim();
                        relevanceVote.acceptTime = acceptTime.trim();
                        relevanceVote.submitTime = submitTime.trim();
                        relevanceVote.comment = comment != null ? comment.trim() : null;
                        if (relevant.length == 0 && irrelevant.length == 0) {
                            try {
                                throw new IllegalStateException(
                                        "the length of the annotations is 0" +
                                                rankedResults.clueWebID + " for HIT "
                                                + relevanceVote.hitID
                                );
                            }
                            catch (IllegalStateException e) {
                                e.printStackTrace();
                            }
                        }
                        for (String r : relevant) {
                            String sentenceId = r.trim();
                            if (!sentenceId.isEmpty() && sentenceId.matches("\\d+")) {
                                QueryResultContainer.SingleSentenceRelevanceVote singleSentenceVote = new QueryResultContainer.SingleSentenceRelevanceVote();
                                singleSentenceVote.sentenceID = sentenceId;
                                singleSentenceVote.relevant = "true";
                                relevanceVote.singleSentenceRelevanceVotes
                                        .add(singleSentenceVote);
                            }
                        }
                        for (String r : irrelevant) {
                            String sentenceId = r.trim();
                            if (!sentenceId.isEmpty() && sentenceId.matches("\\d+")) {
                                QueryResultContainer.SingleSentenceRelevanceVote singleSentenceVote = new QueryResultContainer.SingleSentenceRelevanceVote();
                                singleSentenceVote.sentenceID = sentenceId;
                                singleSentenceVote.relevant = "false";
                                relevanceVote.singleSentenceRelevanceVotes
                                        .add(singleSentenceVote);
                            }
                        }
                        mTurkRelevanceVotes.add(relevanceVote);
                    }
                }

            }
            File outputFile = new File(outputDir, f.getName());
            FileUtils.writeStringToFile(outputFile, queryResultContainer.toXML(), "utf-8");
            System.out.println("Finished " + outputFile);
        }

    }

}