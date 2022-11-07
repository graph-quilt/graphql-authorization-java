package com.intuit.graphql.authorization.extension;

import graphql.language.Field;
import graphql.language.SelectionSetContainer;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import java.util.Map;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

@Builder
@Getter
@EqualsAndHashCode
public class FieldAuthorizationEnvironment {

  @NonNull
  @EqualsAndHashCode.Include
  private FieldCoordinates fieldCoordinates;
  @NonNull
  @EqualsAndHashCode.Exclude
  private Field field;
  @NonNull
  @EqualsAndHashCode.Exclude
  private Map<String, Object> arguments;
  @NonNull
  @EqualsAndHashCode.Exclude
  private GraphQLFieldDefinition fieldDefinition;
  @NonNull
  @EqualsAndHashCode.Exclude
  private GraphQLOutputType parentType;
  @NonNull
  @EqualsAndHashCode.Exclude
  private GraphQLSchema graphQLSchema;
}
