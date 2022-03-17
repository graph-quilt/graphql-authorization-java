package com.intuit.graphql.authorization.enforcement;

import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.intuit.graphql.authorization.config.AuthzClientConfiguration;
import com.intuit.graphql.authorization.util.PrincipleFetcher;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.introspection.IntrospectionQuery;
import graphql.schema.GraphQLSchema;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;

public class AuthorizationTest {

  private ExecutionInput executionInput;
  private AuthzInstrumentation authzInstrumentation;
  private AuthzClientConfiguration authzClientConfiguration = new HelperAuthzClientConfiguration();
  private PrincipleFetcher principleFetcher = new HelperPrincipleFetcher();
  private GraphQLSchema schema;
  private GraphQL graphql;
  private String requestAllFields;
  private String requestWithAllowedFields;
  private String requestWithFragments;
  private String requestWithInvalidFields;
  private String mutationQuery;
  private String fragmentsInMutationQuery;

  @Before
  public void init() throws IOException {

    requestAllFields = getGraphqlQuery("src/test/resources/queries/requestAllFields.txt");
    requestWithAllowedFields = getGraphqlQuery("src/test/resources/queries/requestWithAllowedFields.txt");
    requestWithFragments = getGraphqlQuery("src/test/resources/queries/requestWithFragments.txt");
    requestWithInvalidFields = getGraphqlQuery("src/test/resources/queries/requestWithInvalidFields.txt");
    mutationQuery = getGraphqlQuery("src/test/resources/queries/mutationQuery.txt");
    fragmentsInMutationQuery = getGraphqlQuery("src/test/resources/queries/mutationQueryWithFragments.txt");

    URL url = Resources.getResource("testschema.graphqls");
    String sdl = Resources.toString(url, Charsets.UTF_8);
    schema = HelperBuildTestSchema.buildSchema(sdl);

    authzInstrumentation = new AuthzInstrumentation(authzClientConfiguration, schema, principleFetcher, null);
    GraphQL.Builder builder = GraphQL.newGraphQL(schema);
    builder.instrumentation(authzInstrumentation);
    graphql = builder.build();
  }

  @Test
  public void authzWithSomeRedactionsTest() {
    executionInput = ExecutionInput.newExecutionInput().query(requestAllFields).context("Test.client2").build();

    ExecutionResult result = graphql.execute(executionInput);

    assertTrue(result.getErrors().get(0).getMessage()
        .contains("403 - Not authorized to access field=lastName of type=Author"));
    assertTrue(
        result.getErrors().get(1).getMessage().contains("403 - Not authorized to access field=rating of type=Book"));
    assertTrue(result.getData().toString()
        .equals("{bookById={id=book-2, name=Moby Dick, pageCount=635, author={firstName=Herman}}}"));
  }

  @Test
  public void authzHappycaseTest() {
    executionInput = ExecutionInput.newExecutionInput().query(requestWithAllowedFields).context("Test.client2").build();

    ExecutionResult result = graphql.execute(executionInput);

    assertTrue(result.getErrors().size() == 0);
    assertTrue(result.getData().toString()
        .equals("{bookById={id=book-2, name=Moby Dick, pageCount=635, author={firstName=Herman}}}"));
  }


  @Test
  public void authzHappycaseAllFieldsTest() {
    executionInput = ExecutionInput.newExecutionInput().query(requestAllFields).context("Test.client1").build();

    ExecutionResult result = graphql.execute(executionInput);

    assertTrue(result.getErrors().size() == 0);
    assertTrue(result.getData().toString().equals(
        "{bookById={id=book-2, name=Moby Dick, pageCount=635, author={firstName=Herman, lastName=Melville}, rating={comments=Excellent, stars=5}}}"));
  }

  @Test
  public void authzHappycaseAllFieldsWithFragmentsTest() {
    executionInput = ExecutionInput.newExecutionInput().query(requestWithFragments).context("Test.client1").build();

    ExecutionResult result = graphql.execute(executionInput);

    assertTrue(result.getErrors().size() == 0);
    assertTrue(result.getData().toString().equals(
        "{bookById={id=book-3, name=Interview with the vampire, pageCount=371, author={firstName=Anne, lastName=Rice}, rating={comments=OK, stars=3}}}"));
  }

