/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package parse_wikipedia;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.List;

import org.apache.commons.cli2.CommandLine;
import org.apache.commons.cli2.Group;
import org.apache.commons.cli2.Option;
import org.apache.commons.cli2.OptionException;
import org.apache.commons.cli2.builder.ArgumentBuilder;
import org.apache.commons.cli2.builder.DefaultOptionBuilder;
import org.apache.commons.cli2.builder.GroupBuilder;
import org.apache.commons.cli2.commandline.Parser;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DefaultStringifier;
import org.apache.hadoop.io.Stringifier;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.GenericsUtil;
import org.apache.mahout.common.CommandLineUtil;
import org.apache.mahout.common.HadoopUtil;
import org.apache.mahout.common.commandline.DefaultOptionCreator;
import org.apache.mahout.common.iterator.FileLineIterable;
import parse_wikipedia.WikipediaParser;
import parse_wikipedia.XmlInputFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.mahout.common.Pair;
import org.apache.mahout.common.iterator.sequencefile.SequenceFileIterable;
import org.apache.mahout.vectorizer.DictionaryVectorizer;
import org.apache.mahout.vectorizer.DocumentProcessor;
import org.apache.mahout.vectorizer.common.PartialVectorMerger;
import org.apache.mahout.vectorizer.tfidf.TFIDFConverter;
import org.apache.mahout.math.Vector;

/**
 * This class spawns some Mappers to parse Wikipedia.
 */
public final class ParseWikipedia {
  
  private static final Logger log = LoggerFactory.getLogger(ParseWikipedia.class);

  private ParseWikipedia() { }

  public static void main(String[] args) throws IOException {
    DefaultOptionBuilder obuilder = new DefaultOptionBuilder();
    GroupBuilder gbuilder = new GroupBuilder();

    Option dirInputPathOpt = DefaultOptionCreator.inputOption().create();
    Option dirOutputPathOpt = DefaultOptionCreator.outputOption().create();

    Group group = gbuilder.withName("Options").withOption(dirInputPathOpt).withOption(dirOutputPathOpt).create();

    Parser parser = new Parser();
    parser.setGroup(group);
    
    try {
      CommandLine cmdLine = parser.parse(args);
      
      String inputPath = (String) cmdLine.getValue(dirInputPathOpt);
      String outputPath = (String) cmdLine.getValue(dirOutputPathOpt);

      runJob(inputPath, outputPath);
    } catch (OptionException | InterruptedException | ClassNotFoundException e) {
      log.error("Exception", e);
    }
     
  }

  public static void runJob(String input, String output)
                            throws IOException, InterruptedException, ClassNotFoundException {
    Configuration conf = new Configuration();
    conf.set("xmlinput.start", "<page>");
    conf.set("xmlinput.end", "</page>");
    
    conf.set("io.serializations",
             "org.apache.hadoop.io.serializer.JavaSerialization,"
             + "org.apache.hadoop.io.serializer.WritableSerialization");

    Job job = new Job(conf);
    log.info("Input: {} Out: {}", input, output);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(Text.class);
    FileInputFormat.setInputPaths(job, new Path(input));
    Path outPath = new Path(output);
    FileOutputFormat.setOutputPath(job, outPath);
    job.setMapperClass(WikipediaParser.class);
    job.setInputFormatClass(XmlInputFormat.class);
    job.setReducerClass(Reducer.class);
    job.setOutputFormatClass(SequenceFileOutputFormat.class);
    job.setJarByClass(ParseWikipedia.class);

    HadoopUtil.delete(conf, outPath);
    
    boolean succeeded = job.waitForCompletion(true);
    if (!succeeded) {
      throw new IllegalStateException("Job failed!");
    }

    /* TFIDF*/

    Path tokenizedDocumentsPath = new Path(output,
            DocumentProcessor.TOKENIZED_DOCUMENT_OUTPUT_FOLDER);
    Path termFrequencyVectorsPath = new Path(output,
            DictionaryVectorizer.DOCUMENT_VECTOR_OUTPUT_FOLDER);
    Path tfidfPath = new Path(outPath + "tfidf");
    DocumentProcessor.tokenizeDocuments(outPath,
            StandardAnalyzer.class, tokenizedDocumentsPath, conf);

    DictionaryVectorizer.createTermFrequencyVectors(tokenizedDocumentsPath,
            outPath,
            DictionaryVectorizer.DOCUMENT_VECTOR_OUTPUT_FOLDER,
            conf, 1, 1, 0.0f, PartialVectorMerger.NO_NORMALIZING,
            true, 1, 100, false, false);

    Pair<Long[], List<Path>> documentFrequencies = TFIDFConverter
            .calculateDF(termFrequencyVectorsPath, tfidfPath,
                         conf, 100);

    TFIDFConverter.processTfIdf(termFrequencyVectorsPath, tfidfPath,
            conf, documentFrequencies, 1, 100,
            PartialVectorMerger.NO_NORMALIZING, false, false, false, 1);
  }
}


