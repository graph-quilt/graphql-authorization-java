package com.intuit.graphql.authorization.enforcement;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class AuthorizationHolder {

  private final Map<String, Map<GraphQLType, Set<GraphQLFieldDefinition>>> scopeToTypeMap;

  public AuthorizationHolder(Map<String, Map<GraphQLType, Set<GraphQLFieldDefinition>>> scopeToType) {
    this.scopeToTypeMap = Collections.unmodifiableMap(scopeToType);
  }


  public TypeFieldPermissionVerifier getPermissionsVerifier(Set<String> scopes, GraphQLSchema schema) {

    return new TypeFieldPermissionVerifier(schema,
        Collections.unmodifiableMap(
            scopes.stream()
                .map(scopeToTypeMap::get)
                .filter(Objects::nonNull)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue,
                    (oldSet, newSet) -> Collections.unmodifiableSet(
                        Stream.concat(oldSet.stream(), newSet.stream())
                            .collect(Collectors.toSet()))))));
  }

}
