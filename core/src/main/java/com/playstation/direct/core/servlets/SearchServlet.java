/*
 *  Copyright 2023 Adobe Systems Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.playstation.direct.core.servlets;

import static com.day.cq.commons.jcr.JcrConstants.NT_UNSTRUCTURED;
import static com.playstation.direct.core.utils.Constants.FORMAT_OUTPUT;
import static com.playstation.direct.core.utils.Constants.PARAM_COMPONENT_PATH;
import static com.playstation.direct.core.utils.Constants.PARAM_SEARCH_ROOT;
import static com.playstation.direct.core.utils.Constants.PLAYSTATION_READ_USER;
import static com.playstation.direct.core.utils.Constants.QB_PATH;
import static com.playstation.direct.core.utils.Constants.QB_PROPERTY_LIMIT;
import static com.playstation.direct.core.utils.Constants.QB_PROPERTY_LIMIT_FULL;
import static com.playstation.direct.core.utils.Constants.QB_TYPE;
import static com.playstation.direct.core.utils.Constants.RT_PLAYSTATION_PAGE;
import static org.apache.sling.jcr.resource.api.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;

import com.day.cq.search.result.Hit;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.playstation.direct.core.services.QueryManager;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet that searches for a component resource in a jcr root.
 * It writes the list of pages containing the resource & it's frequency into the response.
 * It is mounted for all resources of a specific Sling resource type. The
 * {@link SlingSafeMethodsServlet} shall be used for HTTP methods that are
 * idempotent.
 */
@Component(service = { Servlet.class })
@SlingServletResourceTypes(
        resourceTypes=RT_PLAYSTATION_PAGE,
        methods=HttpConstants.METHOD_GET,
        extensions="html",
        selectors="csearch")
@ServiceDescription("Playstation resource Search Servlet")
public class SearchServlet extends SlingSafeMethodsServlet {

    private static final Logger log = LoggerFactory.getLogger(SearchServlet.class);

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private QueryManager queryManager;

    @Override
    protected void doGet(final SlingHttpServletRequest req,
            final SlingHttpServletResponse resp) throws ServletException, IOException {

        final String searchRoot = req.getParameter(PARAM_SEARCH_ROOT);
        final String componentPath = req.getParameter(PARAM_COMPONENT_PATH);
        PrintWriter writer = resp.getWriter();

        if (StringUtils.isBlank(searchRoot) || StringUtils.isBlank(componentPath)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writer.println("Invalid input");
            writer.flush();
            return;
        }

        final Map<String, Object> authInfo = Collections.singletonMap(
            ResourceResolverFactory.SUBSERVICE, PLAYSTATION_READ_USER);

        try (ResourceResolver resourceResolver = resolverFactory.getServiceResourceResolver(authInfo)) {
            PageManager pageManager = resourceResolver.adaptTo(PageManager.class);

            Map<String, String> queryParams = buildPredicateMap(searchRoot, componentPath);
            List<Hit> queryResult = queryManager.runQuery(resourceResolver, queryParams);
            Map<String, Integer> resultMap = new LinkedHashMap<>();
            queryResult.forEach(hit -> {

                try {
                    Page containingPage = pageManager.getContainingPage(hit.getResource());
                    int freq = 0;
                    if (resultMap.containsKey(containingPage.getPath())) {
                        freq = resultMap.get(containingPage.getPath());
                    }
                    resultMap.put(containingPage.getPath(), ++freq);
                }
                catch (RepositoryException e) {
                    log.error("RepositoryException while getting pages", e);
                }
            });

            if (resultMap.size() == 0) {
                writer.println("No result found!");
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                writer.flush();
            }
            else {
                resp.setStatus(HttpServletResponse.SC_OK);
                printOutput(req, writer, resultMap);
                writer.flush();
            }

        } catch (LoginException e) {
            log.error("[LoginException] while getting resource resolver of service user", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "[Login Exception], pls check the log for more information");
        }  catch (Exception e) {
            log.error("[Exception]", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Server Exception, pls check the log for more information");
        } finally {
            writer.close();
        }
    }


    /**
     * Sends response to the client. If formatOutput is set, it sends output in a HTML format
     * @param req
     * @param writer
     * @param resultMap
     */
    private void printOutput(SlingHttpServletRequest req, PrintWriter writer, Map<String, Integer> resultMap) {
        if (StringUtils.isNotBlank(req.getParameter(FORMAT_OUTPUT))) {
            writer.println("<table style='border:1px solid; border-collapse: collapse;'>");
            writer.println("<tr><th style='border:1px solid;'> Page Path </th><th style='border:1px solid;'>count</th></tr>");
            for (String page : resultMap.keySet()) {
                writer.println("<tr><td style='border:1px solid;'> " + page
                    + "&nbsp;</td> <td style='border:1px solid;'>" + resultMap.get(page)+"</td> </tr>");
            }
            writer.println("</table>");
        }
        else  {
            for (String page : resultMap.keySet()) {
                writer.println(page + "=" + resultMap.get(page));
            }
        }

    }

    /**
     * Builds search predicate map
     * @param searchRoot
     * @param componentPath
     * @return
     */
    private Map<String, String> buildPredicateMap(String searchRoot, String componentPath) {
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put(QB_PATH, searchRoot);
        queryParams.put(QB_TYPE, NT_UNSTRUCTURED);
        queryParams.put("property", SLING_RESOURCE_TYPE_PROPERTY);
        queryParams.put("property.value", componentPath);
        queryParams.put("orderby", "path");
        queryParams.put(QB_PROPERTY_LIMIT, QB_PROPERTY_LIMIT_FULL);

        return queryParams;
    }

}
