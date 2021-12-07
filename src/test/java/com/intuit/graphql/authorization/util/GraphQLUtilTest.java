package com.intuit.graphql.authorization.util;


import graphql.introspection.Introspection;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import org.assertj.core.api.Assertions;
import org.junit.Test;


import static org.assertj.core.api.Assertions.assertThat;

public class GraphQLUtilTest {

    @Test
    public void isOperationTypeTest() {
        GraphQLSchema schema =  GraphQLSchema.newSchema()
                    .query(GraphQLObjectType.newObject().name("myQuery").build())
                    .mutation(GraphQLObjectType.newObject().name("myMutation").build())
                    .subscription(GraphQLObjectType.newObject().name("mySubscription").build())
                .build();
        schema.getMutationType();
        Assertions.assertThat(GraphQLUtil.isOperationType(schema.getMutationType(), schema)).isTrue();
        Assertions.assertThat(GraphQLUtil.isOperationType(schema.getQueryType(), schema)).isTrue();
        Assertions.assertThat(GraphQLUtil.isOperationType(schema.getSubscriptionType(), schema)).isTrue();
        Assertions.assertThat(GraphQLUtil.isOperationType(GraphQLObjectType.newObject().name("query").build(), schema)).isFalse();
    }

    @Test
    public void isIntrospection__TypeTest() {
        Assertions.assertThat(GraphQLUtil.isIntrospection__Type(Introspection.__Type)).isTrue();
        Assertions.assertThat(GraphQLUtil.isIntrospection__Type(GraphQLNonNull.nonNull((Introspection.__Type)))).isTrue();
        Assertions.assertThat(
            GraphQLUtil.isIntrospection__Type(GraphQLNonNull.nonNull(GraphQLObjectType.newObject().name("fool").build()))).isFalse();
        Assertions.assertThat(GraphQLUtil.isIntrospection__Type(GraphQLObjectType.newObject().name("fool").build())).isFalse();
    }

    @Test
    public void isListOfIntrospection__TypeTest() {
        Assertions.assertThat(GraphQLUtil.isListOfIntrospection__Type(Introspection.__Type)).isFalse();
        Assertions.assertThat(GraphQLUtil.isListOfIntrospection__Type(GraphQLNonNull.nonNull(Introspection.__Type))).isFalse();
        Assertions.assertThat(GraphQLUtil.isListOfIntrospection__Type(GraphQLList.list(Introspection.__Type))).isTrue();
        Assertions.assertThat(GraphQLUtil.isListOfIntrospection__Type(GraphQLList.list(GraphQLNonNull.nonNull(Introspection.__Type)))).isTrue();
        Assertions.assertThat(
            GraphQLUtil.isListOfIntrospection__Type(GraphQLList.list(GraphQLObjectType.newObject().name("fool").build()))).isFalse();
    }
}
