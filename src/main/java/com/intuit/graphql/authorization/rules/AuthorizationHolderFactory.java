package com.intuit.graphql.authorization.rules;

import com.intuit.graphql.authorization.config.AuthzClient;
import com.intuit.graphql.authorization.util.FieldCoordinatesFormattingUtil;
import graphql.schema.FieldCoordinates;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class AuthorizationHolderFactory {

  private final RuleParser ruleParser;

  public AuthorizationHolderFactory(RuleParser ruleParser) {
    this.ruleParser = Objects.requireNonNull(ruleParser);
  }

  public Map<String, Map<String, Set<String>>> parse(
      Map<AuthzClient, List<String>> graphqlRulesByClient
  ) {
    Map<String, Map<String, Set<String>>> scopeToTypeMap = new HashMap<>();

    for (Entry<AuthzClient, List<String>> entry : graphqlRulesByClient.entrySet()) {
      AuthzClient authzClient = entry.getKey();
      List<String> queries = entry.getValue();
      String id = authzClient.getId();

      Map<String, Set<String>> intermediateResults = new HashMap<>();

      InvalidFieldsCollector invalidFieldsCollector = new InvalidFieldsCollector();

      for (final String query : queries) {
        try {
          Map<String, Set<String>> ruleSetMap = ruleParser.parseRule(query, invalidFieldsCollector);
          ruleSetMap.forEach((type, fields) -> intermediateResults.merge(type, fields, (oldSet, newSet) -> {
            oldSet.addAll(newSet);
            return oldSet;
          }));
        } catch (Exception e) {
          log.error("Failed to parse rule for scope " + id, e);
        }
      }

      if (invalidFieldsCollector.hasInvalidFields()) {
        log.error(String.format("Invalid fields found in query rule.  clientId=%s, invalidFields=%s",
          id, invalidFieldsCollector.getInvalidFieldsAsString()));
      }

      if (!intermediateResults.isEmpty()) {
        scopeToTypeMap.put(id, intermediateResults);
      }
    }

    log.info("Parsed rules for scopes " + scopeToTypeMap.keySet());
    return Collections.unmodifiableMap(scopeToTypeMap);
  }

}

