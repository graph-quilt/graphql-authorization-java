package com.intuit.graphql.authorization.enforcement;

import graphql.ExecutionInput;
import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.execution.ExecutionContext;
import graphql.schema.GraphQLSchema;

/**
 * This interface provides a customizable way to listen to various execution steps of query authorization.
 */
public interface AuthzListener {

  /**
   * This will be called just before a query field is redacted for unauthorized access. It gives the execution context
   * and queryVisitorFieldEnvironment as metadata of the field being redacted.
   */
  void onFieldRedaction(final ExecutionContext executionContext,
      final QueryVisitorFieldEnvironment queryVisitorFieldEnvironment);

  /**
   * This will be called just before creating authz instrumentation state.
   *
   * @param isEnforce boolean representing if policy will be enforced.
   * @param schema the graphql schema.
   * @param executionInput the execution input.
   */
  void onCreatingState(final boolean isEnforce, final GraphQLSchema schema, final ExecutionInput executionInput);

  /**
   * This will be called after enforcing authz policy on the execution input if applicable.
   *
   * @param isEnforce boolean representing if policy will be enforced.
   * @param originalExecutionContext execution context before authz policy is enforced.
   * @param enforcedExecutionContext execution context after authz policy is enforced.
   */
  void onEnforcement(final boolean isEnforce, final ExecutionContext originalExecutionContext,
      final ExecutionContext enforcedExecutionContext);
}
