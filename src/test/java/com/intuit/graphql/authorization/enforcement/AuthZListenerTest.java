package com.intuit.graphql.authorization.enforcement;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.intuit.graphql.authorization.config.AuthzClientConfiguration;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.analysis.QueryVisitorFieldEnvironment;
import graphql.execution.ExecutionContext;
import graphql.schema.GraphQLSchema;
import java.io.IOException;
import java.net.URL;
import org.junit.Before;
import org.junit.Test;

public class AuthZListenerTest {


  private GraphQL graphql;
  private AuthzInstrumentation instrumentation;
  private TestAuthZListener authzListener;
  private AuthzClientConfiguration authzClientConfiguration;
  private String requestAllFields;
  private String requestWithFragments;
  private String requestWithInvalidFragment;


  @Before
  public void init() throws IOException {

    requestAllFields = HelperUtils.readString("queries/requestAllFields.txt");
    requestWithFragments = HelperUtils.readString("queries/requestWithFragments.txt");
    requestWithInvalidFragment = HelperUtils.readString("queries/requestWithInvalidFragment.txt");

    //Executable Schema
    URL url = Resources.getResource("testschema.graphqls");
    String sdl = Resources.toString(url, Charsets.UTF_8);
    GraphQLSchema executableSchema = HelperBuildTestSchema.buildSchema(sdl);

    authzListener = new TestAuthZListener();
    authzClientConfiguration = new HelperAuthzClientConfiguration();
    instrumentation = new AuthzInstrumentation(authzClientConfiguration, executableSchema, new HelperPrincipleFetcher(),
        authzListener);

    GraphQL.Builder builder = GraphQL.newGraphQL(executableSchema);
    builder.instrumentation(instrumentation);
    graphql = builder.build();
  }

  @Test
  public void authZWithSomeRedactionTest() {
    ExecutionInput executionInput = ExecutionInput.newExecutionInput().query(requestAllFields).context("Test.client2")
        .build();

    ExecutionResult result = graphql.execute(executionInput);

    assertTrue(result.getErrors().get(0).getMessage()
        .contains("403 - Not authorized to access field=lastName of type=Author"));
    assertTrue(
        result.getErrors().get(1).getMessage().contains("403 - Not authorized to access field=rating of type=Book"));
    assertTrue(result.getData().toString()
        .equals("{bookById={id=book-2, name=Moby Dick, pageCount=635, author={firstName=Herman}}}"));

    assertEquals(authzListener.countOnFieldRedaction, 2);
    assertEquals(authzListener.countOnEnforcement, 1);
    assertEquals(authzListener.countOnCreatingState, 1);
  }


  @Test
  public void authZWithNoFieldRedactionTest() {
    ExecutionInput executionInput = ExecutionInput.newExecutionInput().query(requestAllFields).context("Test.client1")
        .build();

    ExecutionResult result = graphql.execute(executionInput);

    assertTrue(result.getErrors().size() == 0);

    assertTrue(result.getData().toString().equals(
        "{bookById={id=book-2, name=Moby Dick, pageCount=635, author={firstName=Herman, lastName=Melville}, rating={comments=Excellent, stars=5}}}"));
    assertEquals(authzListener.countOnFieldRedaction, 0);
    assertEquals(authzListener.countOnEnforcement, 1);
    assertEquals(authzListener.countOnCreatingState, 1);
  }

  @Test
  public void authzWithFragmentRedactionTest() {
    ExecutionInput executionInput = ExecutionInput.newExecutionInput().query(requestWithInvalidFragment)
        .context("Test.client2").build();

    ExecutionResult result = graphql.execute(executionInput);

    assertTrue(result.getErrors().size() == 1);
    assertTrue(result.getErrors().get(0).getMessage()
        .contains("403 - Not authorized to access field=lastName of type=Author"));
    assertTrue(result.getData().toString().equals(
        "{bookById={id=book-3, name=Interview with the vampire, author={firstName=Anne}}}"));
    assertEquals(authzListener.countOnFieldRedaction, 1);
    assertEquals(authzListener.countOnEnforcement, 1);
    assertEquals(authzListener.countOnCreatingState, 1);
  }

  @Test
  public void authzNoFragmentRedactionTest() {
    ExecutionInput executionInput = ExecutionInput.newExecutionInput().query(requestWithFragments)
        .context("Test.client1").build();

    ExecutionResult result = graphql.execute(executionInput);

    assertTrue(result.getErrors().size() == 0);
    assertTrue(result.getData().toString().equals(
        "{bookById={id=book-3, name=Interview with the vampire, pageCount=371, author={firstName=Anne, lastName=Rice}, rating={comments=OK, stars=3}}}"));
    assertEquals(authzListener.countOnFieldRedaction, 0);
    assertEquals(authzListener.countOnEnforcement, 1);
    assertEquals(authzListener.countOnCreatingState, 1);
  }

  @Test
  public void noAuthZTest() {
    ExecutionInput executionInput = ExecutionInput.newExecutionInput().query(requestAllFields).context("").build();
    ExecutionResult result = graphql.execute(executionInput);

    assertTrue(result.getErrors().size() == 0);
    assertTrue(result.getData().toString().equals(
        "{bookById={id=book-2, name=Moby Dick, pageCount=635, author={firstName=Herman, lastName=Melville}, rating={comments=Excellent, stars=5}}}"));
    assertEquals(authzListener.countOnEnforcement, 1);
    assertEquals(authzListener.countOnCreatingState, 1);
    assertEquals(authzListener.countOnFieldRedaction, 0);
  }


  @Test
  public void authzWithInvalidScopeTest() {
    ExecutionInput executionInput = ExecutionInput.newExecutionInput().query(requestAllFields).context("invalid")
        .build();
    ExecutionResult result = graphql.execute(executionInput);

    assertTrue(result.getErrors().size() == 1);
    assertTrue(
        result.getErrors().get(0).getMessage().contains("403 - Not authorized to access field=bookById of type=Query"));
    assertEquals(authzListener.countOnFieldRedaction, 1);
    assertEquals(authzListener.countOnEnforcement, 1);
    assertEquals(authzListener.countOnCreatingState, 1);
  }


  static class TestAuthZListener extends SimpleAuthZListener {

    int countOnFieldRedaction = 0;
    int countOnCreatingState = 0;
    int countOnEnforcement = 0;

    @Override
    public void onFieldRedaction(ExecutionContext executionContext,
        QueryVisitorFieldEnvironment queryVisitorFieldEnvironment) {
      countOnFieldRedaction = countOnFieldRedaction + 1;
    }

    @Override
    public void onCreatingState(boolean isEnforce, GraphQLSchema schema, ExecutionInput executionInput) {
      countOnCreatingState = countOnCreatingState + 1;
    }

    @Override
    public void onEnforcement(boolean isEnforce, ExecutionContext originalExecutionContext,
        ExecutionContext enforcedExecutionContext) {
      countOnEnforcement = countOnEnforcement + 1;
    }
  }

}
