package com.intuit.graphql.authorization.enforcement;

import static graphql.ErrorType.DataFetchingException;
import static graphql.schema.GraphQLTypeUtil.unwrapAll;

import com.intuit.graphql.authorization.extension.AuthorizationExtension;
import com.intuit.graphql.authorization.extension.FieldAuthorizationEnvironment;
import com.intuit.graphql.authorization.extension.FieldAuthorizationResult;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.analysis.QueryVisitorStub;
import graphql.execution.ExecutionContext;
import graphql.language.Field;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLUnmodifiedType;
import graphql.util.TreeTransformerUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RedactingVisitor extends QueryVisitorStub {

  private final AuthzInstrumentation.AuthzInstrumentationState instrumentationState;
  private final TypeFieldPermissionVerifier typeFieldPermissionVerifier;
  private final ExecutionContext executionContext;
  private final AuthzListener authzListener;
  private final AuthorizationExtension authorizationExtension;


  public RedactingVisitor(AuthzInstrumentation.AuthzInstrumentationState state,
      ExecutionContext executionContext, AuthzListener authzListener,
      AuthorizationExtension authorizationExtension) {
    this.instrumentationState = state;
    this.executionContext = executionContext;
    this.authzListener = authzListener;
    this.authorizationExtension = authorizationExtension;
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

    FieldAuthorizationEnvironment fieldAuthorizationEnvironment = createFieldAuthorizationEnvironment(queryVisitorFieldEnvironment);
    FieldAuthorizationResult fieldAuthorizationResult = authorizationExtension.authorize(fieldAuthorizationEnvironment);
    if (!fieldAuthorizationResult.isAllowed()) {
      authzListener.onFieldRedaction(executionContext, queryVisitorFieldEnvironment);
      instrumentationState.getAuthzErrors().add(fieldAuthorizationResult.getGraphqlErrorException());
      TreeTransformerUtil.deleteNode(queryVisitorFieldEnvironment.getTraverserContext());
    }
  }

  private FieldAuthorizationEnvironment createFieldAuthorizationEnvironment(
      QueryVisitorFieldEnvironment queryVisitorFieldEnvironment) {

    GraphQLUnmodifiedType parentType = unwrapAll(queryVisitorFieldEnvironment.getParentType());

    FieldCoordinates fieldCoordinates = FieldCoordinates
      .coordinates(parentType.getName(), queryVisitorFieldEnvironment.getField().getName());

    return FieldAuthorizationEnvironment.builder()
        .field(queryVisitorFieldEnvironment.getField())
        .arguments(queryVisitorFieldEnvironment.getArguments())
        .fieldCoordinates(fieldCoordinates)
        .fieldDefinition(queryVisitorFieldEnvironment.getFieldDefinition())
        .parentType(queryVisitorFieldEnvironment.getParentType())
        .graphQLSchema(queryVisitorFieldEnvironment.getSchema())
        .build();
  }
}
