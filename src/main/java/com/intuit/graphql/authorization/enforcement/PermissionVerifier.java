package com.intuit.graphql.authorization.enforcement;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLType;


public interface PermissionVerifier {

  default boolean isPermitted(GraphQLType graphQLType) {
    return false;
  }

  default boolean isPermitted(GraphQLType graphQLType, GraphQLFieldDefinition fieldDefinition) {
    return false;
  }
}

