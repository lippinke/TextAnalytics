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

import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.DefaultStringifier;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.util.GenericsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses Wikipedia, outputting title and text of all pages
 */
public class WikipediaParser extends Mapper<LongWritable, Text, Text, Text> {

  private static final Logger log = LoggerFactory.getLogger(WikipediaParser.class);

  private static final Pattern SPACE_NON_ALPHA_PATTERN = Pattern.compile("[\\s]");

  private static final String START_DOC = "<text xml:space=\"preserve\">";

  private static final String END_DOC = "</text>";

  private static final Pattern TITLE = Pattern.compile("<title>(.*)<\\/title>");

  private static final String REDIRECT = "#REDIRECT";

  @Override
  protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {

    String content = value.toString();
    if (content.contains(REDIRECT)) {
      return;
    }
    String document;
    String title;
    try {
      document = getDocument(content);
      document = stripDocumentFormatting(document);
      title = getTitle(content);
    } catch (RuntimeException e) {
      return;
    }
    context.write(new Text(title), new Text(document));
  }

  private static String getDocument(String xml) {
    int start = xml.indexOf(START_DOC) + START_DOC.length();
    int end = xml.indexOf(END_DOC, start);
    return xml.substring(start, end);
  }

  private static String getTitle(CharSequence xml) {
    Matcher m = TITLE.matcher(xml);
    return m.find() ? m.group(1) : "";
  }

  private static String stripDocumentFormatting(String xml) {
//      return xml.replaceAll("(\\{\\{([^{}]+|\\{\\{[^{}]*\\}\\})*\\}\\})|(\\{\\|(((?!\\|})[\\s\\S])*)+\\|\\})|(==[^=]+==)|(===[^=]+===)|(====[^=]+====)|(\\[\\[File:[^\\]]+\\]\\])|(\\[\\[Category:[^\\]]+\\]\\])|(&lt;!--(((?!--&gt;)[\\s\\S])*)+--&gt;)|(&lt;!--&quot(((?!--&gt;)[\\s\\S])*)+--&gt;)|(&lt;ref((?!&gt;)[\\s\\S])*\\/&gt;)|(&lt;ref((?!\\/ref&gt;)[\\s\\S])*\\/ref&gt;)|(&quot;)|(\\[http:\\/\\/(.+)\\])", " ");
//      return xml.replaceAll("(\\{\\{([^{}]+|\\{\\{[^{}]*\\}\\})*\\}\\})|(\\{\\|(((?!\\|})[\\s\\S])*)+\\|\\})|(==[^=]+==)|(===[^=]+===)|(====[^=]+====)|(\\[\\[File:[^\\]]+\\]\\])|(\\[\\[Category:[^\\]]+\\]\\])|(&quot;)|(\\[http:\\/\\/(.+)\\])", "");
      return xml.replaceAll("(&quot;)", "");
  }

}
