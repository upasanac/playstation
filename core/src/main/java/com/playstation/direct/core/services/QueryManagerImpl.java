package com.playstation.direct.core.services;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import java.util.List;
import java.util.Map;
import javax.jcr.Session;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;

@Component(service = {QueryManager.class}
)
public class QueryManagerImpl implements QueryManager {

  @Override
  public List<Hit> runQuery(ResourceResolver resourceResolver, Map<String, String> queryParams) {
    QueryBuilder queryBuilder = resourceResolver.adaptTo(QueryBuilder.class);
    Session session = resourceResolver.adaptTo(Session.class);
    Query query = queryBuilder.createQuery(PredicateGroup.create(queryParams), session);
    return query.getResult().getHits();
  }
}
