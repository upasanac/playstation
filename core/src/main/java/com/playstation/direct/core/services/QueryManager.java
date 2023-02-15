package com.playstation.direct.core.services;

import com.day.cq.search.result.Hit;
import java.util.List;
import java.util.Map;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Interface to deal with queries. Especially query builder
 */

public interface QueryManager {

  /**
   * This method runs a query given a map of parameters and returns a List of Hit.
   *
   * @param resourceResolver
   * @param queryParams A Map of query parameters used to run query
   * @return List<Hit>
   */
  List<Hit> runQuery(ResourceResolver resourceResolver, Map<String, String> queryParams);

}
