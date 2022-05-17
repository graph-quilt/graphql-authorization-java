package com.intuit.graphql.authorization.enforcement;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLNamedType;


public interface PermissionVerifier {

  default boolean isPermitted(GraphQLNamedType graphQLType) {
    return false;
  }

  default boolean isPermitted(GraphQLNamedType graphQLType, GraphQLFieldDefinition fieldDefinition) {
    return false;
  }
}

