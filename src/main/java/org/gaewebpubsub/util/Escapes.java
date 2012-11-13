/*
   Copyright 2012 Alexander Devine

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.gaewebpubsub.util;

import java.net.URLEncoder;

/**
 * This helper class could easily be replaced by commons lang StringEscapeUtils, but for simplicity I didn't want to
 * add any dependencies.
 */
public class Escapes {
    public static String escapeHtml(String input) {
        StringBuilder retVal = new StringBuilder(input.length() + 10);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c > 127 || c == '"' || c == '<' || c == '>') {
                retVal.append("&#").append((int)c).append(';');
            } else {
                retVal.append(c);
            }
        }
        return retVal.toString();
    }

    public static String escapeUrlParam(String input) {
        try {
            return URLEncoder.encode(input, "UTF-8");
        } catch (Exception e) {
            throw new Error("Shouldn't happen, UTF-8 support is guaranteed");
        }
    }

    public static String escapeJavaScriptString(String input) {
        StringBuilder retVal = new StringBuilder(input.length() + 10);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '\'':case '"':case '/':case '\\':
                    retVal.append('\\').append(c);
                    break;
                case '\n':
                    retVal.append("\\n");
                    break;
                case '\r':
                    retVal.append("\\r");
                    break;
                case '\t':
                    retVal.append("\\t");
                    break;
                default:
                    retVal.append(c);
            }
        }
        return retVal.toString();

    }
}
