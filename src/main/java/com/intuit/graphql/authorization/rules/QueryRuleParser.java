package com.intuit.graphql.authorization.rules;

import com.intuit.graphql.authorization.config.AuthzClient.ClientAuthorizationType;
import graphql.analysis.QueryTraverser;
import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.analysis.QueryVisitorStub;
import graphql.language.Document;
import graphql.parser.Parser;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class QueryRuleParser implements RuleParser {

  private final GraphQLSchema schema;

  public QueryRuleParser(GraphQLSchema schema) {
    this.schema = Objects.requireNonNull(schema);
  }


  private Map<GraphQLType, Set<GraphQLFieldDefinition>> getTypesAndFieldsMap(String query) {
    Document document = new Parser().parseDocument(query);

    Map<GraphQLType, Set<GraphQLFieldDefinition>> typeToFieldMap = new HashMap<>();

    QueryTraverser queryTraverser = QueryTraverser.newQueryTraverser()
        .schema(schema)
        .document(document)
        .variables(new HashMap<>())
        .build();

    queryTraverser.visitPreOrder(new QueryVisitorStub() {
      @Override
      public void visitField(QueryVisitorFieldEnvironment env) {
        GraphQLType type = env.getParentType();
        GraphQLFieldDefinition fieldDefinition = env.getFieldDefinition();

        Set<GraphQLFieldDefinition> fields = typeToFieldMap.computeIfAbsent(type, k -> new HashSet<>());
        fields.add(fieldDefinition);
      }
    });

    return typeToFieldMap;
  }

  @Override
  public Map<GraphQLType, Set<GraphQLFieldDefinition>> parseRules(List<String> queries) {
    try {
      return queries.stream()
          .flatMap(query -> parseRule(query).entrySet().stream())
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
              (oldSet, newSet) -> Collections.unmodifiableSet(
                  Stream.concat(oldSet.stream(), newSet.stream())
                      .collect(Collectors.toSet()))));
    } catch (Exception e) {
      log.error("Failed to parse rule " + queries, e);
      throw e;
    }

  }

  @Override
  public Map<GraphQLType, Set<GraphQLFieldDefinition>> parseRule(final String query) {
    return getTypesAndFieldsMap(query);
  }

  @Override
  public boolean supports(final ClientAuthorizationType clientAuthorizationType) {
    return clientAuthorizationType == ClientAuthorizationType.OFFLINE
        || clientAuthorizationType == ClientAuthorizationType.PRIVATE_AUTH_PLUS;
  }
}
