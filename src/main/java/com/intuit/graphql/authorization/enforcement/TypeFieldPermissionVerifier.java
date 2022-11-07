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
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;

public class TypeFieldPermissionVerifier implements PermissionVerifier {

  @Getter
  private final Map<String, Set<String>> typeToFieldsMap;
  private final GraphQLSchema schema;

  TypeFieldPermissionVerifier(GraphQLSchema schema,
      Map<String, Set<String>> typeToFieldsMap) {
    this.typeToFieldsMap = typeToFieldsMap;
    this.schema = schema;
  }

  @Override
  public boolean isPermitted(GraphQLNamedType graphQLType) {
    return isTypeSpecial(graphQLType) || typeToFieldsMap.containsKey(graphQLType.getName());
  }

  @Override
  public boolean isPermitted(GraphQLNamedType parentType, GraphQLFieldDefinition fieldDefinition) {
    if (isTypeSpecial(parentType)) {
      return true;
    }
    final GraphQLNamedType type = unwrapAll(fieldDefinition.getType());
    if (parentType == schema.getQueryType() && type == Introspection.__Schema) {
      return true;
    }
    Set<String> fields = typeToFieldsMap.getOrDefault(parentType.getName(), SetUtils.emptySet());
    //allow __typename, if at least one field is allowed.
    if (fieldDefinition == Introspection.TypeNameMetaFieldDef) {
      return CollectionUtils.isNotEmpty(fields);
    }
    return fields.contains(fieldDefinition.getName());
  }

  private boolean isTypeSpecial(GraphQLNamedType parentType) {
    //input types are permitted
    //schema types are permitted
    return GraphQLUtil.isReservedSchemaType(parentType) || GraphQLTypeUtil.isInput(parentType);
  }
}


