package com.intuit.graphql.authorization.rules;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLType;
import java.util.Map;
import java.util.Set;

public interface RuleParser {

  Map<GraphQLType, Set<GraphQLFieldDefinition>> parseRule(String rule);
}
