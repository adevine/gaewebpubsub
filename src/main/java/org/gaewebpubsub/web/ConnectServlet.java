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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * ConnectServlet is called when a page includes the "javascript" to start up a connection to a topic. This servlet
 * makes the connection and then returns javascript that the client can use to send and subscribe to messages.
 */
public class ConnectServlet extends BaseServlet {
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

        //TODO - forward onto script
        resp.getWriter().println("User channel is " + userChannelToken);
    }
}
