package com.intuit.graphql.authorization.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.intuit.graphql.authorization.config.AuthzClient;
import com.intuit.graphql.authorization.config.AuthzClient.ClientAuthorizationType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLType;
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

    when(ruleParser.supports(any(ClientAuthorizationType.class)))
        .thenReturn(true);

    when(ruleParser.parseRule(anyString()))
        .thenThrow(new RuntimeException("boom"));

    AuthorizationHolderFactory factory = new AuthorizationHolderFactory(Collections.singleton(ruleParser));

    Map<AuthzClient, List<String>> queriesByClient = new HashMap<>();

    AuthzClient client = new AuthzClient();
    client.setId("test-id");
    client.setType(ClientAuthorizationType.OFFLINE);

    queriesByClient.put(client, Collections.singletonList("test-query"));

    final Map<String, Map<GraphQLType, Set<GraphQLFieldDefinition>>> result = factory
        .parse(queriesByClient);
    assertThat(result).isEmpty();
  }

  @Test
  public void mergesMultipleRules() {
    final RuleParser mockRuleParser = mock(RuleParser.class);

    when(mockRuleParser.supports(any(ClientAuthorizationType.class)))
        .thenReturn(true);

    Map<GraphQLType, Set<GraphQLFieldDefinition>> allowedTypesAndFields = new HashMap<>();
    allowedTypesAndFields.put(mock(GraphQLType.class), Collections.emptySet());

    Map<GraphQLType, Set<GraphQLFieldDefinition>> secondTypesAndFields = new HashMap<>();

    secondTypesAndFields.put(mock(GraphQLType.class), Collections.emptySet());

    when(mockRuleParser.parseRule(any()))
        .thenReturn(allowedTypesAndFields)
        .thenReturn(secondTypesAndFields);

    AuthorizationHolderFactory factory = new AuthorizationHolderFactory(Collections.singleton(mockRuleParser));

    Map<AuthzClient, List<String>> queriesByClient = new HashMap<>();
    AuthzClient client = new AuthzClient();
    client.setId("test-id");
    client.setType(ClientAuthorizationType.OFFLINE);

    List<String> queries = Arrays.asList("test-query", "second-query");
    queriesByClient.put(client, queries);

    final Map<String, Map<GraphQLType, Set<GraphQLFieldDefinition>>> result = factory.parse(queriesByClient);

    assertThat(result.get("test-id")).hasSize(2);
  }

  @Test
  public void noRuleParsers() {
    final AuthorizationHolderFactory factory = new AuthorizationHolderFactory(Collections.emptySet());

    Map<AuthzClient, List<String>> queriesByClient = new HashMap<>();
    AuthzClient client = new AuthzClient();
    client.setId("test-id");
    client.setType(ClientAuthorizationType.OFFLINE);

    List<String> queries = Collections.singletonList("test-query");
    queriesByClient.put(client, queries);

    final Map<String, Map<GraphQLType, Set<GraphQLFieldDefinition>>> result = factory.parse(queriesByClient);

    assertThat(result).isEmpty();
  }

  @Test
  public void returnsTypeMapForValidRules() {
    final RuleParser mockRuleParser = mock(RuleParser.class);

    when(mockRuleParser.supports(any(ClientAuthorizationType.class)))
        .thenReturn(true);

    Map<GraphQLType, Set<GraphQLFieldDefinition>> allowedTypesAndFields = new HashMap<>();
    allowedTypesAndFields.put(mock(GraphQLType.class), Collections.emptySet());

    when(mockRuleParser.parseRule(any()))
        .thenReturn(allowedTypesAndFields);

    AuthorizationHolderFactory factory = new AuthorizationHolderFactory(Collections.singleton(mockRuleParser));

    Map<AuthzClient, List<String>> queriesByClient = new HashMap<>();
    AuthzClient client = new AuthzClient();
    client.setId("test-id");
    client.setType(ClientAuthorizationType.OFFLINE);

    List<String> queries = Collections.singletonList("test-query");
    queriesByClient.put(client, queries);

    final Map<String, Map<GraphQLType, Set<GraphQLFieldDefinition>>> result = factory.parse(queriesByClient);

    assertThat(result).isNotEmpty();
  }
}
