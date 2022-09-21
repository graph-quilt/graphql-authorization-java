package com.intuit.graphql.authorization.rules;

import static com.intuit.graphql.authorization.rules.QueryRuleParserErrors.UNKNOWN_FIELD;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.intuit.graphql.authorization.enforcement.AuthzListener;
import com.intuit.graphql.authorization.enforcement.HelperBuildTestSchema;
import com.intuit.graphql.authorization.util.TestStaticResources;
import graphql.language.Field;
import graphql.schema.GraphQLFieldsContainer;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class QueryRuleParserTest {

  @Mock
  private AuthzListener authzListenerMock;

  @Test
  public void testQueryRuleParserUnknownField() {
    QueryRuleParser queryRuleParser = new QueryRuleParser(
        HelperBuildTestSchema.buildSchema(TestStaticResources.TEST_SCHEMA), authzListenerMock);

    final Map<String, Set<String>> graphQLTypeSetMap = queryRuleParser.parseRule("{ author { id }}");

    Assertions.assertThat(graphQLTypeSetMap).hasSize(0);
    verify(authzListenerMock, times(1)).onQueryParsingError(eq(UNKNOWN_FIELD), any(GraphQLFieldsContainer.class), any(
        Field.class));
  }

  @Test
  public void testQueryRuleParser() {
    QueryRuleParser queryRuleParser = new QueryRuleParser(
        HelperBuildTestSchema.buildSchema(TestStaticResources.TEST_SCHEMA), authzListenerMock);
    final Map<String, Set<String>> graphQLTypeSetMap = queryRuleParser
        .parseRule(TestStaticResources.TEST_RULE_QUERY);

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
