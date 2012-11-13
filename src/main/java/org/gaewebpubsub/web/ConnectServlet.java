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
package org.gaewebpubsub.web;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.Scanner;

/**
 * ConnectServlet is called when a page includes the "javascript" to start up a connection to a topic. This servlet
 * makes the connection and then returns javascript that the client can use to send and subscribe to messages.
 */
public class ConnectServlet extends BaseServlet {
    /**
     * This string holds the template that will be filtered in order to send the response
     */
    protected String javascriptTemplate;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        javascriptTemplate = loadJavascriptTemplate();
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/javascript");
        resp.setCharacterEncoding("UTF-8");

        String topicKey = getRequiredParameter(req, TOPIC_KEY_PARAM, false /* can't be empty */);
        String userKey = getRequiredParameter(req, USER_KEY_PARAM, false /* can't be empty */);
        String userName = getRequiredParameter(req, USER_NAME_PARAM, false /* can't be empty */);
        int topicLifetime = 120; //default 120 mins like the Channel API
        try {
            int topicLifetimeParamValue = Integer.parseInt(req.getParameter(TOPIC_LIFETIME_PARAM));
            if (topicLifetimeParamValue > 0) {
                topicLifetime = topicLifetimeParamValue;
            }
        } catch (Exception e) {
            //OK, lifetime stays at the default
        }

        String userChannelToken = getTopicManager().connectUserToTopic(topicKey, userKey, userName, topicLifetime);

        //TODO - get base url
        resp.getWriter().println(filterConnectTemplate(javascriptTemplate, "TODO_GET_BASE", topicKey, userKey, userChannelToken));
    }

    protected String loadJavascriptTemplate() throws ServletException {
        Scanner scanner = null;
        try {
            File file = new File("WEB-INF/connectTemplate.js");
            StringBuilder retVal = new StringBuilder((int) file.length());
            scanner = new Scanner(file, "UTF-8");
            String lineSeparator = System.getProperty("line.separator");
            while (scanner.hasNextLine()) {
                retVal.append(scanner.nextLine()).append(lineSeparator);
            }
            return retVal.toString();
        } catch (IOException ioe) {
            throw new ServletException("Could not load connectTemplate.js file", ioe);
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
    }

    protected String filterConnectTemplate(String template,
                                           String baseUrl, String topicKey, String userKey, String userChannelToken) {
        //TODO - can improve on this
        //TODO - javascript escape all keys - replace " with \"
        template = template.replaceAll("@BASE_URL@", baseUrl);
        template = template.replaceAll("@TOPIC_KEY@", topicKey);
        template = template.replaceAll("@USER_KEY@", userKey);
        template = template.replaceAll("@CHANNEL_TOKEN@", userChannelToken);

        return template;
    }

}
