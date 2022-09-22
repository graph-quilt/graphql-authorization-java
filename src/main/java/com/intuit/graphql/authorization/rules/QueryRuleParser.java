package com.intuit.graphql.authorization.rules;

import static com.intuit.graphql.authorization.util.GraphQLUtil.getFieldDefinition;
import static com.intuit.graphql.authorization.util.GraphQLUtil.isNotEmpty;

import com.intuit.graphql.authorization.util.GraphQLUtil;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.language.SelectionSet;
import graphql.parser.Parser;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class QueryRuleParser implements RuleParser {

  private static final String ERR_MSG = "Unknown field '%s'";

  private final GraphQLSchema schema;

  public QueryRuleParser(GraphQLSchema schema) {
    this.schema = Objects.requireNonNull(schema);
  }

  private void preOrder(GraphQLType graphQLOutputType, SelectionSet selectionSet,
      Map<String, Set<String>> typeToFieldMap, InvalidFieldsCollector invalidFieldsCollector) {
    if (graphQLOutputType instanceof GraphQLFieldsContainer && isNotEmpty(selectionSet)) {
      GraphQLFieldsContainer parentType = (GraphQLFieldsContainer) graphQLOutputType;
      selectionSet.getSelections()
          .forEach(node -> {
            if (node instanceof Field) {
              Field field = (Field) node;
              final GraphQLFieldDefinition fieldDefinition = getFieldDefinition(parentType,
                  field.getName());
              if (fieldDefinition == null) {
                invalidFieldsCollector.onQueryParsingError(parentType, field);
              } else {
                Set<String> fields = typeToFieldMap
                    .computeIfAbsent(parentType.getName(), k -> new HashSet<>());
                fields.add(fieldDefinition.getName());
                preOrder(GraphQLTypeUtil.unwrapAll(fieldDefinition.getType()),
                    field.getSelectionSet(), typeToFieldMap, invalidFieldsCollector);
              }
            }
          });
    }
  }

  @Override
  public Map<String, Set<String>> parseRule(final String query, InvalidFieldsCollector invalidFieldsCollector) {
    Map<String, Set<String>> typeToFieldMap = new HashMap<>();
    Document document = new Parser().parseDocument(query);
    document.getDefinitions()
        .forEach(definition -> {
          if (definition instanceof OperationDefinition) {
            OperationDefinition operationDefinition = (OperationDefinition) definition;
            GraphQLOutputType operationType = GraphQLUtil.getRootTypeFromOperation(operationDefinition, schema);
            preOrder(operationType, operationDefinition.getSelectionSet(), typeToFieldMap, invalidFieldsCollector);
          }
        });
    return typeToFieldMap;
  }

}
