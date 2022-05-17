package com.intuit.graphql.authorization.enforcement;

import static graphql.ErrorType.DataFetchingException;
import static graphql.schema.GraphQLTypeUtil.unwrapAll;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.analysis.QueryVisitorStub;
import graphql.execution.ExecutionContext;
import graphql.language.Field;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLUnmodifiedType;
import graphql.util.TreeTransformerUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RedactingVisitor extends QueryVisitorStub {

  private final AuthzInstrumentation.AuthzInstrumentationState instrumentationState;
  private final TypeFieldPermissionVerifier typeFieldPermissionVerifier;
  private final ExecutionContext executionContext;
  private final AuthzListener authzListener;


  public RedactingVisitor(AuthzInstrumentation.AuthzInstrumentationState state, ExecutionContext executionContext,
      AuthzListener authzListener) {
    this.instrumentationState = state;
    this.executionContext = executionContext;
    this.authzListener = authzListener;
    typeFieldPermissionVerifier = instrumentationState.getTypeFieldPermissionVerifier();
  }


  @Override
  public void visitField(QueryVisitorFieldEnvironment queryVisitorFieldEnvironment) {
    final GraphQLUnmodifiedType graphQLUnmodifiedParentType = unwrapAll(queryVisitorFieldEnvironment.getParentType());
    GraphQLFieldDefinition requestedFieldDefinition = queryVisitorFieldEnvironment.getFieldDefinition();

    boolean permitted = typeFieldPermissionVerifier.isPermitted(graphQLUnmodifiedParentType, requestedFieldDefinition);

    if (!permitted) {
      //record an error
      String parentName = graphQLUnmodifiedParentType.getName();
      authzListener.onFieldRedaction(executionContext, queryVisitorFieldEnvironment);
      Field field = queryVisitorFieldEnvironment.getField();

      GraphQLError error = GraphqlErrorBuilder.newError()
          .errorType(DataFetchingException)
          .message("403 - Not authorized to access field=%s of type=%s", field.getName(), parentName)
          .location(field.getSourceLocation())
          .build();
      instrumentationState.getAuthzErrors().add(error);

      TreeTransformerUtil.deleteNode(queryVisitorFieldEnvironment.getTraverserContext());
    }
  }
}
