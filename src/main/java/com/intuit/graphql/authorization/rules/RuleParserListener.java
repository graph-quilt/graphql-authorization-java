package com.intuit.graphql.authorization.rules;

import graphql.language.Field;
import graphql.schema.GraphQLFieldsContainer;

public interface RuleParserListener {

  void onQueryParsingError(GraphQLFieldsContainer parentType,
      Field field);
}
