package com.intuit.graphql.authorization.enforcement;

import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.intuit.graphql.authorization.config.AuthzClient;
import com.intuit.graphql.authorization.config.AuthzClientConfiguration;
import com.intuit.graphql.authorization.util.PrincipleFetcher;
import com.intuit.graphql.authorization.util.TestStaticResources;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.instrumentation.Instrumentation;
import graphql.introspection.IntrospectionQuery;
import graphql.schema.GraphQLSchema;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.commons.collections4.CollectionUtils;
import org.assertj.core.api.Assertions;
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
  private String requestAllFieldsWithIntrospection;
  private String requestAllBooks;
  private String requestWithAllowedFields;
  private String requestWithFragments;
  private String requestWithInvalidFields;
  private String mutationQuery;
  private String fragmentsInMutationQuery;

  private static String getGraphqlQuery(String filePath) {
    StringBuilder contentBuilder = new StringBuilder();
    try (Stream<String> stream = Files.lines(Paths.get(filePath), StandardCharsets.UTF_8)) {
      stream.forEach(s -> contentBuilder.append(s));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return contentBuilder.toString();
  }

  @Before
  public void init() {

    requestAllFields = getGraphqlQuery("src/test/resources/queries/requestAllFields.graphql");
    requestAllFieldsWithIntrospection = getGraphqlQuery(
        "src/test/resources/queries/requestAllFieldsWithIntrospection.graphql");
    requestAllBooks = getGraphqlQuery("src/test/resources/queries/requestAllBooks.graphql");
    requestWithAllowedFields = getGraphqlQuery("src/test/resources/queries/requestWithAllowedFields.graphql");
    requestWithFragments = getGraphqlQuery("src/test/resources/queries/requestWithFragments.graphql");
    requestWithInvalidFields = getGraphqlQuery("src/test/resources/queries/requestWithInvalidFields.graphql");
    mutationQuery = getGraphqlQuery("src/test/resources/queries/mutationQuery.graphql");
    fragmentsInMutationQuery = getGraphqlQuery("src/test/resources/queries/mutationQueryWithFragments.graphql");

    String sdl = TestStaticResources.TEST_SCHEMA;
    schema = HelperBuildTestSchema.buildSchema(sdl);

    authzInstrumentation = new AuthzInstrumentation(authzClientConfiguration, schema, principleFetcher, null);
    graphql = buildGraphQL(authzInstrumentation);
  }

  @Test
  public void authzWithSomeRedactionsWithListTest() {
    executionInput = ExecutionInput.newExecutionInput().query(requestAllBooks).context("Test.client6").build();

    ExecutionResult result = graphql.execute(executionInput);

    final List<String> errors = result.getErrors().stream().map(e -> e.getMessage()).collect(Collectors.toList());
    assertThat(errors).contains(
        "403 - Not authorized to access field=lastName of type=Author",
        "403 - Not authorized to access field=pageCount of type=Book",
        "403 - Not authorized to access field=rating of type=Book"
    );

    assertThat(result.getData().toString())
        .contains("{id=book-2, name=Moby Dick, author={firstName=Herman}}",
            "{id=book-3, name=Interview with the vampire, author={firstName=Anne}}"
        );
  }

  @Test
  public void authzWithNoClientConfigurationTest() {
    final AuthzClientConfiguration authzClientConfiguration = new AuthzClientConfiguration() {
      @Override
      public Map<AuthzClient, List<String>> getQueriesByClient() {
        return new HashMap<>();
      }
    };
    Assertions
        .assertThatThrownBy(() -> new AuthzInstrumentation(authzClientConfiguration, schema, principleFetcher, null))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Clients missing from AuthZClientConfiguration");
  }

  @Test
  public void authzIntrospectionWithSomeRedactionsTest() {
    executionInput = ExecutionInput.newExecutionInput().query(requestAllFieldsWithIntrospection).context("Test.client2")
        .build();

    ExecutionResult result = graphql.execute(executionInput);

    assertTrue(result.getErrors().get(0).getMessage()
        .contains("403 - Not authorized to access field=lastName of type=Author"));
    assertTrue(
        result.getErrors().get(1).getMessage().contains("403 - Not authorized to access field=rating of type=Book"));
    assertTrue(result.getData().toString()
        .equals(
            "{bookById={__typename=Book, id=book-2, name=Moby Dick, pageCount=635, author={__typename=Author, firstName=Herman}}}"));
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
        .equals("{bookById={__typename=Book, id=book-2, name=Moby Dick, pageCount=635, author={__typename=Author, firstName=Herman}}}"));
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
        "{bookById={__typename=Book, id=book-2, name=Moby Dick, pageCount=635, author={__typename=Author, firstName=Herman, lastName=Melville}, rating={__typename=Rating, comments=Excellent, stars=5}}}"));
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
        "{bookById={__typename=Book, id=book-2, name=Moby Dick, pageCount=635, author={__typename=Author, firstName=Herman, lastName=Melville}, rating={__typename=Rating, comments=Excellent, stars=5}}}"));
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
        "{bookById={__typename=Book, id=book-2, name=Moby Dick, pageCount=635, author={__typename=Author, firstName=Herman}, rating={__typename=Rating, comments=Excellent, stars=5}}}"));
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

    assertTrue(hasValue(types, "kind", "OBJECT", "name", "Author"));
    assertTrue(hasValue(types, "kind", "OBJECT", "name", "Book"));
    assertTrue(hasValue(types, "kind", "OBJECT", "name", "Query"));
    assertTrue(hasValue(types, "kind", "OBJECT", "name", "Mutation"));
    assertTrue(hasValue(types, "kind", "INPUT_OBJECT", "name", "BookID"));
    assertTrue(hasValue(types, "kind", "INPUT_OBJECT", "name", "BookInput"));
    assertTrue(hasValue(types, "kind", "INPUT_OBJECT", "name", "AuthorInput"));

    assertTrue(CollectionUtils.isEqualCollection(getFields(types, "Query"), Arrays.asList("bookById")));
    assertTrue(CollectionUtils.isEqualCollection(getFields(types, "Author"), Arrays.asList("firstName")));
    assertTrue(CollectionUtils
        .isEqualCollection(getFields(types, "Book"), Arrays.asList("id", "name", "pageCount", "author")));
    assertTrue(CollectionUtils.isEqualCollection(getFields(types, "Mutation"), Arrays.asList("updateBookRecord")));
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

    assertTrue(hasValue(types, "kind", "OBJECT", "name", "Author"));
    assertTrue(hasValue(types, "kind", "OBJECT", "name", "Book"));
    assertTrue(hasValue(types, "kind", "OBJECT", "name", "Query"));
    assertTrue(hasValue(types, "kind", "OBJECT", "name", "Mutation"));
    assertTrue(hasValue(types, "kind", "OBJECT", "name", "Rating"));
    assertTrue(hasValue(types, "kind", "INPUT_OBJECT", "name", "BookID"));
    assertTrue(hasValue(types, "kind", "INPUT_OBJECT", "name", "BookInput"));
    assertTrue(hasValue(types, "kind", "INPUT_OBJECT", "name", "AuthorInput"));

    assertTrue(CollectionUtils.isEqualCollection(getFields(types, "Query"), Arrays.asList("bookById", "allBooks")));
    assertTrue(
        CollectionUtils.isEqualCollection(getFields(types, "Author"), Arrays.asList("id", "firstName", "lastName")));
    assertTrue(CollectionUtils
        .isEqualCollection(getFields(types, "Book"), Arrays.asList("id", "name", "pageCount", "author", "rating")));
    assertTrue(CollectionUtils.isEqualCollection(getFields(types, "Mutation"),
        Arrays.asList("createNewBookRecord", "updateBookRecord", "removeBookRecord")));
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

    assertTrue(hasValue(types, "kind", "OBJECT", "name", "Author"));
    assertTrue(hasValue(types, "kind", "OBJECT", "name", "Book"));
    assertTrue(hasValue(types, "kind", "OBJECT", "name", "Query"));
    assertTrue(hasValue(types, "kind", "OBJECT", "name", "Mutation"));
    assertTrue(hasValue(types, "kind", "INPUT_OBJECT", "name", "BookID"));
    assertTrue(hasValue(types, "kind", "INPUT_OBJECT", "name", "BookInput"));
    assertTrue(hasValue(types, "kind", "INPUT_OBJECT", "name", "AuthorInput"));

    assertTrue(CollectionUtils.isEqualCollection(getFields(types, "Query"), Arrays.asList("bookById")));
    assertTrue(CollectionUtils.isEqualCollection(getFields(types, "Author"), Arrays.asList("firstName")));
    assertTrue(CollectionUtils
        .isEqualCollection(getFields(types, "Book"), Arrays.asList("id", "name", "pageCount", "author")));
    assertTrue(CollectionUtils.isEqualCollection(getFields(types, "Mutation"),
        Arrays.asList("createNewBookRecord", "updateBookRecord", "removeBookRecord")));

  }

  @Test
  public void authzWithSomeRedactionsAndInstrumentDataFetcherActionTest() {
    PrincipleFetcher testPrincipleFetcherTest = new HelperPrincipleFetcherWithInstrumentation();
    AuthzInstrumentation testAuthzInstrumentation = new AuthzInstrumentation(authzClientConfiguration, schema, testPrincipleFetcherTest, null);
    GraphQL testGraphQL = buildGraphQL(testAuthzInstrumentation);
    executionInput = ExecutionInput.newExecutionInput().query(requestAllFields).context("Test.client2").build();
    ExecutionResult result = testGraphQL.execute(executionInput);

    assertTrue(result.getErrors().get(0).getMessage()
        .contains("403 - Not authorized to access field=lastName of type=Author"));
    assertTrue(
        result.getErrors().get(1).getMessage().contains("403 - Not authorized to access field=rating of type=Book"));
    assertTrue(result.getData().toString()
        .equals("{bookById={__typename=Book, id=book-2, name=Moby Dick, pageCount=null, author={__typename=Author, firstName=Herman}}}"));
  }

  public boolean hasValue(JsonArray json, String key, String value, String key1, String value1) {
    for (int i = 0; i < json.size(); i++) {  // iterate through the JsonArray
      // first I get the 'i' JsonElement as a JsonObject, then I get the key as a string and I compare it with the value
      if (json.get(i).getAsJsonObject().get(key).getAsString().equals(value) &&
          json.get(i).getAsJsonObject().get(key1).getAsString().equals(value1)) {
        return true;
      }
    }
    return false;
  }

  public Set<String> getFields(JsonArray array, String fieldName) {
    return StreamSupport.stream(array.spliterator(), true)
        .map(JsonElement::getAsJsonObject)
        .filter(js -> js.get("name").getAsString().equals(fieldName))
        .flatMap(q -> StreamSupport.stream(q.getAsJsonArray("fields").spliterator(), true)
            .map(f -> f.getAsJsonObject().get("name").getAsString())
        ).collect(Collectors.toSet());
  }

  private GraphQL buildGraphQL(Instrumentation instrumentation) {
    GraphQL.Builder builder = GraphQL.newGraphQL(this.schema);
    builder.instrumentation(instrumentation);
    return builder.build();
  }
}
