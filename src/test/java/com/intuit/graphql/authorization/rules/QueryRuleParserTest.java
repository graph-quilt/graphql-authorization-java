package com.intuit.graphql.authorization.rules;

import com.intuit.graphql.authorization.enforcement.HelperBuildTestSchema;
import com.intuit.graphql.authorization.util.TestStaticResources;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLType;
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
    final Map<GraphQLType, Set<GraphQLFieldDefinition>> graphQLTypeSetMap = queryRuleParser
        .parseRule(TestStaticResources.TEST_RULE_QUERY);

    Assertions.assertThat(graphQLTypeSetMap).hasSize(4);

    final Set<GraphQLFieldDefinition> query = getFromMap(graphQLTypeSetMap, "Query");
    Assertions.assertThat(query)
        .hasSize(1).hasOnlyOneElementSatisfying(f -> f.getName().equals("bookById"));

    final Set<GraphQLFieldDefinition> author = getFromMap(graphQLTypeSetMap, "Author");
    Assertions.assertThat(author)
        .hasSize(2);
    Assertions.assertThat(author)
        .extracting(f -> f.getName()).containsOnlyOnce("firstName", "lastName");

    final Set<GraphQLFieldDefinition> rating = getFromMap(graphQLTypeSetMap, "Rating");
    Assertions.assertThat(rating)
        .hasSize(2);
    Assertions.assertThat(rating)
        .extracting(f -> f.getName()).containsOnlyOnce("comments", "stars");

    final Set<GraphQLFieldDefinition> book = getFromMap(graphQLTypeSetMap, "Book");
    Assertions.assertThat(book)
        .hasSize(5);

    Assertions.assertThat(book)
        .extracting(f -> f.getName()).containsOnlyOnce("name", "id", "author", "rating", "pageCount");

  }

  private Set<GraphQLFieldDefinition> getFromMap(Map<GraphQLType, Set<GraphQLFieldDefinition>> graphQLTypeSetMap,
      String name) {

    for (Entry<GraphQLType, Set<GraphQLFieldDefinition>> entry : graphQLTypeSetMap.entrySet()) {
      GraphQLType key = entry.getKey();
      Set<GraphQLFieldDefinition> value = entry.getValue();
      if (((GraphQLFieldsContainer) key).getName().equals(name)) {
        return value;
      }
    }
    return new HashSet<>();
  }


}
