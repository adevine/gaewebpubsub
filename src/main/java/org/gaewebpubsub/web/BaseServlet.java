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

import org.gaewebpubsub.services.Defaults;
import org.gaewebpubsub.services.TopicManager;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;

/**
 * This base servlet just loads the topic manager and makes it available to subclasses.
 */
public class BaseServlet extends HttpServlet {
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
            log("Loading topic manager");
            getServletConfig().getServletContext().setAttribute("topicManager", Defaults.newTopicManager());
        }
    }

    protected TopicManager getTopicManager() {
        return (TopicManager) getServletConfig().getServletContext().getAttribute("topicManager");
    }

    protected String getRequiredParameter(HttpServletRequest request,
                                          String paramName,
                                          boolean canBeEmpty,
                                          int maxLength) {
        String retVal = request.getParameter(paramName);
        if (retVal == null) {
            //TODO - is this the right exception to throw? Ideally throwing an exception returns the proper HTTP status code
            throw new IllegalArgumentException("Missing parameter " + paramName);
        }
        if (!canBeEmpty && retVal.trim().length() == 0)  {
            throw new IllegalArgumentException("Empty parameter " + paramName);
        }
        if (retVal.length() >= maxLength) {
            throw new IllegalArgumentException(paramName + " must have fewer than " + maxLength + " characters");
        }
        return retVal;
    }

    /**
     * By overriding this method we allow incoming requests from AJAX calls being served from a different domain.
     * However, subclasses will still need to call the allowCrossOriginRequests method from their respective handlers
     * to allow incoming cross domain requests.
     */
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        allowCrossOriginRequests(resp);
    }

    /**
     * This method sets the Access-Control-Allow-Origin, Access-Control-Allow-Methods and Access-Control-Max-Age
     * methods to allow cross domain AJAX requests for GET, POST and OPTION requests.
     */
    protected void allowCrossOriginRequests(HttpServletResponse response) throws ServletException, IOException {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.setHeader("Access-Control-Max-Age", "1728000");
    }

    /**
     * Helper method gets the base URL of a request.
     *
     * @param request The incoming request.
     * @return The base URL of the request (i.e., the request without the servlet path)
     */
    public static String getBaseUrl(HttpServletRequest request) {
        try {
            URL baseUrl = new URL(request.getScheme(),
                                  request.getServerName(),
                                  request.getServerPort(),
                                  request.getContextPath());
            if (baseUrl.getDefaultPort() == baseUrl.getPort()) {
                //then leave out the port for a friendlier url
                baseUrl = new URL(request.getScheme(), request.getServerName(), request.getContextPath());
            }
            return baseUrl.toExternalForm();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
