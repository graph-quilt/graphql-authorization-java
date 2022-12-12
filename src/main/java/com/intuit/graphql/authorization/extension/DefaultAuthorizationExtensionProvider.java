package com.intuit.graphql.authorization.extension;

import graphql.execution.ExecutionContext;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;

public class DefaultAuthorizationExtensionProvider implements
    AuthorizationExtensionProvider {

  private static final AuthorizationExtension DEFAULT_AUTH_EXTENSION = new DefaultAuthorizationExtension();

  @Override
  public AuthorizationExtension getAuthorizationExtension(ExecutionContext executionContext,
      InstrumentationExecutionParameters parameters) {
    return DEFAULT_AUTH_EXTENSION;
  }
}
