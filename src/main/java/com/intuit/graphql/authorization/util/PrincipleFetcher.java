package com.intuit.graphql.authorization.util;

import static com.intuit.graphql.authorization.util.InstrumentDataFetcherAction.DEFAULT;

import com.intuit.graphql.authorization.enforcement.AuthzInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.schema.DataFetcher;
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

  /**
   * This method is called from {@link AuthzInstrumentation#instrumentDataFetcher(DataFetcher,
   * InstrumentationFieldFetchParameters)} which allow user's of this library to influence the
   * behavior of {@link AuthzInstrumentation#instrumentDataFetcher(DataFetcher,
   * InstrumentationFieldFetchParameters)}
   *
   * @param dataFetcher dataFetcher to be instrumented
   * @param parameters metadata pertaining to datafether
   * @return
   */
  default InstrumentDataFetcherAction instrumentDataFetcher(DataFetcher<?> dataFetcher,
      InstrumentationFieldFetchParameters parameters) {
    return DEFAULT;
  }

}
