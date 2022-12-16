package com.intuit.graphql.authorization.enforcement;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.intuit.graphql.authorization.extension.AuthorizationExtension;
import com.intuit.graphql.authorization.extension.FieldAuthorizationResult;
import graphql.GraphQLError;
import graphql.GraphqlErrorException;
import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.execution.ExecutionContext;
import graphql.language.Field;
import graphql.language.Node;
import graphql.language.SourceLocation;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.util.NodeZipper;
import graphql.util.TraverserContext;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RedactingVisitorTest {

  @Mock
  private AuthzInstrumentation.AuthzInstrumentationState instrumentationState;
  @Mock
  private ExecutionContext executionContext;
  @Mock
  private AuthzListener authzListener;
  @Mock
  private AuthorizationExtension authorizationExtension;
  @Mock
  private TypeFieldPermissionVerifier typeFieldPermissionVerifier;
  @Mock
  private GraphQLSchema graphQLSchema;
  @Mock
  private TraverserContext<Node> traverserContext;
  @Mock
  private NodeZipper nodeZipper;

  private Queue<NodeZipper> zippers = new ArrayDeque<>();

  @Mock
  private QueryVisitorFieldEnvironment queryVisitorFieldEnvironment;
  @Mock
  private GraphQLFieldDefinition fieldDefinition;

  private static final GraphQLObjectType PARENT_TYPE = GraphQLObjectType.newObject()
      .name("ParentType").build();
  private static final Field FIELD = Field.newField("foo")
      .sourceLocation(SourceLocation.EMPTY)
      .build();

  private RedactingVisitor subjectUnderTest;

  GraphqlErrorException authExtensionGraphqlError = GraphqlErrorException.newErrorException().build();

  @Before
  public void setup() {

    when(nodeZipper.deleteNode()).thenReturn(nodeZipper);
    when(traverserContext.getVar(NodeZipper.class)).thenReturn(nodeZipper);
    when(traverserContext.getSharedContextData()).thenReturn(zippers);

    when(instrumentationState.getTypeFieldPermissionVerifier()).thenReturn(
        typeFieldPermissionVerifier);
    List<GraphQLError> graphQLErrorList = new ArrayList<>();
    when(instrumentationState.getAuthzErrors()).thenReturn(graphQLErrorList);

    subjectUnderTest = new RedactingVisitor(instrumentationState, executionContext, authzListener,
        authorizationExtension);

    when(queryVisitorFieldEnvironment.getParentType()).thenReturn(PARENT_TYPE);
    when(queryVisitorFieldEnvironment.getFieldDefinition()).thenReturn(fieldDefinition);
    when(queryVisitorFieldEnvironment.getField()).thenReturn(FIELD);
    when(queryVisitorFieldEnvironment.getArguments()).thenReturn(Collections.emptyMap());
    when(queryVisitorFieldEnvironment.getSchema()).thenReturn(graphQLSchema);
    when(queryVisitorFieldEnvironment.getTraverserContext()).thenReturn(traverserContext);
  }

  @Test
  public void visitField_accessToTypeFieldPermittedAcceptedByAuthExtension_NoGraphQLErrors() {
    // GIVEN
    when(instrumentationState.isEnforce()).thenReturn(true);
    when(typeFieldPermissionVerifier.isPermitted(eq(PARENT_TYPE), eq(fieldDefinition)))
        .thenReturn(true);

    when(authorizationExtension.authorize(any())).thenReturn(
        FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT);

    // WHEN
    subjectUnderTest.visitField(queryVisitorFieldEnvironment);

    // THEN
    verify(queryVisitorFieldEnvironment, times(2)).getFieldDefinition();
    verify(typeFieldPermissionVerifier, times(1)).isPermitted(any(), any());
    verify(authzListener, never()).onFieldRedaction(any(), any());
    verify(nodeZipper, never()).deleteNode();
    verify(authorizationExtension, times(1)).authorize(any());

    assertThat(instrumentationState.getAuthzErrors()).hasSize(0);
  }

  @Test
  public void visitField_accessToTypeFieldPermittedButDeniedByAuthExtension_providesCorrectGraphQLErrors() {
    // GIVEN
    when(instrumentationState.isEnforce()).thenReturn(true);
    when(typeFieldPermissionVerifier.isPermitted(eq(PARENT_TYPE), eq(fieldDefinition)))
        .thenReturn(true);

    when(authorizationExtension.authorize(any())).thenReturn(
        FieldAuthorizationResult.createDeniedResult(authExtensionGraphqlError));

    // WHEN
    subjectUnderTest.visitField(queryVisitorFieldEnvironment);

    // THEN
    verify(queryVisitorFieldEnvironment, times(2)).getFieldDefinition();
    verify(typeFieldPermissionVerifier, times(1)).isPermitted(any(), any());
    verify(authzListener, times(1)).onFieldRedaction(any(), any());
    verify(nodeZipper, times(1)).deleteNode();
    verify(authorizationExtension, times(1)).authorize(any());

    assertThat(instrumentationState.getAuthzErrors()).hasSize(1);
    assertThat(instrumentationState.getAuthzErrors().get(0)).isSameAs(authExtensionGraphqlError);
  }

  @Test
  public void visitField_accessToTypeFieldIsNotPermitted_authExtensionIsBypassedAndProvidesCorrectGraphQLErrors() {
    // GIVEN
    when(instrumentationState.isEnforce()).thenReturn(true);
    when(typeFieldPermissionVerifier.isPermitted(eq(PARENT_TYPE), eq(fieldDefinition)))
        .thenReturn(false);

    // WHEN
    subjectUnderTest.visitField(queryVisitorFieldEnvironment);

    // THEN
    verify(queryVisitorFieldEnvironment, times(1)).getFieldDefinition();
    verify(typeFieldPermissionVerifier, times(1)).isPermitted(any(), any());
    verify(authzListener, times(1)).onFieldRedaction(any(), any());
    verify(nodeZipper, times(1)).deleteNode();
    verify(authorizationExtension, never()).authorize(any());

    assertThat(instrumentationState.getAuthzErrors()).hasSize(1);
    assertThat(instrumentationState.getAuthzErrors().get(0).getMessage()).contains(
        "403 - Not authorized to access field");
  }

  @Test
  public void visitField_authzNotEnforcedDeniedByAuthExtension_providesCorrectGraphQLErrors() {
    // GIVEN
    when(instrumentationState.isEnforce()).thenReturn(false);

    when(authorizationExtension.authorize(any())).thenReturn(
        FieldAuthorizationResult.createDeniedResult(authExtensionGraphqlError));

    // WHEN
    subjectUnderTest.visitField(queryVisitorFieldEnvironment);

    // THEN
    verify(queryVisitorFieldEnvironment, times(2)).getFieldDefinition();
    verify(typeFieldPermissionVerifier, never()).isPermitted(any(), any());
    verify(authzListener, times(1)).onFieldRedaction(any(), any());
    verify(nodeZipper, times(1)).deleteNode();

    assertThat(instrumentationState.getAuthzErrors()).hasSize(1);
    assertThat(instrumentationState.getAuthzErrors().get(0)).isSameAs(authExtensionGraphqlError);
  }

  @Test
  public void visitField_authzNotEnforcedAllowedByAuthExtension_noGraphQLErrors() {
    // GIVEN
    when(instrumentationState.isEnforce()).thenReturn(false);
    when(authorizationExtension.authorize(any())).thenReturn(
        FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT);

    // WHEN
    subjectUnderTest.visitField(queryVisitorFieldEnvironment);

    // THEN
    verify(queryVisitorFieldEnvironment, times(2)).getFieldDefinition();
    verify(typeFieldPermissionVerifier, never()).isPermitted(any(), any());
    verify(authzListener, never()).onFieldRedaction(any(), any());
    verify(nodeZipper, never()).deleteNode();
    verify(authorizationExtension, times(1)).authorize(any());

    assertThat(instrumentationState.getAuthzErrors()).hasSize(0);
  }

}
