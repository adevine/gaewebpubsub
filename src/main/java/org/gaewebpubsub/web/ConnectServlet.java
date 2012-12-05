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

import org.gaewebpubsub.services.SubscriberData;
import org.gaewebpubsub.services.TopicManager;
import org.gaewebpubsub.util.Escapes;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * ConnectServlet is called when a page includes the "javascript" to start up a connection to a topic. This servlet
 * makes the connection and then returns javascript that the client can use to send and subscribe to messages.
 */
public class ConnectServlet extends BaseServlet {

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
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
        String validationToken = req.getParameter(ValidationFilter.VALIDATION_PARAM_NAME);

        //add user to the topic and return filtered JS file
        SubscriberData subscriberData =
                getTopicManager().connectUserToTopic(topicKey, userKey, userName, topicLifetime);

        //JSP file needs access to the following pieces of data
        String validationParam = validationToken == null ?
                                 "" :
                                 "&" + ValidationFilter.VALIDATION_PARAM_NAME
                                 + "=" + Escapes.escapeUrlParam(validationToken);
        req.setAttribute("basePath", Escapes.escapeJavaScriptString(getBaseUrl(req)));
        req.setAttribute("validationParam", Escapes.escapeJavaScriptString(validationParam));
        req.setAttribute("topicKey", Escapes.escapeJavaScriptString(topicKey));
        req.setAttribute("userKey", Escapes.escapeJavaScriptString(userKey));
        req.setAttribute("userName", Escapes.escapeJavaScriptString(userName));
        req.setAttribute("channelToken", Escapes.escapeJavaScriptString(subscriberData.channelToken));
        req.setAttribute("startingMessageNum", subscriberData.messageCount);

        getServletContext().getRequestDispatcher("/WEB-INF/pages/connectTemplate.jsp").forward(req, resp);
    }
}
