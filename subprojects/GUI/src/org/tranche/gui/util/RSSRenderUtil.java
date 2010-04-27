/*
 *    Copyright 2005 The Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tranche.gui.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.tranche.remote.RemoteUtil;

/**
 * @author Bryan Smith <bryanesmith at gmail.com>
 */
public class RSSRenderUtil {

    private static String NEWLINE = "\n";
    private static String PREVIEW_CSS = "<style type=\"text/css\"> <!--" + NEWLINE
            + "body {"
            + "  margin: 0;"
            + "  font-family: arial, sans-serif;"
            + "  line-height: 1.3em;"
            + "  font-size: 10px;"
            + "}" + NEWLINE
            + "h1 {  "
            + "  font-weight: normal; "
            + "  font-size: 1.5em; "
            + "  font-weight: bold; "
            + "  font-family: Georgia, Times, Times New Roman, serif; "
            + "  margin-top: 5px; "
            + "  margin-bottom: 14px;"
            + "}" + NEWLINE
            + "h2 {"
            + "  font-size: 1em; "
            + "  font-weight: bold; "
            + "  margin-bottom:0; "
            + "  margin-top: 8px; "
            + "  color: #666666; "
            + "}" + NEWLINE
            + "p.date {"
            + "  font-size: 1em; "
            + "  margin-top:0; "
            + "  margin-bottom: 0px;"
            + "  color: #000000;"
            + "}" + NEWLINE
            + "p.description {font-size: 1em; margin-top: 0;}" + NEWLINE
            + "p {font-size: 1em; margin-bottom: 5px;}" + NEWLINE
            + "a {"
            + "  color: #660000;"
            + "  font-size: 1.2em;"
            + "}" + NEWLINE
            + "hr {height: 1px; border: 0; margin: 0; background: none;}" + NEWLINE
            + "--></style>";
    /**
     * The regular expressions for XML tags
     */
    private static String TITLE_PATTERN = ".*<title\\s?.*?>(.*?)</title>.*", DESCRIPTION_PATTERN = ".*<description\\s?.*?>(.*?)</description>.*", DATE_PATTERN = ".*<pubDate\\s?.*?>(.*?)</pubDate>.*";
    /**
     * Number of entries in RSS to show
     */
    public static final int NUM_ENTRIES_TO_RENDER = 3;

    public RSSRenderUtil() {
    }

    /**
     * <p>Converts input from Tranche rss to HTML. Uses default number of entries that should be rendered. Uses small fonts and doesn't show links.</p>
     * @param in RSS InputStream
     * @param entriesToRead The number of entries to read
     * @return InputStream containing styled HTML...
     */
    public static InputStream RSSToHTMLPreview(InputStream in, int entriesToRead) throws Exception {
        return InnerRSSConverter(in, entriesToRead, PREVIEW_CSS);
    }

    private static InputStream InnerRSSConverter(InputStream in, int entriesToRead, String CSS) throws Exception {
        // Will hold the end HTML
        StringBuffer htmlBuffer = new StringBuffer();

        // Holds one item at a time
        StringBuffer itemBuffer;

        // Number of items already read
        int numItems = 0;

        // Reading in RSS, parsing out items, and building the HTML
        String nextLine = null;

        htmlBuffer.append("<html><head>" + CSS + "</head><body>" + NEWLINE);
        htmlBuffer.append("<h1>Latest News...</h1>" + NEWLINE);

        // Flag used to terminate parsing at end of RSS feed
        boolean isEndOfRSS = false;

        // Think about a header. Space is an issue...
//        htmlBuffer.append("<h1>Tranche RSS Feed</h1>" + NEWLINE);

        try {

            // Con
            while (numItems < entriesToRead && !isEndOfRSS) {

                // Burn everything until we find an item
                while (nextLine == null || !nextLine.contains("<item>")) {

                    nextLine = RemoteUtil.readLine(in);

                    // Happens if rss contains items than entriesToRead OR XML improperly formatted
                    if (nextLine != null && nextLine.contains("</rss>")) {
                        isEndOfRSS = true;
                        break;
                    }
                }

                if (isEndOfRSS) {
                    break;
                }

                itemBuffer = new StringBuffer();

                // Build the item buffer
                while (nextLine == null || !nextLine.contains("</item>")) {

                    nextLine = RemoteUtil.readLine(in);

                    // Happens if </item> and </rss> on the same line...
                    if (nextLine != null && nextLine.contains("</rss>")) {
                        isEndOfRSS = true;
                        break;
                    }

                    if (nextLine != null && !nextLine.contains("</item>")) {
                        itemBuffer.append(nextLine);
                    }
                }

                // Parse the item, adding to HTML

                String titleTextNode = parseTag(itemBuffer.toString(), TITLE_PATTERN),
                        dateTextNode = parseTag(itemBuffer.toString(), DATE_PATTERN),
                        descTextNode = parseTag(itemBuffer.toString(), DESCRIPTION_PATTERN);

                if (!titleTextNode.trim().equals("")) {
                    htmlBuffer.append("<h2>" + titleTextNode + "</h2>" + NEWLINE);
                }
                if (!dateTextNode.trim().equals("")) {
                    htmlBuffer.append("<p class=\"date\">" + dateTextNode + "</p>" + NEWLINE);
                }
                if (!descTextNode.trim().equals("")) {

                    // Trim down to 50 characters
                    if (descTextNode.length() > 100) {
                        descTextNode = descTextNode.substring(0, 99) + "...";
                    }

                    htmlBuffer.append("<p class=\"description\">" + descTextNode + "</p>" + NEWLINE);
                }

                htmlBuffer.append("<hr />");

                numItems++;
            }

        } catch (Exception e) {
            // Nope
        }

        htmlBuffer.append("<p><a href=\"http://tranche.proteomecommons.org/NewsfeedRedirector.jsp\" target=\"_blank\">More...</a></p>");

        htmlBuffer.append("</body></html>");

        return new ByteArrayInputStream(htmlBuffer.toString().getBytes());
    }

    /**
     * <p>Returns content of tag specified using RegEx in patternString.</p>
     */
    private static String parseTag(String item, String patternString) {

        Pattern pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(item);

        if (matcher.find()) {

            String match = matcher.group(1);
            return match;
        }

        // Not found
        return "";
    }
}
