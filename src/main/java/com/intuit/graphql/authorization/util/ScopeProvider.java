package com.intuit.graphql.authorization.util;

import com.intuit.graphql.authorization.enforcement.RedactionContext;

import java.util.HashSet;
import java.util.Set;

public interface ScopeProvider {

  String DEFAULT_ERROR_MESSAGE = "403 - Not authorized to access field=%s of type=%s";

  // This method needs to return a set of strings if the scopes are passed and an empty set if not passed
  default Set<String> getScopes(Object o) {
    return new HashSet<>();
  }

  default String getErrorMessage(RedactionContext redactionContext) {
    return String.format(DEFAULT_ERROR_MESSAGE,
            redactionContext.getField().getName(), redactionContext.getFieldCoordinates().getTypeName());
  }

}
