package com.intuit.graphql.authorization.util;


import graphql.Scalars;
import graphql.introspection.Introspection;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.language.OperationDefinition.Operation;
import graphql.language.SelectionSet;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class GraphQLUtilTest {

  final static GraphQLFieldDefinition fieldDefinition = GraphQLFieldDefinition.newFieldDefinition().name("foo")
      .type(Scalars.GraphQLString).build();
  final static GraphQLSchema schema = GraphQLSchema.newSchema()
      .query(GraphQLObjectType.newObject().name("myQuery").field(
          fieldDefinition).build())
      .mutation(GraphQLObjectType.newObject().name("myMutation").field(
          fieldDefinition).build())
      .subscription(GraphQLObjectType.newObject().name("mySubscription").field(
          fieldDefinition).build())
      .build();

  @Test
  public void isOperationTypeTest() {
    schema.getMutationType();
    Assertions.assertThat(GraphQLUtil.isOperationType(schema.getMutationType(), schema)).isTrue();
    Assertions.assertThat(GraphQLUtil.isOperationType(schema.getQueryType(), schema)).isTrue();
    Assertions.assertThat(GraphQLUtil.isOperationType(schema.getSubscriptionType(), schema)).isTrue();
    Assertions.assertThat(GraphQLUtil.isOperationType(GraphQLObjectType.newObject().name("query").build(), schema))
        .isFalse();
  }

  @Test
  public void getRootTypeFromOperationDefinition() {
    final OperationDefinition query = OperationDefinition.newOperationDefinition().operation(Operation.QUERY)
        .build();
    final OperationDefinition mutation = OperationDefinition.newOperationDefinition().operation(Operation.MUTATION)
        .build();
    final OperationDefinition subscription = OperationDefinition.newOperationDefinition()
        .operation(Operation.SUBSCRIPTION)
        .build();
    Assertions.assertThat(GraphQLUtil.getRootTypeFromOperation(query, schema).getName()).isEqualTo("myQuery");
    Assertions.assertThat(GraphQLUtil.getRootTypeFromOperation(mutation, schema).getName()).isEqualTo("myMutation");
    Assertions.assertThat(GraphQLUtil.getRootTypeFromOperation(subscription, schema).getName())
        .isEqualTo("mySubscription");

    final OperationDefinition none = OperationDefinition.newOperationDefinition().build();
    Assertions.assertThatThrownBy(() -> GraphQLUtil.getRootTypeFromOperation(none, schema)).isInstanceOf(
        NullPointerException.class);

  }

  @Test
  public void isIntrospection__TypeTest() {
    Assertions.assertThat(GraphQLUtil.isIntrospection__Type(Introspection.__Type)).isTrue();
    Assertions.assertThat(GraphQLUtil.isIntrospection__Type(GraphQLNonNull.nonNull((Introspection.__Type)))).isTrue();
    Assertions.assertThat(
        GraphQLUtil.isIntrospection__Type(GraphQLNonNull.nonNull(GraphQLObjectType.newObject().name("fool").build())))
        .isFalse();
    Assertions.assertThat(GraphQLUtil.isIntrospection__Type(GraphQLObjectType.newObject().name("fool").build()))
        .isFalse();
  }

  @Test
  public void isListOfIntrospection__TypeTest() {
    Assertions.assertThat(GraphQLUtil.isListOfIntrospection__Type(Introspection.__Type)).isFalse();
    Assertions.assertThat(GraphQLUtil.isListOfIntrospection__Type(GraphQLNonNull.nonNull(Introspection.__Type)))
        .isFalse();
    Assertions.assertThat(GraphQLUtil.isListOfIntrospection__Type(GraphQLList.list(Introspection.__Type))).isTrue();
    Assertions.assertThat(
        GraphQLUtil.isListOfIntrospection__Type(GraphQLList.list(GraphQLNonNull.nonNull(Introspection.__Type))))
        .isTrue();
    Assertions.assertThat(
        GraphQLUtil.isListOfIntrospection__Type(GraphQLList.list(GraphQLObjectType.newObject().name("fool").build())))
        .isFalse();
  }

  @Test
  public void isNotEmptySelectionSet() {
    Assertions.assertThat(GraphQLUtil.isNotEmpty(SelectionSet.newSelectionSet().selection(new Field("foo")).build()))
        .isTrue();
    Assertions.assertThat(GraphQLUtil.isNotEmpty(SelectionSet.newSelectionSet().build())).isFalse();
    Assertions.assertThat(GraphQLUtil.isNotEmpty(null)).isFalse();
  }
}
