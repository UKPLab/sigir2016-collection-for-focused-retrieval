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

import com.martinkl.warc.WARCFileReader;
import com.martinkl.warc.WARCRecord;
import de.tudarmstadt.ukp.experiments.dip.wp1.data.QueryResultContainer;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Adds raw html text for each retrieved result in the query containers. Requires .warc.gz file
 * with all ClueWeb documents extracted in advance.
 *
 * @author Ivan Habernal
 */
public class Step3AddRawDocumentsFromClueWeb
{
    public static void main(String[] args)
            throws IOException
    {
        // input dir - list of xml query containers
        // step2a-retrieved-results
        File inputDir = new File(args[0]);

        // warc.bz file containing all required documents according to ClueWeb IDs
        // ltr-50queries-100docs-clueweb-export.warc.gz
        File warc = new File(args[1]);

        // output dir
        File outputDir = new File(args[2]);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // iterate over query containers
        for (File f : FileUtils.listFiles(inputDir, new String[] { "xml" }, false)) {
            QueryResultContainer queryResultContainer = QueryResultContainer
                    .fromXML(FileUtils.readFileToString(f, "utf-8"));

            // iterate over warc for each query
            WARCFileReader reader = new WARCFileReader(new Configuration(),
                    new Path(warc.getAbsolutePath()));
            try {
                while (true) {
                    WARCRecord read = reader.read();
                    String trecId = read.getHeader().getField("WARC-TREC-ID");

                    // now iterate over retrieved results for the query and find matching IDs
                    for (QueryResultContainer.SingleRankedResult rankedResults : queryResultContainer.rankedResults) {
                        if (rankedResults.clueWebID.equals(trecId)) {
                            // add the raw html content
                            String fullHTTPResponse = new String(read.getContent(), "utf-8");
                            // TODO fix coding?

                            String html = removeHTTPHeaders(fullHTTPResponse);

                            rankedResults.originalHtml = sanitizeXmlChars(html.trim());
                        }
                    }
                }
            }
            catch (EOFException e) {
                // end of file
            }

            // check if all results have filled html
            for (QueryResultContainer.SingleRankedResult rankedResults : queryResultContainer.rankedResults) {
                if (rankedResults.originalHtml == null) {
                    System.err.println("Missing original html for\t" + rankedResults.clueWebID
                            + ", setting relevance to false");
                    rankedResults.relevant = Boolean.FALSE.toString();
                }
            }

            // and save the query to output dir
            File outputFile = new File(outputDir, queryResultContainer.qID + ".xml");
            FileUtils.writeStringToFile(outputFile, queryResultContainer.toXML(), "utf-8");
            System.out.println("Finished " + outputFile);
        }

    }

    /**
     * Sanitizes the input string so it can be serialized as XML (removes unsupported control
     * characters)
     *
     * @param xml input string
     * @return output string
     */
    public static String sanitizeXmlChars(String xml)
    {
        if (xml == null || ("".equals(xml))) {
            return "";
        }

        Pattern xmlInvalidChars =
                Pattern.compile(
                        "[^\\u0009\\u000A\\u000D\\u0020-\\uD7FF\\uE000-\\uFFFD\\x{10000}-\\x{10FFFF}]"
                );
        return xmlInvalidChars.matcher(xml).replaceAll("");
    }

    public static String removeHTTPHeaders(String fullHTTPResponse)
    {
        int i = fullHTTPResponse.indexOf("Content-Length:");
        int j = fullHTTPResponse.indexOf("\n", i);

        // Get rid of HTTP headers. Look for the first '<'.
        int k = fullHTTPResponse.indexOf("<", j);

        return k != -1 ? fullHTTPResponse.substring(k) : fullHTTPResponse.substring(j + 1);
    }
}