  @Test
  public void noAuthzTest() {
    executionInput = ExecutionInput.newExecutionInput().query(requestAllFields).context("").build();
    ExecutionResult result = graphql.execute(executionInput);

    assertTrue(result.getErrors().size() == 0);
    assertTrue(result.getData().toString().equals(
        "{bookById={id=book-2, name=Moby Dick, pageCount=635, author={firstName=Herman, lastName=Melville}, rating={comments=Excellent, stars=5}}}"));
  }


  @Test
  public void authzWithInvalidScopeTest() {
    executionInput = ExecutionInput.newExecutionInput().query(requestAllFields).context("INV001").build();
    ExecutionResult result = graphql.execute(executionInput);

    assertTrue(result.getErrors().size() == 1);
    assertTrue(
        result.getErrors().get(0).getMessage().contains("403 - Not authorized to access field=bookById of type=Query"));
  }


  @Test
  public void authzMultiScopesTest() {
    executionInput = ExecutionInput.newExecutionInput().query(requestAllFields).context("Test.client3,Test.client2")
        .build();
    ExecutionResult result = graphql.execute(executionInput);

    assertTrue(result.getErrors().get(0).getMessage()
        .contains("403 - Not authorized to access field=lastName of type=Author"));
    assertTrue(result.getData().toString().equals(
        "{bookById={id=book-2, name=Moby Dick, pageCount=635, author={firstName=Herman}, rating={comments=Excellent, stars=5}}}"));
  }

  @Test
  public void authzWithInvalidFieldTest() {
    executionInput = ExecutionInput.newExecutionInput().query(requestWithInvalidFields).context("Test.client2").build();
    ExecutionResult result = graphql.execute(executionInput);

    assertTrue(result.getErrors().size() == 1);
    assertNull(result.getData());
    assertTrue(result.getErrors().get(0).getMessage().contains(
        "Validation error of type FieldUndefined: Field 'userName' in type 'Book' is undefined @ 'bookById/userName'"));
  }

  @Test
  public void authzWithMutationTest() {
    executionInput = ExecutionInput.newExecutionInput().query(mutationQuery).context("Test.client4").build();
    ExecutionResult result = graphql.execute(executionInput);

    assertTrue(result.getErrors().size() == 3);
    assertTrue(
        result.getErrors().get(0).getMessage().contains("403 - Not authorized to access field=pageCount of type=Book"));
    assertTrue(result.getErrors().get(1).getMessage()
        .contains("403 - Not authorized to access field=lastName of type=Author"));
    assertTrue(result.getErrors().get(2).getMessage()
        .contains("403 - Not authorized to access field=updateBookRecord of type=Mutation"));
    assertTrue(result.getData().toString().equals(
        "{createNewBookRecord={id=Book-7, name=New World, author={firstName=Mickey}}, removeBookRecord={id=book-1}}"));
  }

  @Test
  public void authzWithMutationMultiScopesTest() {
    executionInput = ExecutionInput.newExecutionInput().query(mutationQuery).context("Test.client4,Test.client2")
        .build();
    ExecutionResult result = graphql.execute(executionInput);
    assertTrue(result.getData().toString().equals(
        "{createNewBookRecord={id=Book-7, name=New World, pageCount=1001, author={firstName=Mickey}}, updateBookRecord={id=book-3}, removeBookRecord={id=book-1}}"));
    assertTrue(result.getErrors().size() == 1);
    assertTrue(result.getErrors().get(0).getMessage()
        .contains("403 - Not authorized to access field=lastName of type=Author"));
  }

  @Test
  public void authzWithMutationMultiScopes2Test() {
    executionInput = ExecutionInput.newExecutionInput().query(mutationQuery).context("CCC03,Test.client2").build();
    ExecutionResult result = graphql.execute(executionInput);
    assertTrue(result.getData().toString().equals("{updateBookRecord={id=book-3}}"));
    assertTrue(result.getErrors().size() == 2);
    assertTrue(result.getErrors().get(0).getMessage()
        .contains("403 - Not authorized to access field=createNewBookRecord of type=Mutation"));
    assertTrue(result.getErrors().get(1).getMessage()
        .contains("403 - Not authorized to access field=removeBookRecord of type=Mutation"));
  }

