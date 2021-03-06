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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * This servlet lets the client query for the currently connected users to a topic.
 */
public class SubscribersServlet extends BaseServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        allowCrossOriginRequests(resp);

        resp.setContentType("text/javascript");
        resp.setCharacterEncoding("UTF-8");

        String topicKey = getRequiredParameter(req, TOPIC_KEY_PARAM, false, TopicManager.MAX_KEY_LENGTH);

        List<String> subscriberNames = getTopicManager().getCurrentSubscribers(topicKey);

        resp.getWriter().print(listToJsonString(subscriberNames));
    }

    protected String listToJsonString(List<String> elements) {
        StringBuilder retVal = new StringBuilder();
        retVal.append('[');
        for (int i = 0; i < elements.size(); i++) {
            if (i > 0) {
                retVal.append(',');
            }
            retVal.append('"').append(Escapes.escapeJavaScriptString(elements.get(i))).append('"');
        }
        retVal.append(']');
        return retVal.toString();
    }
}
