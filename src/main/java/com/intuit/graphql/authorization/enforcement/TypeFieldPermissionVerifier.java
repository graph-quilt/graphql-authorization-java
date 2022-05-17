package com.intuit.graphql.authorization.enforcement;

import static graphql.schema.GraphQLTypeUtil.unwrapAll;

import com.intuit.graphql.authorization.util.GraphQLUtil;
import graphql.introspection.Introspection;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLTypeUtil;
import java.util.Map;
import java.util.Set;

public class TypeFieldPermissionVerifier implements PermissionVerifier {

  private final Map<String, Set<String>> typeToFieldsMap;
  private final GraphQLSchema schema;

  TypeFieldPermissionVerifier(GraphQLSchema schema,
      Map<String, Set<String>> typeToFieldsMap) {
    this.typeToFieldsMap = typeToFieldsMap;
    this.schema = schema;
  }

  @Override
  public boolean isPermitted(GraphQLNamedType graphQLType) {
    //input types are permitted
    if (GraphQLTypeUtil.isInput(graphQLType)) {
      return true;
    }
    //operation itself is permitted
//    if (GraphQLUtil.isOperationType(graphQLType, schema)) {
//      return true;
//    }
    //check if this is a schema type - they are permitted
    if (GraphQLUtil.isReservedSchemaType(graphQLType)) {
      return true;
    }
    return typeToFieldsMap.containsKey(graphQLType.getName());
  }

  @Override
  public boolean isPermitted(GraphQLNamedType parentType, GraphQLFieldDefinition fieldDefinition) {
    if (GraphQLUtil.isReservedSchemaType(parentType)) {
      return true;
    }
    if (GraphQLTypeUtil.isInput(parentType)) {
      return true;
    }
    final GraphQLNamedType type = unwrapAll(fieldDefinition.getType());
    if (parentType == schema.getQueryType() && type == Introspection.__Schema) {
      return true;
    }
    Set<String> fields = typeToFieldsMap.get(parentType.getName());
    boolean fieldAllowed = fields != null && fields.contains(fieldDefinition.getName());
    boolean typeAllowed = isPermitted(type);
    return fieldAllowed && typeAllowed;
  }
}
