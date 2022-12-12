package com.intuit.graphql.authorization.extension;

import com.intuit.graphql.authorization.enforcement.AuthzInstrumentation;
import graphql.execution.ExecutionContext;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;

/**
 * This class provides capability to customize the creation of {@link AuthorizationExtension} during
 * {@link AuthzInstrumentation#instrumentExecutionContext(ExecutionContext,
 * InstrumentationExecutionParameters)}. This is useful if the authorization extension logic
 * requires data such as from HTTP headers, parameters, etc. which can be extracted from
 * instrumentationExecutionContext method parameters if properly setup.
 */
public interface AuthorizationExtensionProvider {

  AuthorizationExtension getAuthorizationExtension(ExecutionContext executionContext,
      InstrumentationExecutionParameters parameters);

}
