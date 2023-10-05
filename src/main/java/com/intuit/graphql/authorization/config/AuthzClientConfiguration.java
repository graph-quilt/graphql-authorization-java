package com.intuit.graphql.authorization.config;

import java.util.List;
import java.util.Map;

/**
 * This client represents the configuration that is needed to initialize the
 * {@link com.intuit.graphql.authorization.enforcement.AuthzInstrumentation} class. It represnts a map of your client
 * against the list of queries that defines the access control.
 */
public interface AuthzClientConfiguration {

  /**
   * Provide the access control map
   */
  Map<AuthzClient, List<String>> getQueriesByClient();
}
