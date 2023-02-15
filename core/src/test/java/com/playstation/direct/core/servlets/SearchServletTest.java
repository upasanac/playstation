/*
 *  Copyright 2018 Adobe Systems Incorporated
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

import static com.playstation.direct.core.utils.Constants.PARAM_COMPONENT_PATH;
import static com.playstation.direct.core.utils.Constants.PARAM_SEARCH_ROOT;
import static junit.framework.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.playstation.direct.core.services.QueryManager;
import com.playstation.direct.core.services.QueryManagerImpl;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.apache.sling.testing.resourceresolver.MockResourceResolverFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchServletTest {

    private SearchServlet searchServlet;
    private MockSlingHttpServletRequest request;
    private MockSlingHttpServletResponse response;
    public static final String PLAYSTATION_CONTENT_ROOT = "/content/playstation/us/en";

    final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    @Mock
    private Session mockSession;

    @Mock
    private PageManager pageManager;

    @Mock
    public QueryBuilder queryBuilder;

    @Mock
    private Query query;

    @Mock
    private Hit hit;

    @Mock
    SearchResult searchResult;

    @Mock
    private Page page;

    @BeforeEach
    public void setup() throws Exception {
        request = context.request();
        response = context.response();
        context.load().json("/search-content.json", PLAYSTATION_CONTENT_ROOT);
        context.registerService(ResourceResolverFactory.class, new MockResourceResolverFactory());
        context.registerAdapter(ResourceResolver.class, Session.class, mockSession);
        context.registerAdapter(ResourceResolver.class, QueryBuilder.class, queryBuilder);
        context.registerAdapter(ResourceResolver.class, PageManager.class, pageManager);
        context.registerService(QueryManager.class, new QueryManagerImpl());
        searchServlet = new SearchServlet();
        context.registerInjectActivateService(searchServlet);
    }

    @Test
    public void testBadInput() throws Exception {
        context.requestPathInfo().setExtension("html");
        searchServlet.doGet(request, response);
        validateStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void testNoResult() throws Exception {
        setRequestParams(PLAYSTATION_CONTENT_ROOT, "playstation/components/non-existing");
        when(queryBuilder.createQuery(any(), any())).thenReturn(query);
        when(query.getResult()).thenReturn(searchResult);
        searchServlet.doGet(request, response);
        validateStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @Test
    public void testNotEmptyResult() throws Exception {
        setRequestParams(PLAYSTATION_CONTENT_ROOT, "playstation/components/title");
        when(queryBuilder.createQuery(any(), any())).thenReturn(query);
        when(query.getResult()).thenReturn(searchResult);
        when(searchResult.getHits()).thenReturn(Collections.singletonList(hit));

        Resource resource1 = context.currentResource("/content/playstation/us/en/page1/jcr:content/root/container/title");
        Resource resource2 = context.currentResource("/content/playstation/us/en/page3/jcr:content/root/container/container/title_1");
        when(hit.getResource()).thenReturn(resource1, resource2);
        when(pageManager.getContainingPage(any(Resource.class))).thenReturn(page);
        searchServlet.doGet(request, response);
        validateStatus(HttpServletResponse.SC_OK);
    }

    private void validateStatus(int status) {
        assertEquals(status, response.getStatus());
    }

    private void setRequestParams(String searchRoot, String componentPath) throws ServletException, IOException {
        context.requestPathInfo().setExtension("html");
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put(PARAM_SEARCH_ROOT, searchRoot);
        paramMap.put(PARAM_COMPONENT_PATH, componentPath);
        context.request().setParameterMap(paramMap);
    }
}
