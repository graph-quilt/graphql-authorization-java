package com.intuit.graphql.authorization.rules;

import com.intuit.graphql.authorization.config.ApiScopesProperties.RuleType;
import com.intuit.graphql.authorization.config.AuthzClient.ClientAuthorizationType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLType;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RuleParser {

  @Deprecated
  Map<GraphQLType, Set<GraphQLFieldDefinition>> parseRules(List<String> values);

  Map<GraphQLType, Set<GraphQLFieldDefinition>> parseRule(String rule);

  @Deprecated
  boolean supports(RuleType ruleType);

  boolean supports(ClientAuthorizationType clientAuthorizationType);
}
