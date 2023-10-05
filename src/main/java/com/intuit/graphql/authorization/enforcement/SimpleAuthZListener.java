package com.intuit.graphql.authorization.enforcement;

import graphql.ExecutionInput;
import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.execution.ExecutionContext;
import graphql.schema.GraphQLSchema;

public class SimpleAuthZListener implements AuthzListener {

  @Override
  public void onFieldRedaction(ExecutionContext executionContext,
      QueryVisitorFieldEnvironment queryVisitorFieldEnvironment) {
    //do nothing
  }

  @Override
  public void onCreatingState(GraphQLSchema schema, ExecutionInput executionInput) {
    //do nothing
  }

  @Override
  public void onEnforcement(ExecutionContext originalExecutionContext,
      ExecutionContext enforcedExecutionContext) {
    //do nothing
  }
}
