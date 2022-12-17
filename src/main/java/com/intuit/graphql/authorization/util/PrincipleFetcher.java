package com.intuit.graphql.authorization.util;

import java.util.HashSet;
import java.util.Set;

public interface PrincipleFetcher {

  // This method needs to return a set of strings if the scopes are passed and an empty set if not passed
  default Set<String> getScopes(Object o) {
    return new HashSet<>();
  }

  // This method returns if a client/appid need to exempt enforcement
  default boolean authzEnforcementExemption(Object o) {
    return false;
  }

}