  @Test
  public void authzWithMutationNoAccessTest() {
    executionInput = ExecutionInput.newExecutionInput().query(mutationQuery).context("CCC03").build();
    ExecutionResult result = graphql.execute(executionInput);

    assertTrue(result.getErrors().get(0).getMessage()
        .contains("403 - Not authorized to access field=createNewBookRecord of type=Mutation"));
    assertTrue(result.getErrors().get(1).getMessage()
        .contains("403 - Not authorized to access field=updateBookRecord of type=Mutation"));
    assertTrue(result.getErrors().get(2).getMessage()
        .contains("403 - Not authorized to access field=removeBookRecord of type=Mutation"));
    assertTrue(result.getData().toString().equals("{}"));
  }

  @Test
  public void authzWithMutationNoScopeTest() {
    executionInput = ExecutionInput.newExecutionInput().query(mutationQuery).context("").build();
    ExecutionResult result = graphql.execute(executionInput);

    assertTrue(result.getErrors().size() == 0);
    assertTrue(result.getData().toString().equals(
        "{createNewBookRecord={id=Book-7, name=New World, pageCount=1001, author={firstName=Mickey, lastName=Mouse}}, updateBookRecord={id=book-3}, removeBookRecord={id=book-1}}"));
  }

  @Test
  public void authzWithMutationAndFragmentsTest() {
    executionInput = ExecutionInput.newExecutionInput().query(fragmentsInMutationQuery).context("Test.client4").build();
    ExecutionResult result = graphql.execute(executionInput);

    assertTrue(result.getErrors().size() == 3);
    assertTrue(result.getErrors().get(1).getMessage()
        .contains("403 - Not authorized to access field=updateBookRecord of type=Mutation"));
    assertTrue(
        result.getErrors().get(0).getMessage().contains("403 - Not authorized to access field=pageCount of type=Book"));
    assertTrue(result.getErrors().get(2).getMessage()
        .contains("403 - Not authorized to access field=lastName of type=Author"));
    assertTrue(result.getData().toString().equals(
        "{createNewBookRecord={id=Book-7, name=New World, author={firstName=Mickey}}, removeBookRecord={id=book-1}}"));
  }

  @Test
  public void authzWithMutationNonOauth2Test() {
    executionInput = ExecutionInput.newExecutionInput().query(fragmentsInMutationQuery).context("Test.client5").build();
    ExecutionResult result = graphql.execute(executionInput);

    assertTrue(result.getErrors().size() == 3);
    assertTrue(result.getErrors().get(1).getMessage()
        .contains("403 - Not authorized to access field=updateBookRecord of type=Mutation"));
    assertTrue(
        result.getErrors().get(0).getMessage().contains("403 - Not authorized to access field=pageCount of type=Book"));
    assertTrue(result.getErrors().get(2).getMessage()
        .contains("403 - Not authorized to access field=lastName of type=Author"));
    assertTrue(result.getData().toString().equals(
        "{createNewBookRecord={id=Book-7, name=New World, author={firstName=Mickey}}, removeBookRecord={id=book-1}}"));
  }


