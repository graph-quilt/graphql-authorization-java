package com.intuit.graphql.authorization.enforcement;

import static graphql.introspection.Introspection.__Type;

import com.intuit.graphql.authorization.util.GraphQLUtil;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLType;
import java.util.List;
import java.util.stream.Collectors;

class IntrospectionRedactingDataFetcher implements DataFetcher {

  private final DataFetcher delegate;
  private final AuthzInstrumentation.AuthzInstrumentationState state;

  public IntrospectionRedactingDataFetcher(DataFetcher delegate, AuthzInstrumentation.AuthzInstrumentationState state) {
    this.state = state;
    this.delegate = delegate;
  }

  @Override
  public Object get(DataFetchingEnvironment environment) throws Exception {
    Object delegatedGetResult = delegate.get(environment);
    if (delegatedGetResult != null) {
      if (GraphQLUtil.isListOfIntrospection__Type(environment.getFieldType())) {
        //would be nice if there were no type erasure for generics
        return redactTypeList((List<GraphQLType>) delegatedGetResult);
      }

      //used for introspection of fields
      //Note: Since fieldsDatafetcher is now private, we are getting it from codeRegistry
      if (delegate == fieldsDataFetcher(environment)) {
        Object type = environment.getSource();
        return redactFields(
            (List<GraphQLFieldDefinition>) delegatedGetResult,
            (GraphQLFieldsContainer) type);
      }
    }
    return delegatedGetResult;
  }

  private DataFetcher fieldsDataFetcher(DataFetchingEnvironment environment) {
    return environment.getGraphQLSchema().getCodeRegistry().getDataFetcher(__Type,
        __Type.getFieldDefinition("fields"));
  }

  private List<GraphQLFieldDefinition> redactFields(List<GraphQLFieldDefinition> fields,
      GraphQLFieldsContainer fieldsContainer) {
    return fields.stream().filter(fieldDefinition ->
        state.getTypeFieldPermissionVerifier().isPermitted(fieldsContainer, fieldDefinition))
        .collect(Collectors.toList());
  }

  private List<GraphQLType> redactTypeList(List<GraphQLType> fields) {
    return fields.stream()
        .filter(type -> state.getTypeFieldPermissionVerifier().isPermitted(type))
        .collect(Collectors.toList());
  }

}
