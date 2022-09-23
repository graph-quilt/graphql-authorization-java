package com.intuit.graphql.authorization.rules;

import com.intuit.graphql.authorization.util.FieldCoordinatesFormattingUtil;
import graphql.language.Field;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLFieldsContainer;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;

public class InvalidFieldsCollector implements RuleParserListener {

  private final Set<FieldCoordinates> invalidFields = new HashSet<>();

  @Override
  public void onQueryParsingError(GraphQLFieldsContainer parentType, Field field) {
    invalidFields.add(FieldCoordinates.coordinates(parentType.getName(), field.getName()));
  }

  public boolean hasInvalidFields() {
    return CollectionUtils.isNotEmpty(invalidFields);
  }

  public Set<FieldCoordinates> getInvalidFields() {
    return this.invalidFields;
  }

  public String getInvalidFieldsAsString() {
    return FieldCoordinatesFormattingUtil.toString(this.invalidFields);
  }
}
