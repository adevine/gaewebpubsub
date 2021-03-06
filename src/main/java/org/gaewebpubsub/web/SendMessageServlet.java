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
 * This message is called when a user wishes to send a message to all the other users connected to a topic.
 */
public class SendMessageServlet extends BaseServlet {
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        allowCrossOriginRequests(resp);

        resp.setContentType("text/javascript");
        resp.setCharacterEncoding("UTF-8");

        String topicKey = getRequiredParameter(req, TOPIC_KEY_PARAM, false, TopicManager.MAX_KEY_LENGTH);
        String userKey = getRequiredParameter(req, USER_KEY_PARAM, false, TopicManager.MAX_KEY_LENGTH);
        String message = getRequiredParameter(req, MESSAGE_PARAM, true, TopicManager.MAX_MESSAGE_LENGTH);
        boolean selfNotify = Boolean.parseBoolean(getRequiredParameter(req, SELF_NOTIFY_PARAM, true, 10));
        boolean needsReceipt = Boolean.parseBoolean(getRequiredParameter(req, NEEDS_RECEIPT_PARAM, true, 10));
        int messageNumber = Integer.parseInt(getRequiredParameter(req, MESSAGE_NUMBER_PARAM, false, 100));

        log(String.format("Sending message '%s' on topic '%s' from user key '%s'", message, topicKey, userKey));

        getTopicManager().sendMessage(topicKey, userKey, message, messageNumber, selfNotify, needsReceipt);

        resp.setStatus(HttpServletResponse.SC_OK);
    }
}