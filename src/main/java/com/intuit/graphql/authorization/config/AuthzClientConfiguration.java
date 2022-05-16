package com.intuit.graphql.authorization.config;

import java.util.List;
import java.util.Map;

public interface AuthzClientConfiguration {

  Map<AuthzClient, List<String>> getQueriesByClient();
}
