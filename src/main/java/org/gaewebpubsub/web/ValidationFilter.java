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
import org.gaewebpubsub.services.ConfigManager;
import org.gaewebpubsub.util.ValidationUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * ValidationFilter can be used to ensure only authorized requests are made to the service.
 * By default, validation works as follows:
 *
 * <ol>
 *     <li>If there is no config property in the datastore (kind=Config) with the key name "validationKey", then
 *     no validation is performed and all requests are allowed.</li>
 *     <li>If there IS a validationKey config property, then it is used to validate the request path. It is expected
 *     that the last path element in the request will be of the format "timestamp|validationHash", where timestamp is
 *     the current Java timestamp, and validationHash is equal to
 *     com.gaewebpubsub.util.SecureHash.hash(timestamp + validationKeyFromConfig). This validation protocol makes it
 *     so that only other servers can send valid requests if they know the secret validation key.</li>
 * </ol>
 *
 * A different validation algorithm can be implemented by overriding the "validateRequest" method of this class, and
 * then specifying that subclass as the filter in web.xml.
 *
 * @see org.gaewebpubsub.util.ValidationUtils
 */
public class ValidationFilter implements Filter {
    private static final Logger logger = Logger.getLogger(ValidationFilter.class.getName());

    public static final String VALIDATION_KEY_CONFIG_PROP = "validationKey";
    public static final String VALIDATION_PARAM_NAME = "validation";

    protected ConfigManager configManager;

    public void init(FilterConfig filterConfig) throws ServletException {
        configManager = Defaults.newConfigManager();
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        if (validateRequest((HttpServletRequest)request)) {
            filterChain.doFilter(request, response);
        } else {
            ((HttpServletResponse)response).sendError(HttpServletResponse.SC_FORBIDDEN, "Request failed validation");
        }
    }

    public void destroy() { }

    protected boolean validateRequest(HttpServletRequest request) {
        String privateValidationKey = configManager.get(VALIDATION_KEY_CONFIG_PROP, "");
        if (privateValidationKey.length() > 0) {
            long now = System.currentTimeMillis();
            return ValidationUtils.isTokenValid(privateValidationKey,
                                                request.getParameter(VALIDATION_PARAM_NAME),
                                                now - (24 * 60 * 60 * 1000),
                                                now + (24 * 60 * 60 * 1000));
        }

        logger.info("Note validation for this server is not currently enabled. " +
                    "See the project README for instructions on enabling validation");
        return true;
    }
}
