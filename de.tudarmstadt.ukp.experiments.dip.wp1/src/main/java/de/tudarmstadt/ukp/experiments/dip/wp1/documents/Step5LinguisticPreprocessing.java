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

import de.tudarmstadt.ukp.dkpro.core.stanfordnlp.StanfordSegmenter;
import de.tudarmstadt.ukp.experiments.dip.wp1.data.QueryResultContainer;
import de.tudarmstadt.ukp.experiments.dip.wp1.documents.helpers.WebParagraphAnnotator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import sun.misc.BASE64Encoder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creates embedded XMI-serialized JCas with annotations: token, sentence, paragraph
 *
 * @author Ivan Habernal
 */
public class Step5LinguisticPreprocessing
{

    public static final Pattern OPENING_TAG_PATTERN = Pattern.compile("^<(\\S+)>");

    public static void main(String[] args)
            throws Exception
    {
        // input dir - list of xml query containers
        // step4-boiler-plate/
        File inputDir = new File(args[0]);

        // output dir
        File outputDir = new File(args[1]);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // iterate over query containers
        for (File f : FileUtils.listFiles(inputDir, new String[] { "xml" }, false)) {
            QueryResultContainer queryResultContainer = QueryResultContainer
                    .fromXML(FileUtils.readFileToString(f, "utf-8"));

            for (QueryResultContainer.SingleRankedResult rankedResults : queryResultContainer.rankedResults) {
                //                System.out.println(rankedResults.plainText);

                if (rankedResults.plainText != null) {
                    String[] lines = StringUtils.split(rankedResults.plainText, "\n");

                    // collecting all cleaned lines
                    List<String> cleanLines = new ArrayList<>(lines.length);
                    // collecting line tags
                    List<String> lineTags = new ArrayList<>(lines.length);

                    for (String line : lines) {
                        // get the tag
                        String tag = null;
                        Matcher m = OPENING_TAG_PATTERN.matcher(line);

                        if (m.find()) {
                            tag = m.group(1);
                        }

                        if (tag == null) {
                            throw new IllegalArgumentException(
                                    "No html tag found for line:\n" + line);
                        }

                        // replace the tag at the beginning and the end
                        String noTagText = line.replaceAll("^<\\S+>", "")
                                .replaceAll("</\\S+>$", "");

                        // do some html cleaning
                        noTagText = noTagText.replaceAll("&nbsp;", " ");

                        noTagText = noTagText.trim();

                        // add to the output
                        if (!noTagText.isEmpty()) {
                            cleanLines.add(noTagText);
                            lineTags.add(tag);
                        }
                    }

                    if (cleanLines.isEmpty()) {
                        // the document is empty
                        System.err.println("Document " + rankedResults.clueWebID + " in query " +
                                queryResultContainer.qID + " is empty");
                    }
                    else {
                        // now join them back to paragraphs
                        String text = StringUtils.join(cleanLines, "\n");

                        // create JCas
                        JCas jCas = JCasFactory.createJCas();
                        jCas.setDocumentText(text);
                        jCas.setDocumentLanguage("en");

                        // annotate WebParagraph
                        SimplePipeline.runPipeline(
                                jCas,
                                AnalysisEngineFactory.createEngineDescription(
                                        WebParagraphAnnotator.class
                                )
                        );

                        // fill the original tag information
                        List<WebParagraph> webParagraphs = new ArrayList<>(
                                JCasUtil.select(jCas, WebParagraph.class));

                        // they must be the same size as original ones
                        if (webParagraphs.size() != lineTags.size()) {
                            throw new IllegalStateException(
                                    "Different size of annotated paragraphs and original lines");
                        }

                        for (int i = 0; i < webParagraphs.size(); i++) {
                            WebParagraph p = webParagraphs.get(i);
                            // get tag
                            String tag = lineTags.get(i);

                            p.setOriginalHtmlTag(tag);
                        }

                        SimplePipeline.runPipeline(
                                jCas,
                                AnalysisEngineFactory.createEngineDescription(
                                        StanfordSegmenter.class,
                                        // only on existing WebParagraph annotations
                                        StanfordSegmenter.PARAM_ZONE_TYPES,
                                        WebParagraph.class.getCanonicalName()
                                )
                        );

                        // now convert to XMI
                        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
                        XmiCasSerializer.serialize(jCas.getCas(), byteOutputStream);

                        // encode to base64
                        String encoded = new BASE64Encoder().encode(byteOutputStream.toByteArray());

                        rankedResults.originalXmi = encoded;
                    }
                }
            }

            // and save the query to output dir
            File outputFile = new File(outputDir, queryResultContainer.qID + ".xml");
            FileUtils.writeStringToFile(outputFile, queryResultContainer.toXML(), "utf-8");
            System.out.println("Finished " + outputFile);
        }

    }
}
