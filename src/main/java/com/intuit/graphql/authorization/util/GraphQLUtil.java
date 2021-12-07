package com.intuit.graphql.authorization.util;

import static graphql.Assert.assertNotNull;
import static graphql.Assert.assertShouldNeverHappen;

import graphql.introspection.Introspection;
import graphql.language.Document;
import graphql.language.OperationDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.GraphQLUnmodifiedType;

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

  //TODO: add empty and NULL protection
  //TODO: can there be multiple operations?
  public static OperationDefinition getOperationFromDocument(Document document) {
    return (OperationDefinition) document.getDefinitions().get(0);
  }

  public static boolean isOperationType(GraphQLType type, GraphQLSchema schema) {
    return type == schema.getQueryType() || type == schema.getMutationType() || type == schema.getSubscriptionType();
  }


  public static boolean isReservedSchemaType(GraphQLType type) {
    GraphQLUnmodifiedType unwrapped = GraphQLTypeUtil.unwrapAll(type);
    return unwrapped.getName().startsWith("__");
  }

  public static boolean isIntrospection__Type(GraphQLType type) {
    GraphQLType unwrappedType = GraphQLTypeUtil.unwrapAll(type);
    return unwrappedType == Introspection.__Type;
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
