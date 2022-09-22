package com.intuit.graphql.authorization.rules;

import graphql.language.Field;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLFieldsContainer;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;

public class InvalidFieldsCollector implements RuleParserListener {

  private final List<FieldCoordinates> invalidField = new ArrayList<>();

  @Override
  public void onQueryParsingError(GraphQLFieldsContainer parentType, Field field) {
    invalidField.add(FieldCoordinates.coordinates(parentType.getName(), field.getName()));
  }

  public boolean hasInvalidFields() {
    return CollectionUtils.isNotEmpty(invalidField);
  }

  public List<FieldCoordinates> getInvalidFields() {
    return this.invalidField;
  }
}
