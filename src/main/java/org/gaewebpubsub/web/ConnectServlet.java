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

import org.gaewebpubsub.services.TopicManager;
import org.gaewebpubsub.util.Escapes;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Matcher;

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

        //get params
        String topicKey = getRequiredParameter(req, TOPIC_KEY_PARAM, false, TopicManager.MAX_KEY_LENGTH);
        String userKey = getRequiredParameter(req, USER_KEY_PARAM, false, TopicManager.MAX_KEY_LENGTH);
        String userName = getRequiredParameter(req, USER_NAME_PARAM, false, TopicManager.MAX_KEY_LENGTH);
        int topicLifetime = TopicManager.DEFAULT_TOPIC_LIFESPAN;
        try {
            int topicLifetimeParamValue = Integer.parseInt(req.getParameter(TOPIC_LIFETIME_PARAM));
            if (topicLifetimeParamValue > 0) {
                topicLifetime = Math.min(topicLifetimeParamValue, TopicManager.MAX_TOPIC_LIFESPAN);
            }
        } catch (Exception e) {
            //OK, lifetime stays at the default
        }
        boolean selfNotify = Boolean.parseBoolean(req.getParameter(SELF_NOTIFY_PARAM)); //defaults to false
        String validationToken = req.getParameter(ValidationFilter.VALIDATION_PARAM_NAME);

        //add user to the topic and return filtered JS file
        String userChannelToken = getTopicManager().connectUserToTopic(topicKey, userKey, userName,
                                                                       topicLifetime, selfNotify);

        resp.getWriter().println(filterConnectTemplate(javascriptTemplate,
                                                       getBaseUrl(req),
                                                       validationToken,
                                                       topicKey,
                                                       userKey,
                                                       userChannelToken));
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
                                           String baseUrl,
                                           String validationToken,
                                           String topicKey,
                                           String userKey,
                                           String userChannelToken) {
        String validationParam = validationToken == null ?
                                 "" :
                                 "&" + ValidationFilter.VALIDATION_PARAM_NAME
                                 + "=" + Escapes.escapeUrlParam(validationToken);

        template = template.replaceAll("@BASE_PATH@",
                                       Matcher.quoteReplacement(Escapes.escapeJavaScriptString(baseUrl)));
        template = template.replaceAll("@VALIDATION_PARAM@",
                                       Matcher.quoteReplacement(Escapes.escapeJavaScriptString(validationParam)));
        template = template.replaceAll("@TOPIC_KEY@",
                                       Matcher.quoteReplacement(Escapes.escapeJavaScriptString(topicKey)));
        template = template.replaceAll("@USER_KEY@",
                                       Matcher.quoteReplacement(Escapes.escapeJavaScriptString(userKey)));
        template = template.replaceAll("@CHANNEL_TOKEN@",
                                       Matcher.quoteReplacement(Escapes.escapeJavaScriptString(userChannelToken)));

        return template;
    }

}
