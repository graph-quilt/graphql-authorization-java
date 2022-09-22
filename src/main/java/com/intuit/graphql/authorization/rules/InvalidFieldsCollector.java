package com.intuit.graphql.authorization.rules;

import graphql.language.Field;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLFieldsContainer;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;

public class InvalidFieldsCollector implements RuleParserListener {

  private final Set<FieldCoordinates> invalidField = new HashSet<>();

  @Override
  public void onQueryParsingError(GraphQLFieldsContainer parentType, Field field) {
    invalidField.add(FieldCoordinates.coordinates(parentType.getName(), field.getName()));
  }

  public boolean hasInvalidFields() {
    return CollectionUtils.isNotEmpty(invalidField);
  }

  public Set<FieldCoordinates> getInvalidFields() {
    return this.invalidField;
  }
}
