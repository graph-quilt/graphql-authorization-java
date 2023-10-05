package com.intuit.graphql.authorization.enforcement;

import graphql.schema.GraphQLSchema;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections4.SetUtils;


public class AuthorizationHolder {

  private final Map<String, Map<String, Set<String>>> scopeToTypeMap;

  public AuthorizationHolder(Map<String, Map<String, Set<String>>> scopeToType) {
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
                    (oldSet, newSet) -> SetUtils.union(oldSet, newSet)
                ))));
  }
}
