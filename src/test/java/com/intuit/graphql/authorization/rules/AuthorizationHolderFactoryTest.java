package com.intuit.graphql.authorization.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intuit.graphql.authorization.config.AuthzClient;
import com.intuit.graphql.authorization.config.AuthzClient.ClientAuthorizationType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class AuthorizationHolderFactoryTest {

  @Test
  public void failsToParseRules() {
    RuleParser ruleParser = mock(RuleParser.class);

    when(ruleParser.parseRule(anyString(), any(InvalidFieldsCollector.class)))
        .thenThrow(new RuntimeException("boom"));

    AuthorizationHolderFactory factory = new AuthorizationHolderFactory(ruleParser);

    Map<AuthzClient, List<String>> queriesByClient = new HashMap<>();

    AuthzClient client = new AuthzClient();
    client.setId("test-id");
    client.setType(ClientAuthorizationType.OFFLINE);

    queriesByClient.put(client, Collections.singletonList("test-query"));

    final Map<String, Map<String, Set<String>>> result = factory
        .parse(queriesByClient);
    assertThat(result).isEmpty();
  }

  @Test
  public void unknownFieldDuringParseRule() {
    GraphQLObjectType queryTypeMock = mock(GraphQLObjectType.class);

    GraphQLSchema graphQLSchemaMock = mock(GraphQLSchema.class);
    when(graphQLSchemaMock.getQueryType()).thenReturn(queryTypeMock);

    QueryRuleParser ruleParser = new QueryRuleParser(graphQLSchemaMock);
    AuthorizationHolderFactory factory = new AuthorizationHolderFactory(ruleParser);

    Map<AuthzClient, List<String>> queriesByClient = new HashMap<>();

    AuthzClient client = new AuthzClient();
    client.setId("test-id");
    client.setType(ClientAuthorizationType.OFFLINE);

    queriesByClient.put(client, Collections.singletonList("query { thisIsInValidField }"));

    final Map<String, Map<String, Set<String>>> result = factory
        .parse(queriesByClient);
    assertThat(result).isEmpty();
  }

  @Test
  public void mergesMultipleRules() {
    final RuleParser mockRuleParser = mock(RuleParser.class);

    Map<String, Set<String>> allowedTypesAndFields = new HashMap<>();
    allowedTypesAndFields.put("type1", Collections.emptySet());

    Map<String, Set<String>> secondTypesAndFields = new HashMap<>();

    secondTypesAndFields.put("type2", Collections.emptySet());

    when(mockRuleParser.parseRule(any(), any(InvalidFieldsCollector.class)))
        .thenReturn(allowedTypesAndFields)
        .thenReturn(secondTypesAndFields);

    AuthorizationHolderFactory factory = new AuthorizationHolderFactory(mockRuleParser);

    Map<AuthzClient, List<String>> queriesByClient = new HashMap<>();
    AuthzClient client = new AuthzClient();
    client.setId("test-id");
    client.setType(ClientAuthorizationType.OFFLINE);

    List<String> queries = Arrays.asList("test-query", "second-query");
    queriesByClient.put(client, queries);

    final Map<String, Map<String, Set<String>>> result = factory.parse(queriesByClient);

    assertThat(result.get("test-id")).hasSize(2);
  }

  @Test
  public void noRuleParsers() {
    assertThatThrownBy(() -> new AuthorizationHolderFactory(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  public void returnsTypeMapForValidRules() {
    final RuleParser mockRuleParser = mock(RuleParser.class);

    Map<String, Set<String>> allowedTypesAndFields = new HashMap<>();
    allowedTypesAndFields.put("type", Collections.emptySet());

    when(mockRuleParser.parseRule(any(), any(InvalidFieldsCollector.class)))
        .thenReturn(allowedTypesAndFields);

    AuthorizationHolderFactory factory = new AuthorizationHolderFactory(mockRuleParser);

    Map<AuthzClient, List<String>> queriesByClient = new HashMap<>();
    AuthzClient client = new AuthzClient();
    client.setId("test-id");
    client.setType(ClientAuthorizationType.OFFLINE);

    List<String> queries = Collections.singletonList("test-query");
    queriesByClient.put(client, queries);

    final Map<String, Map<String, Set<String>>> result = factory.parse(queriesByClient);

    assertThat(result).isNotEmpty();
  }
}
