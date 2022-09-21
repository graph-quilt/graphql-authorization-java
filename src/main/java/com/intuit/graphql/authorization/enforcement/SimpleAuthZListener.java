package com.intuit.graphql.authorization.enforcement;

import com.intuit.graphql.authorization.rules.QueryRuleParserErrors;
import graphql.ExecutionInput;
import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.execution.ExecutionContext;
import graphql.language.Field;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLSchema;

public class SimpleAuthZListener implements AuthzListener {

  @Override
  public void onFieldRedaction(ExecutionContext executionContext,
      QueryVisitorFieldEnvironment queryVisitorFieldEnvironment) {
    //do nothing
  }

  @Override
  public void onCreatingState(boolean isEnforce, GraphQLSchema schema, ExecutionInput executionInput) {
    //do nothing
  }

  @Override
  public void onEnforcement(boolean isEnforce, ExecutionContext originalExecutionContext,
      ExecutionContext enforcedExecutionContext) {
    //do nothing
  }

  @Override
  public void onQueryParsingError(
      QueryRuleParserErrors error, GraphQLFieldsContainer parentType, Field field) {
    //do nothing
  }
}
