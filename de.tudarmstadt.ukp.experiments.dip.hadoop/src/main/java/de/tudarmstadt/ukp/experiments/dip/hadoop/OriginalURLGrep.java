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

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Given a file with node IDs, it finds their original URLs (stored in the index)
 *
 * @author Ivan Habernal
 */
public class OriginalURLGrep
        extends Configured
        implements Tool
{
    private static final String NODE_IDS = "node_ids";

    public static void main(String[] args)
            throws Exception
    {
        System.out.println("Main");
        ToolRunner.run(new OriginalURLGrep(), args);
    }

    @Override
    public int run(String[] args)
            throws Exception
    {
        org.apache.hadoop.conf.Configuration conf = getConf();
        String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();

        System.out.println("Other args: " + Arrays.toString(otherArgs));

        Job job = Job.getInstance();
        job.setJarByClass(OriginalURLGrep.class);

        job.setJobName(OriginalURLGrep.class.getName());
        job.setMapperClass(OrigURLGrepMapper.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(NullWritable.class);

        // cache file - IDs for index
        String idFile = args[2];
        System.err.println("idFile: " + idFile);
        job.addCacheFile(new URI(idFile + "#" + NODE_IDS));

        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        String commaSeparatedInputFiles = otherArgs[0];
        String outputPath = otherArgs[1];
        System.err.println("commaSeparatedInputFiles: " + commaSeparatedInputFiles);
        System.err.println("outputPath: " + outputPath);

        FileInputFormat.addInputPaths(job, commaSeparatedInputFiles);
        FileOutputFormat.setOutputPath(job, new Path(outputPath));

        return job.waitForCompletion(true) ? 0 : 1;
    }

    public static class OrigURLGrepMapper
            extends Mapper<LongWritable, Text, Text, NullWritable>
    {
        // a set of all IDs for whose we look for their original URLs
        private Set<String> requiredNodeIds = new HashSet<String>();

        @Override
        protected void setup(Context context)
                throws IOException, InterruptedException
        {
            // load the file with IDs
            if (context.getCacheFiles() != null && context.getCacheFiles().length > 0) {
                File ccNodesFile = new File("./" + NODE_IDS);

                List<String> lines = FileUtils.readLines(ccNodesFile);
                for (String line : lines) {
                    requiredNodeIds.add(line.trim());
                }
            }
        }

        @Override
        public void map(LongWritable key, Text value, Context context)
                throws IOException, InterruptedException
        {
            String line = value.toString();

            // split line
            String[] split = line.split("\\s+");
            String url = split[0];
            String nodeId = split[1];

            if (requiredNodeIds.contains(nodeId)) {
                // then write the URL to the output
                context.write(new Text(url), NullWritable.get());
            }
        }
    }
}
