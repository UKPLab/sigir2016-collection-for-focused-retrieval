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

package de.tudarmstadt.ukp.experiments.dip.hadoop;

import com.martinkl.warc.WARCWritable;
import com.martinkl.warc.mapreduce.WARCInputFormat;
import com.martinkl.warc.mapreduce.WARCOutputFormat;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Extracts WARC records given a list of of TREC-IDs (in a text file); the matching results
 * are written into WARC files.
 * <p/>
 * In order to create only a single warc.gz file, run with
 * {@code -Dmapreduce.job.reduces=1} parameter
 * <p/>
 * For instance:
 * <pre>
 *     habernal@node-00b:~/dip-trec-extractor$ hadoop jar \
 *     de.tudarmstadt.ukp.dkpro.dip.hadoop-0.1-SNAPSHOT.jar \
 *     de.tudarmstadt.ukp.dkpro.dip.hadoop.ClueWebTRECIdFileExtractor \
 *     -Dmapreduce.job.queuename=longrunning -Dmapreduce.map.failures.maxpercent=1 \
 *     -Dmapreduce.job.reduces=1  /user/habernal/ClueWeb12/*.warc.gz \
 *     /user/habernal/dip-docs-for-quries-with-narratives2 \
 *     ltr-additional-queries-with-narratives.txt
 * </pre>
 * @author Ivan Habernal
 */
public class ClueWebTRECIdFileExtractor
        extends Configured
        implements Tool
{
    public static final String MAPREDUCE_MAPPER_TREC_IDS = "mapreduce.mapper.trec_ids";

    @Override
    public int run(String[] args)
            throws Exception
    {
        Job job = Job.getInstance(getConf());

        for (Map.Entry<String, String> next : job.getConfiguration()) {
            System.out.println(next.getKey() + ": " + next.getValue());
        }

        job.setJarByClass(ClueWebTRECIdFileExtractor.class);
        job.setJobName(ClueWebTRECIdFileExtractor.class.getName());

        // mapper
        job.setMapperClass(MapperClass.class);

        // input
        job.setInputFormatClass(WARCInputFormat.class);

        // output
        job.setOutputFormatClass(WARCOutputFormat.class);
        job.setOutputKeyClass(NullWritable.class);
        job.setOutputValueClass(WARCWritable.class);
        FileOutputFormat.setCompressOutput(job, true);

        // paths
        String commaSeparatedInputFiles = args[0];
        String outputPath = args[1];

        // load IDs to be searched for
        job.getConfiguration().set(MAPREDUCE_MAPPER_TREC_IDS, loadTrecIds(args[2]));

        FileInputFormat.addInputPaths(job, commaSeparatedInputFiles);
        FileOutputFormat.setOutputPath(job, new Path(outputPath));

        return job.waitForCompletion(true) ? 0 : 1;
    }

    /**
     * Returns a set of TREC IDs loaded from a file containing retrieved results in the form
     * <pre>
     * 1001 Q0 clueweb12-0710wb-90-29809 1 8.74085912 indri
     * 1001 Q0 clueweb12-0001wb-50-11437 2 8.12331515 indri
     * ...
     * </pre>
     *
     * @param irResultsFile file
     * @return a new-line delimited IDs
     * @throws IOException
     */
    static String loadTrecIds(String irResultsFile)
            throws IOException
    {
        StringBuilder sb = new StringBuilder();

        BufferedReader br = new BufferedReader(new FileReader(irResultsFile));
        String line;
        while ((line = br.readLine()) != null) {
            // split line
            sb.append(line.split(" ")[2]);
            sb.append("\n");
        }

        return sb.toString();
    }

    public static void main(String[] args)
            throws Exception
    {
        ToolRunner.run(new ClueWebTRECIdFileExtractor(), args);
    }

    /**
     * Mapper; omits WARCWritable for matching entries (with particular WARC-TREC-ID)
     */
    public static class MapperClass
            extends Mapper<LongWritable, WARCWritable, NullWritable, WARCWritable>
    {
        final Set<String> ids = new HashSet<String>();

        @Override
        protected void setup(Context context)
                throws IOException, InterruptedException
        {
            super.setup(context);
            String trecIds = context.getConfiguration().get(MAPREDUCE_MAPPER_TREC_IDS);
            ids.addAll(Arrays.asList(trecIds.split("\n")));
        }

        @Override
        protected void map(LongWritable key, WARCWritable value, Context context)
                throws IOException, InterruptedException
        {
            String trecId = value.getRecord().getHeader().getField("WARC-TREC-ID");

            if (ids.contains(trecId)) {
                context.write(NullWritable.get(), value);
            }
        }
    }
}

