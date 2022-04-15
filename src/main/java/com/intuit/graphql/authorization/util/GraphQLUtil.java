package com.intuit.graphql.authorization.util;

import static graphql.Assert.assertNotNull;
import static graphql.Assert.assertShouldNeverHappen;
import static graphql.introspection.Introspection.INTROSPECTION_SYSTEM_FIELDS;

import graphql.introspection.Introspection;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.language.SelectionSet;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.GraphQLUnmodifiedType;
import org.apache.commons.collections4.CollectionUtils;

public class GraphQLUtil {

  //hides public constructor
  private GraphQLUtil() {
  }

  public static GraphQLObjectType getRootTypeFromOperation(OperationDefinition operationDefinition,
      GraphQLSchema schema) {
    switch (operationDefinition.getOperation()) {
      case MUTATION:
        return assertNotNull(schema.getMutationType());
      case QUERY:
        return assertNotNull(schema.getQueryType());
      case SUBSCRIPTION:
        return assertNotNull(schema.getSubscriptionType());
      default:
        return assertShouldNeverHappen();
    }
  }

  public static boolean isOperationType(GraphQLType type, GraphQLSchema schema) {
    return type == schema.getQueryType() || type == schema.getMutationType() || type == schema.getSubscriptionType();
  }

  public static boolean isReservedSchemaType(GraphQLType type) {
    GraphQLUnmodifiedType unwrapped = GraphQLTypeUtil.unwrapAll(type);
    return unwrapped.getName().startsWith("__");
  }

  public static GraphQLFieldDefinition getFieldDefinition(GraphQLFieldsContainer graphQLFieldsContainer, String fieldName) {
    if (Introspection.TypeNameMetaFieldDef.getName().equals(fieldName)) {
      return Introspection.TypeNameMetaFieldDef;
    }
    return graphQLFieldsContainer.getFieldDefinition(fieldName);
  }

  public static boolean isIntrospection__Type(GraphQLType type) {
    GraphQLType unwrappedType = GraphQLTypeUtil.unwrapAll(type);
    return unwrappedType == Introspection.__Type;
  }

  public static boolean isNotEmpty(SelectionSet selectionSet) {
    return selectionSet != null && CollectionUtils.isNotEmpty(selectionSet.getSelections());
  }

  public static boolean isListOfIntrospection__Type(GraphQLType type) {
    if (GraphQLTypeUtil.isNonNull(type)) {
      return isListOfIntrospection__Type(GraphQLTypeUtil.unwrapOne(type));
    }
    if (GraphQLTypeUtil.isList(type)) {
      return isIntrospection__Type(type);
    }
    return false;
  }
}
