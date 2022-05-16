package com.intuit.graphql.authorization.rules;

import com.intuit.graphql.authorization.enforcement.HelperBuildTestSchema;
import com.intuit.graphql.authorization.util.TestStaticResources;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class QueryRuleParserTest {

  @Test
  public void testQueryRuleParserUnkownField() {
    QueryRuleParser queryRuleParser = new QueryRuleParser(
        HelperBuildTestSchema.buildSchema(TestStaticResources.TEST_SCHEMA));

    Assertions.assertThatThrownBy(() -> queryRuleParser.parseRule("{ author { id }}"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Unknown field 'author'");
  }

  @Test
  public void testQueryRuleParser() {
    QueryRuleParser queryRuleParser = new QueryRuleParser(
        HelperBuildTestSchema.buildSchema(TestStaticResources.TEST_SCHEMA));
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
