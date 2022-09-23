package com.intuit.graphql.authorization.rules;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.intuit.graphql.authorization.enforcement.HelperBuildTestSchema;
import com.intuit.graphql.authorization.util.TestStaticResources;
import graphql.language.Field;
import graphql.schema.GraphQLFieldsContainer;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class QueryRuleParserTest {

  @Mock
  private InvalidFieldsCollector invalidFieldsCollectorMock;

  @Test
  public void testQueryRuleParserOneUnknownFieldInQuery() {
    QueryRuleParser queryRuleParser = new QueryRuleParser(
        HelperBuildTestSchema.buildSchema(TestStaticResources.TEST_SCHEMA));

    final Map<String, Set<String>> graphQLTypeSetMap = queryRuleParser.parseRule("{ unknownField { id }}", invalidFieldsCollectorMock);

    Assertions.assertThat(graphQLTypeSetMap).hasSize(0);

    ArgumentCaptor<GraphQLFieldsContainer> acParentType = ArgumentCaptor.forClass(GraphQLFieldsContainer.class);
	  ArgumentCaptor<Field> acField = ArgumentCaptor.forClass(Field.class);
    verify(invalidFieldsCollectorMock, times(1)).onQueryParsingError(acParentType.capture(), acField.capture());

    Assertions.assertThat(acParentType.getValue().getName()).isEqualTo("Query");
    Assertions.assertThat(acField.getValue().getName()).isEqualTo("unknownField");

    verify(invalidFieldsCollectorMock, times(1)).onQueryParsingError(any(), any());
  }

  @Test
  public void testQueryRuleParserOneUnknownFieldAndOneValidFieldInQuery() {
    QueryRuleParser queryRuleParser = new QueryRuleParser(
        HelperBuildTestSchema.buildSchema(TestStaticResources.TEST_SCHEMA));

    final Map<String, Set<String>> graphQLTypeSetMap = queryRuleParser.parseRule("{ unknownField { id } bookById { id } }", invalidFieldsCollectorMock);

    Assertions.assertThat(graphQLTypeSetMap).hasSize(2);
    final Set<String> query = getFromMap(graphQLTypeSetMap, "Query");
    Assertions.assertThat(query)
        .hasSize(1);

    Assertions.assertThat(query)
        .containsOnlyOnce("bookById");

    final Set<String> book = getFromMap(graphQLTypeSetMap, "Book");
    Assertions.assertThat(book)
        .hasSize(1);

    Assertions.assertThat(book)
        .containsOnlyOnce("id");


    ArgumentCaptor<GraphQLFieldsContainer> acParentType = ArgumentCaptor.forClass(GraphQLFieldsContainer.class);
	  ArgumentCaptor<Field> acField = ArgumentCaptor.forClass(Field.class);
    verify(invalidFieldsCollectorMock, times(1)).onQueryParsingError(acParentType.capture(), acField.capture());

    Assertions.assertThat(acParentType.getValue().getName()).isEqualTo("Query");
    Assertions.assertThat(acField.getValue().getName()).isEqualTo("unknownField");

    verify(invalidFieldsCollectorMock, times(1)).onQueryParsingError(any(), any());
  }

  @Test
  public void testQueryRuleParserMultipleUnknownFieldInQuery() {
    QueryRuleParser queryRuleParser = new QueryRuleParser(
        HelperBuildTestSchema.buildSchema(TestStaticResources.TEST_SCHEMA));

    String testQuery = "{ unknownField1 { child1 } unknownField2 { child2 } }";
    final Map<String, Set<String>> graphQLTypeSetMap = queryRuleParser.parseRule(testQuery, invalidFieldsCollectorMock);

    Assertions.assertThat(graphQLTypeSetMap).hasSize(0);

    ArgumentCaptor<GraphQLFieldsContainer> acParentType = ArgumentCaptor.forClass(GraphQLFieldsContainer.class);
	  ArgumentCaptor<Field> acField = ArgumentCaptor.forClass(Field.class);
    verify(invalidFieldsCollectorMock, times(2)).onQueryParsingError(acParentType.capture(), acField.capture());

    List<GraphQLFieldsContainer> actualFieldContainers = acParentType.getAllValues();
    List<Field> actualFields = acField.getAllValues();
    Assertions.assertThat(actualFieldContainers.get(0).getName()).isEqualTo("Query");
    Assertions.assertThat(actualFieldContainers.get(1).getName()).isEqualTo("Query");
    Assertions.assertThat(actualFields.get(0).getName()).isEqualTo("unknownField1");
    Assertions.assertThat(actualFields.get(1).getName()).isEqualTo("unknownField2");

    verify(invalidFieldsCollectorMock, times(2)).onQueryParsingError(any(), any());
  }

  @Test
  public void testQueryRuleParser() {
    QueryRuleParser queryRuleParser = new QueryRuleParser(
        HelperBuildTestSchema.buildSchema(TestStaticResources.TEST_SCHEMA));
    final Map<String, Set<String>> graphQLTypeSetMap = queryRuleParser
        .parseRule(TestStaticResources.TEST_RULE_QUERY, invalidFieldsCollectorMock);

    Assertions.assertThat(graphQLTypeSetMap).hasSize(4);

    final Set<String> query = getFromMap(graphQLTypeSetMap, "Query");
    Assertions.assertThat(query)
        .hasSize(2);
    Assertions.assertThat(query).containsOnlyOnce("bookById", "allBooks");

    final Set<String> author = getFromMap(graphQLTypeSetMap, "Author");
    Assertions.assertThat(author)
        .hasSize(3);
    Assertions.assertThat(author)
        .containsOnlyOnce("firstName", "lastName", "__typename");

    final Set<String> rating = getFromMap(graphQLTypeSetMap, "Rating");
    Assertions.assertThat(rating)
        .hasSize(3);
    Assertions.assertThat(rating)
        .containsOnlyOnce("comments", "stars", "__typename");

    final Set<String> book = getFromMap(graphQLTypeSetMap, "Book");
    Assertions.assertThat(book)
        .hasSize(6);

    Assertions.assertThat(book)
        .containsOnlyOnce("name", "id", "author", "rating", "pageCount", "__typename");

  }

  @Test(expected = graphql.parser.InvalidSyntaxException.class)
  public void testQueryRuleParserWithInvalidQuery() {
    QueryRuleParser queryRuleParser = new QueryRuleParser(
        HelperBuildTestSchema.buildSchema(TestStaticResources.TEST_SCHEMA));

    String invalidGraphQLQuery = "not a valid graphql query";
    queryRuleParser.parseRule(invalidGraphQLQuery, invalidFieldsCollectorMock);
  }

  private Set<String> getFromMap(Map<String, Set<String>> graphQLTypeSetMap,
      String name) {

    for (Entry<String, Set<String>> entry : graphQLTypeSetMap.entrySet()) {
      String key = entry.getKey();
      Set<String> value = entry.getValue();
      if (key.equals(name)) {
        return value;
      }
    }
    return new HashSet<>();
  }


}
