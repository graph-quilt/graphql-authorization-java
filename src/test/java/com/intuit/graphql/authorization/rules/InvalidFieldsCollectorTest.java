package com.intuit.graphql.authorization.rules;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import graphql.language.Field;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLFieldsContainer;
import java.util.Set;
import org.junit.Test;

public class InvalidFieldsCollectorTest {

  private InvalidFieldsCollector subjectUnderTest = new InvalidFieldsCollector();

  @Test
  public void collectsInvalidFieldsTest() {
      GraphQLFieldsContainer parentTypeMock = mock(GraphQLFieldsContainer.class);
      when(parentTypeMock.getName()).thenReturn("ParentTypeName");
      Field fieldMock = mock(Field.class);
      when(fieldMock.getName()).thenReturn("fieldName");

     subjectUnderTest.onQueryParsingError(parentTypeMock, fieldMock);

     Set<FieldCoordinates> actualInvalidField = subjectUnderTest.getInvalidFields();
     assertThat(actualInvalidField).containsOnly(FieldCoordinates.coordinates("ParentTypeName","fieldName"));
  }

}
