package com.intuit.graphql.authorization.enforcement;

import com.intuit.graphql.authorization.util.GraphQLUtil;
import graphql.introspection.Introspection;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;
import java.util.Map;
import java.util.Set;

public class TypeFieldPermissionVerifier implements PermissionVerifier {

  private final Map<GraphQLType, Set<GraphQLFieldDefinition>> typeToFieldsMap;
  private final GraphQLSchema schema;

  TypeFieldPermissionVerifier(GraphQLSchema schema,
      Map<GraphQLType, Set<GraphQLFieldDefinition>> typeToFieldsMap) {
    this.typeToFieldsMap = typeToFieldsMap;
    this.schema = schema;
  }

  @Override
  public boolean isPermitted(GraphQLType graphQLType) {
    //input types are permitted
    if (GraphQLTypeUtil.isInput(graphQLType)) {
      return true;
    }
    //operation itself is permitted
    if (GraphQLUtil.isOperationType(graphQLType, schema)) {
      return true;
    }
    //check if this is a schema type - they are permitted
    if (GraphQLUtil.isReservedSchemaType(graphQLType)) {
      return true;
    }
    return typeToFieldsMap.containsKey(graphQLType);
  }

  @Override
  public boolean isPermitted(GraphQLType graphQLType, GraphQLFieldDefinition fieldDefinition) {
    if (GraphQLUtil.isReservedSchemaType(graphQLType)) {
      return true;
    }
    if (GraphQLTypeUtil.isInput(graphQLType)) {
      return true;
    }
    if (graphQLType == schema.getQueryType()
        && GraphQLTypeUtil.unwrapAll(fieldDefinition.getType()) == Introspection.__Schema) {
      return true;
    }
    Set<GraphQLFieldDefinition> fields = typeToFieldsMap.get(graphQLType);
    return fields != null && fields.contains(fieldDefinition);
  }
}
