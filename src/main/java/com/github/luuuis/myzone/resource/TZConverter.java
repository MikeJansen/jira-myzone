/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.github.luuuis.myzone.resource;

import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.opensymphony.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

/**
 * This service converts dates and times between different time zones.
 */
@Consumes (MediaType.APPLICATION_JSON)
@Produces (MediaType.APPLICATION_JSON)
@Path ("convert")
public class TZConverter
{
    /**
     * A DateTZ instance with no conversion performed.
     */
    private static final DateTZ NULL_DATE_TZ = new DateTZ("");

    /**
     * Logger for this TZConverter instance.
     */
    private final Logger log = LoggerFactory.getLogger(TZConverter.class);

    /**
     * A JiraAuthenticationContext.
     */
    private final JiraAuthenticationContext authContext;

    /**
     * The JIRA application properties.
     */
    private final ApplicationProperties applicationProperties;

    /**
     * Creates a new TZConverter.
     *
     * @param applicationProperties an ApplicationProperties
     * @param authContext a JiraAuthenticationContext
     */
    public TZConverter(ApplicationProperties applicationProperties, JiraAuthenticationContext authContext)
    {
        this.applicationProperties = applicationProperties;
        this.authContext = authContext;
    }

    @POST
    public DateTZ convert(DateTZ request)
    {
        log.debug("Received date: {}", request);
        try
        {
            DateFormat parser = new SimpleDateFormat(getDateFormatString());
            parser.setTimeZone(TimeZone.getDefault());

            User user = authContext.getUser();
            if (user == null)
            {
                throw new WebApplicationException(401);
            }

            String selectedTZ = user.getPropertySet().getString(Prefs.SELECTED_TZ);
            if (selectedTZ == null)
            {
                // TODO: no TZ selected, return a link or something...
                return NULL_DATE_TZ;
            }

            SimpleDateFormat jiraDateFormat = new SimpleDateFormat(getDateFormatString());
            jiraDateFormat.setTimeZone(TimeZone.getDefault());

            TimeZone userTZ = TimeZone.getTimeZone(selectedTZ);
            SimpleDateFormat userDateFormat = new SimpleDateFormat(getDateFormatString());
            userDateFormat.setTimeZone(userTZ);

            // do the conversion
            Date dateInJiraTZ = jiraDateFormat.parse(request.getTime());
            String dateInUserTZ = userDateFormat.format(dateInJiraTZ);

            // return a date string w/ TZ info
            return new DateTZ(String.format("%s %s", dateInUserTZ, userTZ.getDisplayName(true, TimeZone.SHORT)));
        }
        catch (ParseException e)
        {
            log.error("Unable to convert date: {}", request);
            return NULL_DATE_TZ;
        }
    }

    /**
     * Returns the date format string used by JIRA.
     *
     * @return a String specifying the date format used by JIRA
     */
    protected String getDateFormatString()
    {
        return applicationProperties.getDefaultBackedString(APKeys.JIRA_LF_DATE_COMPLETE);
    }
}