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

package de.tudarmstadt.ukp.experiments.dip.wp1.data;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.io.IOUtils;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Container for processing results of a single query mainly for relevance annotations.
 * It contains a list of retrieved results (with rank, score,
 * and ClueWeb id), each result then contains MTurk votes for relevance of sentences
 * All fields are intentionally public.
 *
 * @author Ivan Habernal
 */
public class QueryResultContainer
{
    public String qID;
    public String query;
    public List<String> relevantInformationExamples = new ArrayList<>();
    public List<String> irrelevantInformationExamples = new ArrayList<>();

    public List<SingleRankedResult> rankedResults = new ArrayList<>();

    public static class SingleRankedResult
    {
        public Integer rank;
        public String clueWebID;
        public Double score;
        public String additionalInfo;
        public String plainText;
        public String originalHtml;

        // true if all sentences (segments) are relevant; computed after segment annotation
        public String relevant;
        public String originalXmi;
        public String goldAnnotationsXmi;

        public List<MTurkRelevanceVote> mTurkRelevanceVotes = new ArrayList<>();

        public List<SingleSentenceRelevanceVote> goldEstimatedLabels = new ArrayList<>();

        public Double observedAgreement;
    }

    public static class MTurkRelevanceVote
    {
        public String turkID;
        public String hitID;
        public String acceptTime; //when a turker accepted a hit
        public String submitTime; //when a turker submitted a hit
        public String comment;
        public List<SingleSentenceRelevanceVote> singleSentenceRelevanceVotes = new ArrayList<>();
    }

    public static class SingleSentenceRelevanceVote
    {
        public String sentenceID;
        public String relevant;
    }

    public String toXML()
            throws IOException
    {
        Writer out = new StringWriter();
        out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");

        XStream xStream = initializeXStream();

        xStream.toXML(this, out);
        IOUtils.closeQuietly(out);

        return out.toString();
    }

    /**
     * Loads from XML file, performs validation.
     *
     * @param xml xml
     * @return new populated instance of query result container
     * @throws IOException if validation fails
     */
    public static QueryResultContainer fromXML(String xml)
            throws IOException
    {
        //        validateXML(xml);
        XStream xStream = initializeXStream();
        return (QueryResultContainer) xStream.fromXML(new StringReader(xml));
    }

    public static XStream initializeXStream()
    {
        XStream xStream = new XStream();
        xStream.alias("singleRankedResult", SingleRankedResult.class);
        xStream.alias("queryResultContainer", QueryResultContainer.class);
        xStream.alias("mTurkRelevanceVote", MTurkRelevanceVote.class);
        xStream.alias("singleSentenceRelevanceVote", SingleSentenceRelevanceVote.class);

        return xStream;
    }

    /**
     * Validates input XML file using 'queryResult.xsd' schema.
     *
     * @param xml xml
     * @throws IOException if file is not valid
     */
    public static void validateXML(String xml)
            throws IOException
    {
        String xsdName = "queryResult.xsd";
        URL resource = QueryResultContainer.class.getClass().getResource(xsdName);
        if (resource == null) {
            throw new IllegalStateException("Cannot locate resource " + xsdName + " on classpath");
        }

        URL xsdFile;
        try {
            xsdFile = resource.toURI().toURL();
        }
        catch (MalformedURLException | URISyntaxException e) {
            throw new IOException(e);
        }

        SchemaFactory factory =
                SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            Schema schema = factory.newSchema(xsdFile);
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new StringReader(xml)));
        }
        catch (SAXException e) {
            throw new IOException(e);
        }
    }
}
