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

import com.google.appengine.api.channel.ChannelServiceFactory;
import org.gaewebpubsub.services.ChannelApiTopicManager;
import org.gaewebpubsub.services.InMemoryTopicPersister;
import org.gaewebpubsub.services.TopicManager;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This base servlet just loads the topic manager and makes it available to subclasses.
 */
public class BaseServlet extends HttpServlet {
    private static final Logger log = Logger.getLogger(BaseServlet.class.getName());

    //common parameter names
    public static final String TOPIC_KEY_PARAM = "topicKey";
    public static final String USER_KEY_PARAM = "userKey";
    public static final String USER_NAME_PARAM = "userName";
    public static final String TOPIC_LIFETIME_PARAM = "topicLifetime";
    public static final String SELF_NOTIFY_PARAM = "selfNotify";
    public static final String MESSAGE_PARAM = "message";

    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        //only first servlet loads the topic manager
        if (config.getServletContext().getAttribute("topicManager") == null) {
            log.info("Loading topic manager");
            //note the creation of the topic manager could be made customizable with servlet config params -
            //for now, just always use a ChannelApiTopicManager
            ChannelApiTopicManager topicManager = new ChannelApiTopicManager();
            topicManager.setChannelService(ChannelServiceFactory.getChannelService());
            topicManager.setTopicPersister(new InMemoryTopicPersister());
            getServletConfig().getServletContext().setAttribute("topicManager", topicManager);
        }
    }

    protected TopicManager getTopicManager() {
        return (TopicManager) getServletConfig().getServletContext().getAttribute("topicManager");
    }

    protected String getRequiredParameter(HttpServletRequest request, String paramName, boolean canBeEmpty) {
        String retVal = request.getParameter(paramName);
        if (retVal == null) {
            log.warning("Require parameter " + paramName + " not found");
            //TODO - is this the right exception to throw? Ideally throwing an exception returns the proper HTTP status code
            throw new IllegalArgumentException("Missing parameter " + paramName);
        }
        if (!canBeEmpty && retVal.trim().length() == 0)  {
            log.warning("Require parameter " + paramName + " was empty");
            throw new IllegalArgumentException("Empty parameter " + paramName);
        }
        return retVal;
    }

    protected void debugLog(String format, Object... params) {
        //TODO - change this back to fine
        if (log.isLoggable(Level.INFO)) {
            log.info(String.format(format, params));
        }
    }

    /**
     * Helper method gets the base URL of a request.
     *
     * @param request The incoming request.
     * @return The base URL of the request (i.e., the request without the final file)
     */
    public static String getBaseUrl(HttpServletRequest request) {
        try {
            URL baseUrl = new URL(request.getScheme(),
                                  request.getServerName(),
                                  request.getServerPort(),
                                  request.getContextPath());
            return baseUrl.toExternalForm();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
