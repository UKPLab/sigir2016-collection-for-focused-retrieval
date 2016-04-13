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

import com.thoughtworks.xstream.XStream;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.experiments.dip.wp1.data.QueryResultContainer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import sun.misc.BASE64Decoder;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * Exporting the final annotated data into XML output, one file per query.
 *
 * @author Ivan Habernal
 */
public class Step12GolDataExporter
{
    private static XStream xStream;

    public static TreeMap<Integer, Sentence> collectSentenceIDs(JCas jCas)
    {
        // for each sentence, we'll collect all its annotations
        TreeMap<Integer, Sentence> result = new TreeMap<>();
        for (Sentence sentence : JCasUtil.select(jCas, Sentence.class)) {
            int sentenceID = sentence.getBegin();

            // sentence begin is its ID
            result.put(sentenceID, sentence);
        }

        return result;
    }

    /**
     * Result container class
     */
    public static class SimpleSingleQueryResultContainer
    {
        public List<SimpleSingleDocumentQueryResultContainer> documents = new ArrayList<>();
        public String queryID;
        public String query;
    }

    /**
     * Result container class
     */
    public static class SimpleSingleDocumentQueryResultContainer
    {
        public List<ExportedSentence> sentences = new ArrayList<>();
        public String clueWebID;
    }

    /**
     * Result container class
     */
    public static class ExportedSentence
    {
        public String content;
        public Boolean relevant;
    }

    /**
     * Exporting final data
     *
     * @param inputDir  input directory with xml files
     * @param outputDir output directory
     * @throws Exception exception
     */
    public static void exportGoldDataSimple(File inputDir, File outputDir)
            throws Exception
    {
        // iterate over query containers
        for (File f : FileUtils.listFiles(inputDir, new String[] { "xml" }, false)) {
            QueryResultContainer queryResultContainer = QueryResultContainer
                    .fromXML(FileUtils.readFileToString(f, "utf-8"));

            SimpleSingleQueryResultContainer outputContainer = new SimpleSingleQueryResultContainer();
            outputContainer.query = queryResultContainer.query;
            outputContainer.queryID = queryResultContainer.qID;

            for (QueryResultContainer.SingleRankedResult rankedResult : queryResultContainer.rankedResults) {
                if (rankedResult.plainText != null && !rankedResult.plainText.isEmpty()) {
                    // first, get all the sentence IDs
                    byte[] bytes = new BASE64Decoder().decodeBuffer(
                            new ByteArrayInputStream(rankedResult.originalXmi.getBytes()));

                    JCas jCas = JCasFactory.createJCas();
                    XmiCasDeserializer
                            .deserialize(new ByteArrayInputStream(bytes), jCas.getCas());

                    // for each sentence, we'll collect all its annotations
                    TreeMap<Integer, Sentence> sentencesAndRelevanceAnnotations = collectSentenceIDs(
                            jCas);

                    // prepare output container
                    SimpleSingleDocumentQueryResultContainer singleOutputDocument = new SimpleSingleDocumentQueryResultContainer();
                    singleOutputDocument.clueWebID = rankedResult.clueWebID;

                    if (rankedResult.goldEstimatedLabels != null
                            && !rankedResult.goldEstimatedLabels.isEmpty()) {

                        for (QueryResultContainer.SingleSentenceRelevanceVote sentenceRelevanceVote : rankedResult.goldEstimatedLabels) {

                            String sentenceIDString = sentenceRelevanceVote.sentenceID;
                            Integer sentenceIDInt = Integer.valueOf(sentenceIDString);
                            String value = sentenceRelevanceVote.relevant;
                            Sentence sentence = sentencesAndRelevanceAnnotations.get(sentenceIDInt);

                            ExportedSentence s = new ExportedSentence();
                            s.content = sentence.getCoveredText();
                            s.relevant = Boolean.valueOf(value);

                            // add to the list
                            singleOutputDocument.sentences.add(s);
                        }

                        outputContainer.documents.add(singleOutputDocument);
                    }

                    // save
                    String xml = toXML(outputContainer);

                    File outputFile = new File(outputDir, outputContainer.queryID + ".xml");
                    FileUtils.writeStringToFile(outputFile, xml, "utf-8");
                }
            }
        }
    }

    /**
     * Configures XStream
     *
     * @return xStream instance
     */
    private static XStream getXStream()
    {
        if (xStream == null) {
            xStream = new XStream();

            xStream.alias("document", SimpleSingleDocumentQueryResultContainer.class);
            xStream.alias("singleQueryResults", SimpleSingleQueryResultContainer.class);
            xStream.alias("s", ExportedSentence.class);

            xStream.useAttributeFor(ExportedSentence.class, "relevant");
            xStream.useAttributeFor(SimpleSingleQueryResultContainer.class, "queryID");
            xStream.useAttributeFor(SimpleSingleDocumentQueryResultContainer.class, "clueWebID");

        }

        return xStream;
    }

    /**
     * Serializes object to XML
     *
     * @param object object
     * @return xml as string
     * @throws IOException exception
     */
    public static String toXML(Object object)
            throws IOException
    {
        Writer out = new StringWriter();
        out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");

        XStream xStream = getXStream();

        xStream.toXML(object, out);
        IOUtils.closeQuietly(out);

        return out.toString();
    }

    public static void main(String[] args)
            throws Exception
    {
        // /tmp/Step10AggregatedCleanGoldData /tmp/DIP2016Corpus
        exportGoldDataSimple(new File(args[0]), new File(args[1]));
    }

}