  @Test
  public void introspectionWithTestClient2() {
    executionInput = ExecutionInput.newExecutionInput().query(IntrospectionQuery.INTROSPECTION_QUERY)
        .context("Test.client2").build();
    ExecutionResult result = graphql.execute(executionInput);
    assertTrue(result.getErrors().size() == 0);

    GsonBuilder builder = new GsonBuilder();
    Gson gson = builder.create();
    JsonElement res = gson.toJsonTree(result.toSpecification());
    JsonElement jsonres = res.getAsJsonObject().get("data").getAsJsonObject().get("__schema");
    assertTrue(jsonres.getAsJsonObject().size() == 4);

    assertTrue(jsonres.getAsJsonObject().get("queryType").toString().equals("{\"name\":\"Query\"}"));
    assertTrue(jsonres.getAsJsonObject().get("mutationType").toString().equals("{\"name\":\"Mutation\"}"));

    JsonArray types = (JsonArray) jsonres.getAsJsonObject().get("types");
    assertTrue(types.size() == 19);

    String results = types.toString();
    assertTrue(results.contains(
        "{\"kind\":\"OBJECT\",\"name\":\"Author\",\"fields\":[{\"name\":\"firstName\",\"args\":[],\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"},\"isDeprecated\":false}],\"interfaces\":[]}"));
    assertTrue(results.contains(
        "{\"kind\":\"OBJECT\",\"name\":\"Book\",\"fields\":[{\"name\":\"id\",\"args\":[],\"type\":{\"kind\":\"SCALAR\",\"name\":\"ID\"},\"isDeprecated\":false},{\"name\":\"name\",\"args\":[],\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"},\"isDeprecated\":false},{\"name\":\"pageCount\",\"args\":[],\"type\":{\"kind\":\"SCALAR\",\"name\":\"Int\"},\"isDeprecated\":false},{\"name\":\"author\",\"args\":[],\"type\":{\"kind\":\"OBJECT\",\"name\":\"Author\"},\"isDeprecated\":false}],\"interfaces\":[]}"));
    assertTrue(results.contains(
        "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[{\"name\":\"bookById\",\"args\":[{\"name\":\"id\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"ID\"}}],\"type\":{\"kind\":\"OBJECT\",\"name\":\"Book\"},\"isDeprecated\":false}],\"interfaces\":[]}"));
    assertTrue(results.contains(
        "{\"kind\":\"OBJECT\",\"name\":\"Mutation\",\"fields\":[{\"name\":\"updateBookRecord\",\"args\":[{\"name\":\"input\",\"type\":{\"kind\":\"INPUT_OBJECT\",\"name\":\"BookInput\"}}],\"type\":{\"kind\":\"OBJECT\",\"name\":\"Book\"},\"isDeprecated\":false}],\"interfaces\":[]}"));
    assertTrue(results.contains(
        "{\"kind\":\"INPUT_OBJECT\",\"name\":\"BookID\",\"inputFields\":[{\"name\":\"id\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"ID\"}}]}"));
    assertTrue(results.contains(
        "{\"kind\":\"INPUT_OBJECT\",\"name\":\"BookInput\",\"inputFields\":[{\"name\":\"id\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"ID\"}},{\"name\":\"name\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}},{\"name\":\"pageCount\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"Int\"}},{\"name\":\"author\",\"type\":{\"kind\":\"INPUT_OBJECT\",\"name\":\"AuthorInput\"}}]}"));
    assertTrue(results.contains(
        "{\"kind\":\"INPUT_OBJECT\",\"name\":\"AuthorInput\",\"inputFields\":[{\"name\":\"id\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"ID\"}},{\"name\":\"firstName\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}},{\"name\":\"lastName\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}]}"));
  }


  @Test
  public void introspectionWithoutScope() {
    executionInput = ExecutionInput.newExecutionInput().query(IntrospectionQuery.INTROSPECTION_QUERY).context("")
        .build();
    ExecutionResult result = graphql.execute(executionInput);
    assertTrue(result.getErrors().size() == 0);

    GsonBuilder builder = new GsonBuilder();
    Gson gson = builder.create();
    JsonElement res = gson.toJsonTree(result.toSpecification());
    JsonElement jsonres = res.getAsJsonObject().get("data").getAsJsonObject().get("__schema");
    assertTrue(jsonres.getAsJsonObject().size() == 4);
    assertTrue(jsonres.getAsJsonObject().get("queryType").toString().equals("{\"name\":\"Query\"}"));
    assertTrue(jsonres.getAsJsonObject().get("mutationType").toString().equals("{\"name\":\"Mutation\"}"));

    JsonArray types = (JsonArray) jsonres.getAsJsonObject().get("types");
    assertTrue(types.size() == 20);

    String results = types.toString();
    assertTrue(results.contains(
        "{\"kind\":\"OBJECT\",\"name\":\"Author\",\"fields\":[{\"name\":\"id\",\"args\":[],\"type\":{\"kind\":\"SCALAR\",\"name\":\"ID\"},\"isDeprecated\":false},{\"name\":\"firstName\",\"args\":[],\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"},\"isDeprecated\":false},{\"name\":\"lastName\",\"args\":[],\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"},\"isDeprecated\":false}],\"interfaces\":[]}"));
    assertTrue(results.contains(
        "{\"kind\":\"OBJECT\",\"name\":\"Book\",\"fields\":[{\"name\":\"id\",\"args\":[],\"type\":{\"kind\":\"SCALAR\",\"name\":\"ID\"},\"isDeprecated\":false},{\"name\":\"name\",\"args\":[],\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"},\"isDeprecated\":false},{\"name\":\"pageCount\",\"args\":[],\"type\":{\"kind\":\"SCALAR\",\"name\":\"Int\"},\"isDeprecated\":false},{\"name\":\"author\",\"args\":[],\"type\":{\"kind\":\"OBJECT\",\"name\":\"Author\"},\"isDeprecated\":false},{\"name\":\"rating\",\"args\":[],\"type\":{\"kind\":\"OBJECT\",\"name\":\"Rating\"},\"isDeprecated\":false}],\"interfaces\":[]}"));
    assertTrue(results.contains(
        "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[{\"name\":\"bookById\",\"args\":[{\"name\":\"id\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"ID\"}}],\"type\":{\"kind\":\"OBJECT\",\"name\":\"Book\"},\"isDeprecated\":false}],\"interfaces\":[]}"));
    assertTrue(results.contains(
        "{\"kind\":\"OBJECT\",\"name\":\"Rating\",\"fields\":[{\"name\":\"comments\",\"args\":[],\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"},\"isDeprecated\":false},{\"name\":\"stars\",\"args\":[],\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"},\"isDeprecated\":false}],\"interfaces\":[]}"));
    assertTrue(results.contains(
        "{\"kind\":\"OBJECT\",\"name\":\"Mutation\",\"fields\":[{\"name\":\"createNewBookRecord\",\"args\":[{\"name\":\"input\",\"type\":{\"kind\":\"INPUT_OBJECT\",\"name\":\"BookInput\"}}],\"type\":{\"kind\":\"OBJECT\",\"name\":\"Book\"},\"isDeprecated\":false},{\"name\":\"updateBookRecord\",\"args\":[{\"name\":\"input\",\"type\":{\"kind\":\"INPUT_OBJECT\",\"name\":\"BookInput\"}}],\"type\":{\"kind\":\"OBJECT\",\"name\":\"Book\"},\"isDeprecated\":false},{\"name\":\"removeBookRecord\",\"args\":[{\"name\":\"input\",\"type\":{\"kind\":\"INPUT_OBJECT\",\"name\":\"BookID\"}}],\"type\":{\"kind\":\"OBJECT\",\"name\":\"Book\"},\"isDeprecated\":false}],\"interfaces\":[]}"));
    assertTrue(results.contains(
        "{\"kind\":\"INPUT_OBJECT\",\"name\":\"BookID\",\"inputFields\":[{\"name\":\"id\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"ID\"}}]}"));
    assertTrue(results.contains(
        "{\"kind\":\"INPUT_OBJECT\",\"name\":\"BookInput\",\"inputFields\":[{\"name\":\"id\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"ID\"}},{\"name\":\"name\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}},{\"name\":\"pageCount\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"Int\"}},{\"name\":\"author\",\"type\":{\"kind\":\"INPUT_OBJECT\",\"name\":\"AuthorInput\"}}]}"));
    assertTrue(results.contains(
        "{\"kind\":\"INPUT_OBJECT\",\"name\":\"AuthorInput\",\"inputFields\":[{\"name\":\"id\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"ID\"}},{\"name\":\"firstName\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}},{\"name\":\"lastName\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}]}"));
  }

  @Test
  public void introspectionWithMultiScopes() {
    executionInput = ExecutionInput.newExecutionInput().query(IntrospectionQuery.INTROSPECTION_QUERY)
        .context("Test.client4,Test.client2").build();
    ExecutionResult result = graphql.execute(executionInput);
    assertTrue(result.getErrors().size() == 0);

    GsonBuilder builder = new GsonBuilder();
    Gson gson = builder.create();
    JsonElement res = gson.toJsonTree(result.toSpecification());
    JsonElement jsonres = res.getAsJsonObject().get("data").getAsJsonObject().get("__schema");
    assertTrue(jsonres.getAsJsonObject().size() == 4);
    assertTrue(jsonres.getAsJsonObject().get("queryType").toString().equals("{\"name\":\"Query\"}"));
    assertTrue(jsonres.getAsJsonObject().get("mutationType").toString().equals("{\"name\":\"Mutation\"}"));

    JsonArray types = (JsonArray) jsonres.getAsJsonObject().get("types");
    assertTrue(types.size() == 19);

    String results = types.toString();
    assertTrue(results.contains(
        "{\"kind\":\"OBJECT\",\"name\":\"Author\",\"fields\":[{\"name\":\"firstName\",\"args\":[],\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"},\"isDeprecated\":false}],\"interfaces\":[]}"));
    assertTrue(results.contains(
        "{\"kind\":\"OBJECT\",\"name\":\"Book\",\"fields\":[{\"name\":\"id\",\"args\":[],\"type\":{\"kind\":\"SCALAR\",\"name\":\"ID\"},\"isDeprecated\":false},{\"name\":\"name\",\"args\":[],\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"},\"isDeprecated\":false},{\"name\":\"pageCount\",\"args\":[],\"type\":{\"kind\":\"SCALAR\",\"name\":\"Int\"},\"isDeprecated\":false},{\"name\":\"author\",\"args\":[],\"type\":{\"kind\":\"OBJECT\",\"name\":\"Author\"},\"isDeprecated\":false}],\"interfaces\":[]}"));
    assertTrue(results.contains(
        "{\"kind\":\"OBJECT\",\"name\":\"Query\",\"fields\":[{\"name\":\"bookById\",\"args\":[{\"name\":\"id\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"ID\"}}],\"type\":{\"kind\":\"OBJECT\",\"name\":\"Book\"},\"isDeprecated\":false}],\"interfaces\":[]}"));
    assertTrue(results.contains(
        "{\"kind\":\"OBJECT\",\"name\":\"Mutation\",\"fields\":[{\"name\":\"createNewBookRecord\",\"args\":[{\"name\":\"input\",\"type\":{\"kind\":\"INPUT_OBJECT\",\"name\":\"BookInput\"}}],\"type\":{\"kind\":\"OBJECT\",\"name\":\"Book\"},\"isDeprecated\":false},{\"name\":\"updateBookRecord\",\"args\":[{\"name\":\"input\",\"type\":{\"kind\":\"INPUT_OBJECT\",\"name\":\"BookInput\"}}],\"type\":{\"kind\":\"OBJECT\",\"name\":\"Book\"},\"isDeprecated\":false},{\"name\":\"removeBookRecord\",\"args\":[{\"name\":\"input\",\"type\":{\"kind\":\"INPUT_OBJECT\",\"name\":\"BookID\"}}],\"type\":{\"kind\":\"OBJECT\",\"name\":\"Book\"},\"isDeprecated\":false}],\"interfaces\":[]}"));
    assertTrue(results.contains(
        "{\"kind\":\"INPUT_OBJECT\",\"name\":\"BookID\",\"inputFields\":[{\"name\":\"id\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"ID\"}}]}"));
    assertTrue(results.contains(
        "{\"kind\":\"INPUT_OBJECT\",\"name\":\"BookInput\",\"inputFields\":[{\"name\":\"id\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"ID\"}},{\"name\":\"name\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}},{\"name\":\"pageCount\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"Int\"}},{\"name\":\"author\",\"type\":{\"kind\":\"INPUT_OBJECT\",\"name\":\"AuthorInput\"}}]}"));
    assertTrue(results.contains(
        "{\"kind\":\"INPUT_OBJECT\",\"name\":\"AuthorInput\",\"inputFields\":[{\"name\":\"id\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"ID\"}},{\"name\":\"firstName\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}},{\"name\":\"lastName\",\"type\":{\"kind\":\"SCALAR\",\"name\":\"String\"}}]}"));
  }


  private static String getGraphqlQuery(String filePath) {
    StringBuilder contentBuilder = new StringBuilder();
    try (Stream<String> stream = Files.lines(Paths.get(filePath), StandardCharsets.UTF_8)) {
      stream.forEach(s -> contentBuilder.append(s));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return contentBuilder.toString();
  }


}
