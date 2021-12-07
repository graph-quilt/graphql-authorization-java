package com.intuit.graphql.authorization.enforcement;

import static graphql.ErrorType.DataFetchingException;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.analysis.QueryVisitorStub;
import graphql.execution.ExecutionContext;
import graphql.language.Field;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLTypeUtil;
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
    GraphQLOutputType type = queryVisitorFieldEnvironment.getFieldDefinition().getType();
    GraphQLOutputType parentType = queryVisitorFieldEnvironment.getParentType();
    GraphQLFieldDefinition requestedFieldDefinition = queryVisitorFieldEnvironment.getFieldDefinition();
    Field field = queryVisitorFieldEnvironment.getField();

    //first check if the type itself is permitted
    //this is just an optimization to keep from descending on each field node
    boolean permitted = typeFieldPermissionVerifier.isPermitted(requestedFieldDefinition.getType());

    // a special case is the request for a introspection schema object - it is allowed
    //TODO: this looks  a little awkward. we should change the verifier interface to only make one call from the visitor
    //check if the field being accessed is premitted
    if (permitted) {
      permitted = typeFieldPermissionVerifier.isPermitted(parentType, requestedFieldDefinition);
    }

    if (!permitted) {
      //record an error
      String parentName = GraphQLTypeUtil.unwrapAll(parentType).getName();
      authzListener.onFieldRedaction(executionContext, queryVisitorFieldEnvironment);

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
