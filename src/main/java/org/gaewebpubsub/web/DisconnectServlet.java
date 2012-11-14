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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This servlet is called if a user wishes to disconnect from the topic.
 */
public class DisconnectServlet extends BaseServlet {
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/javascript");
        resp.setCharacterEncoding("UTF-8");

        String topicKey = getRequiredParameter(req, TOPIC_KEY_PARAM, false, TopicManager.MAX_KEY_LENGTH);
        String userKey = getRequiredParameter(req, USER_KEY_PARAM, false, TopicManager.MAX_KEY_LENGTH);

        getTopicManager().disconnectUser(topicKey, userKey);

        resp.setStatus(HttpServletResponse.SC_OK);
    }
}