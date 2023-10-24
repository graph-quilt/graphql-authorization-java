package com.intuit.graphql.authorization.enforcement

import com.google.gson.Gson;
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.intuit.graphql.authorization.config.AuthzClient
import com.intuit.graphql.authorization.config.AuthzClientConfiguration
import com.intuit.graphql.authorization.util.ScopeProvider
import com.intuit.graphql.authorization.util.TestStaticResources
import graphql.ExecutionInput
import graphql.GraphQL
import graphql.introspection.IntrospectionQuery
import graphql.schema.GraphQLSchema
import java.util.stream.Collectors
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Before
import org.junit.Test
import spock.lang.Specification


import java.nio.charset.StandardCharsets
import static org.assertj.core.api.Assertions.assertThat

class AuthorizationTest extends Specification{

    private AuthzInstrumentation authzInstrumentation;
    private AuthzClientConfiguration authzClientConfiguration = new HelperAuthzClientConfiguration();
    private ScopeProvider scopeProvider = new HelperScopeProvider();
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
        def contentBuilder = new StringBuilder()
        try {
            new File(filePath).withReader(StandardCharsets.UTF_8 as String) { reader ->
                reader.eachLine { line ->
                    contentBuilder.append(line)
                }
            }
        } catch (IOException e) {
            e.printStackTrace()
        }
        return contentBuilder.toString()
    }

    @Before
    void setup() {
        requestAllBooks = getGraphqlQuery("src/test/resources/queries/requestAllBooks.graphql")
        requestAllFields = getGraphqlQuery("src/test/resources/queries/requestAllFields.graphql")
        requestAllFieldsWithIntrospection = getGraphqlQuery(
                "src/test/resources/queries/requestAllFieldsWithIntrospection.graphql")
        requestWithAllowedFields = getGraphqlQuery("src/test/resources/queries/requestWithAllowedFields.graphql")
        requestWithFragments = getGraphqlQuery("src/test/resources/queries/requestWithFragments.graphql")
        requestWithInvalidFields = getGraphqlQuery("src/test/resources/queries/requestWithInvalidFields.graphql")
        mutationQuery = getGraphqlQuery("src/test/resources/queries/mutationQuery.graphql")
        fragmentsInMutationQuery = getGraphqlQuery("src/test/resources/queries/mutationQueryWithFragments.graphql")

        String sdl = TestStaticResources.TEST_SCHEMA;
        schema = HelperBuildTestSchema.buildSchema(sdl);

        authzInstrumentation = AuthzInstrumentation.builder()
                .configuration(authzClientConfiguration)
                .schema(schema)
                .scopeProvider(scopeProvider)
                .authzListener(null)
                .build();

        GraphQL.Builder builder = GraphQL.newGraphQL(schema);
        builder.instrumentation(authzInstrumentation);
        graphql = builder.build();
    }

    @Test
    void "test authorization with some redactions with list"() {

        def executionInput = ExecutionInput.newExecutionInput().query(requestAllBooks).context("Test.client6").build()

        when:
        def result = graphql.execute(executionInput)

        then:
        def errors = result.getErrors().collect { it.getMessage() }
        assertThat(errors).contains(
                "403 - Not authorized to access field=lastName of type=Author",
                "403 - Not authorized to access field=pageCount of type=Book",
                "403 - Not authorized to access field=rating of type=Book"
        )

        def data = result.getData().toString()
        assertThat(data).contains(
                "[id:book-1, name:Harry Potter and the Philosopher's Stone, author:[firstName:Joanne]]",
                "[id:book-2, name:Moby Dick, author:[firstName:Herman]]",
                "[id:book-3, name:Interview with the vampire, author:[firstName:Anne]]"
        )
    }

    @Test
    void "test authz with no client configuration"() {
        given:
        final AuthzClientConfiguration authzClientConfiguration = new AuthzClientConfiguration() {
            @Override
            Map<AuthzClient, List<String>> getQueriesByClient() {
                return [:]
            }
        }
        when:
        AuthzInstrumentation.builder()
                .configuration(authzClientConfiguration)
                .schema(schema)
                .scopeProvider(new HelperScopeProvider())
                .build()

        then:
        def exception = thrown(IllegalArgumentException)
        exception.message == "Clients missing from AuthZClientConfiguration"
    }

    @Test
    void "test authz Introspection With Some Redactions"() {
        given:
        def executionInput = ExecutionInput.newExecutionInput()
                .query(requestAllFieldsWithIntrospection)
                .context("Test.client2")
                .build()

        when:
        def result = graphql.execute(executionInput)

        then:
        assertThat(result.errors[0].message)
                .contains("403 - Not authorized to access field=lastName of type=Author")
        assertThat(result.errors[1].message)
                .contains("403 - Not authorized to access field=rating of type=Book")
        assertThat(result.data.toString())
                .isEqualTo("[bookById:[__typename:Book, id:book-2, name:Moby Dick, pageCount:635, author:[__typename:Author, firstName:Herman]]]")
    }

    @Test
    void "test authz happy case"() {
        given:
        def executionInput = ExecutionInput.newExecutionInput()
                .query(requestWithAllowedFields)
                .context("Test.client2")
                .build();

        when:
        def result = graphql.execute(executionInput)

        then:
        assertThat(result.errors.size()).isEqualTo(0)
        assertThat(result.data.toString())
                .isEqualTo("[bookById:[id:book-2, name:Moby Dick, pageCount:635, author:[firstName:Herman]]]")
    }

    @Test
    void "test authz happy case all fields"() {
        given:
        def executionInput = ExecutionInput.newExecutionInput()
                .query(requestAllFields)
                .context("Test.client1")
                .build()

        when:
        def result = graphql.execute(executionInput);\

        then:
        assertThat(result.errors.size()).isEqualTo(0)
        assertThat(result.data.toString())
                .isEqualTo("[bookById:[__typename:Book, id:book-2, name:Moby Dick, pageCount:635, author:[__typename:Author, firstName:Herman, lastName:Melville], rating:[__typename:Rating, comments:Excellent, stars:5]]]")
    }

    @Test
    void "test authz happy case all fields with fragments"() {
        given:
        def executionInput = ExecutionInput.newExecutionInput()
                .query(requestWithFragments)
                .context("Test.client1")
                .build()

        when:
        def result = graphql.execute(executionInput)

        then:
        assertThat(result.errors.size()).isEqualTo(0)
        assertThat(result.data.toString())
                .isEqualTo("[bookById:[id:book-3, name:Interview with the vampire, pageCount:371, author:[firstName:Anne, lastName:Rice], rating:[comments:OK, stars:3]]]")

    }

    @Test
    void "test no Authz"() {
        given:
        def executionInput = ExecutionInput.newExecutionInput()
                .query(requestAllFields)
                .context("")
                .build()

        when:
        def result = graphql.execute(executionInput)

        then:
        assertThat(result.errors.size()).isEqualTo(1)
        assertThat(result.errors[0].getMessage()).contains("403 - Not authorized to access field=bookById of type=Query")
    }

    @Test
    void "test authz with invalid scope"() {
        given:
        def executionInput = ExecutionInput.newExecutionInput()
                .query(requestAllFields)
                .context("INV001")
                .build()

        when:
        def result = graphql.execute(executionInput)

        then:
        assertThat(result.errors.size()).isEqualTo(1)
        assertThat(result.errors[0].getMessage()).contains("403 - Not authorized to access field=bookById of type=Query")
    }

    @Test
    void "test authz multi scopes"() {
        given:
        def executionInput = ExecutionInput.newExecutionInput()
                .query(requestAllFields)
                .context("Test.client3,Test.client2")
                .build()

        when:
        def result = graphql.execute(executionInput)

        then:
        assertThat(result.errors[0].getMessage())
                .contains("403 - Not authorized to access field=lastName of type=Author")
        assertThat(result.data.toString())
                .isEqualTo("[bookById:[__typename:Book, id:book-2, name:Moby Dick, pageCount:635, author:[__typename:Author, firstName:Herman], rating:[__typename:Rating, comments:Excellent, stars:5]]]")
    }

    @Test
    void "test authz with invalid field"() {
        given:
        def executionInput = ExecutionInput.newExecutionInput()
                .query(requestWithInvalidFields)
                .context("Test.client2")
                .build()

        when:
        def result = graphql.execute(executionInput)

        then:
        assertThat(result.errors.size()).isEqualTo(1)
        assertThat(result.getData()).isNull()
        assertThat(result.errors[0].getMessage())
                .contains("Validation error (FieldUndefined@[bookById/userName]) : Field 'userName' in type 'Book' is undefined")
    }

    @Test
    void "test authz with mutation"() {
        given:
        def executionInput = ExecutionInput.newExecutionInput()
                .query(mutationQuery)
                .context("Test.client4")
                .build()

        when:
        def result = graphql.execute(executionInput)

        then:
        assertThat(result.errors.size()).isEqualTo(3)
        assertThat( result.errors[0].getMessage()).contains("403 - Not authorized to access field=pageCount of type=Book")
        assertThat(result.errors[1].getMessage())
                .contains("403 - Not authorized to access field=lastName of type=Author")
        assertThat(result.errors[2].getMessage())
                .contains("403 - Not authorized to access field=updateBookRecord of type=Mutation")
        assertThat(result.data.toString())
                .isEqualTo("[createNewBookRecord:[id:Book-7, name:New World, author:[firstName:Mickey]], removeBookRecord:[id:book-1]]")
    }

    @Test
    void "test authz with mutation multi scopes"() {
        given:
        def executionInput = ExecutionInput.newExecutionInput()
                .query(mutationQuery)
                .context("Test.client4,Test.client2")
                .build()

        when:
        def result = graphql.execute(executionInput)

        then:
        assertThat(result.data.toString())
                .isEqualTo("[createNewBookRecord:[id:Book-7, name:New World, pageCount:1001, author:[firstName:Mickey]], updateBookRecord:[id:book-3], removeBookRecord:[id:book-1]]")
        assertThat(result.errors.size()).isEqualTo(1)
        assertThat(result.errors[0].getMessage())
                .contains("403 - Not authorized to access field=lastName of type=Author")
    }

    @Test
    public void "test authz with mutation multi scopes2"() {
        given:
        def executionInput = ExecutionInput.newExecutionInput()
                .query(mutationQuery)
                .context("CCC03,Test.client2")
                .build()

        when:
        def result = graphql.execute(executionInput)

        then:
        assertThat(result.data.toString()).isEqualTo("[updateBookRecord:[id:book-3]]")
        assertThat(result.errors.size()).isEqualTo(2)
        assertThat(result.errors[0].getMessage())
                .contains("403 - Not authorized to access field=createNewBookRecord of type=Mutation")
        assertThat(result.errors[1].getMessage())
                .contains("403 - Not authorized to access field=removeBookRecord of type=Mutation")
    }

    def "test authz with mutation no access"() {
        given:
        def executionInput = ExecutionInput.newExecutionInput()
                .query(mutationQuery)
                .context("CCC03")
                .build()

        when:
        def result = graphql.execute(executionInput)

        then:
        assertThat(result.errors[0].message)
                .contains("403 - Not authorized to access field=createNewBookRecord of type=Mutation")
        assertThat(result.errors[1].message)
                .contains("403 - Not authorized to access field=updateBookRecord of type=Mutation")
        assertThat(result.errors[2].message)
                .contains("403 - Not authorized to access field=removeBookRecord of type=Mutation")
        assertThat(result.data.toString()).isEqualTo("[:]")
    }

    def "test authz with mutation no scope"() {
        given:
        def executionInput = ExecutionInput.newExecutionInput()
                .query(mutationQuery)
                .context("")
                .build()

        when:
        def result = graphql.execute(executionInput)

        then:
        assertThat(result.errors[0].message)
                .contains("403 - Not authorized to access field=createNewBookRecord of type=Mutation")
        assertThat(result.errors[1].message)
                .contains("403 - Not authorized to access field=updateBookRecord of type=Mutation")
        assertThat(result.errors[2].message)
                .contains("403 - Not authorized to access field=removeBookRecord of type=Mutation")
        assertThat(result.data.toString()).isEqualTo("[:]")
    }

    def "test authz with mutation and fragments"() {
        given:
        def executionInput = ExecutionInput.newExecutionInput()
                .query(fragmentsInMutationQuery)
                .context("Test.client4")
                .build()

        when:
        def result = graphql.execute(executionInput)

        then:
        assertThat(result.errors.size()).isEqualTo(3)
        assertThat(result.errors[1].message)
                .contains("403 - Not authorized to access field=updateBookRecord of type=Mutation")
        assertThat(result.errors[0].message)
                .contains("403 - Not authorized to access field=pageCount of type=Book")
        assertThat(result.errors[2].message)
                .contains("403 - Not authorized to access field=lastName of type=Author")
        assertThat(result.data.toString())
                .isEqualTo("[createNewBookRecord:[id:Book-7, name:New World, author:[firstName:Mickey]], removeBookRecord:[id:book-1]]")
    }

    def "test authz with mutation non oauth2"() {
        given:
        def executionInput = ExecutionInput.newExecutionInput()
                .query(fragmentsInMutationQuery)
                .context("Test.client5")
                .build()

        when:
        def result = graphql.execute(executionInput)

        then:
        assertThat(result.errors.size()).isEqualTo(3)
        assertThat(result.errors[1].message)
                .contains("403 - Not authorized to access field=updateBookRecord of type=Mutation")
        assertThat(result.errors[0].message)
                .contains("403 - Not authorized to access field=pageCount of type=Book")
        assertThat(result.errors[2].message)
                .contains("403 - Not authorized to access field=lastName of type=Author")
        assertThat(result.data.toString()).isEqualTo("[createNewBookRecord:[id:Book-7, name:New World, author:[firstName:Mickey]], removeBookRecord:[id:book-1]]")
    }

    def "test introspection with test client2"() {
        given:
        def executionInput = ExecutionInput.newExecutionInput()
                .query(IntrospectionQuery.INTROSPECTION_QUERY)
                .context("Test.client2")
                .build()

        when:
        def result = graphql.execute(executionInput)

        then:
        assertThat(result.errors.size()).isEqualTo(0)

        GsonBuilder builder = new GsonBuilder()
        Gson gson = builder.create()
        JsonElement res = gson.toJsonTree(result.toSpecification())
        JsonObject jsonres = res.getAsJsonObject().get("data").getAsJsonObject().get("__schema").getAsJsonObject()
        assertThat(jsonres.size()).isEqualTo(4)

        assertThat(jsonres.get("queryType").toString()).isEqualTo("{\"name\":\"Query\"}")
        assertThat(jsonres.get("mutationType").toString()).isEqualTo("{\"name\":\"Mutation\"}")

        JsonArray types = jsonres.get("types").getAsJsonArray()
        assertThat(types.size()).isEqualTo(19)

        assertThat(hasValue(types, "kind", "OBJECT", "name", "Author")).isTrue()
        assertThat(hasValue(types, "kind", "OBJECT", "name", "Book")).isTrue()
        assertThat(hasValue(types, "kind", "OBJECT", "name", "Query")).isTrue()
        assertThat(hasValue(types, "kind", "OBJECT", "name", "Mutation")).isTrue()
        assertThat(hasValue(types, "kind", "INPUT_OBJECT", "name", "BookID")).isTrue()
        assertThat(hasValue(types, "kind", "INPUT_OBJECT", "name", "BookInput")).isTrue()
        assertThat(hasValue(types, "kind", "INPUT_OBJECT", "name", "AuthorInput")).isTrue()

        assertThat(getFields(types, "Query")).containsExactly("bookById")
        assertThat(getFields(types, "Author")).containsExactly("firstName")
        assertThat(getFields(types, "Book")).containsExactly("id", "name", "pageCount", "author")
        assertThat(getFields(types, "Mutation")).containsExactly("updateBookRecord")
    }

    def "test introspection without scope"() {
        given:
        def executionInput = ExecutionInput.newExecutionInput()
                .query(IntrospectionQuery.INTROSPECTION_QUERY)
                .context("")
                .build()

        when:
        def result = graphql.execute(executionInput)

        then:
        assertThat(result.errors.size()).isEqualTo(0)

        GsonBuilder builder = new GsonBuilder()
        Gson gson = builder.create()
        JsonElement res = gson.toJsonTree(result.toSpecification())
        JsonObject jsonres = res.getAsJsonObject().get("data").getAsJsonObject().get("__schema").getAsJsonObject()
        assertThat(jsonres.size()).isEqualTo(4)
        assertThat(jsonres.get("queryType").toString()).isEqualTo("{\"name\":\"Query\"}")
        assertThat(jsonres.get("mutationType").toString()).isEqualTo("{\"name\":\"Mutation\"}")

        JsonArray types = jsonres.get("types").getAsJsonArray()
        assertThat(types.size()).isEqualTo(15)

        assertThat(hasValue(types, "kind", "OBJECT", "name", "Author")).isFalse()
        assertThat(hasValue(types, "kind", "OBJECT", "name", "Book")).isFalse()
        assertThat(hasValue(types, "kind", "OBJECT", "name", "Query")).isFalse()
        assertThat(hasValue(types, "kind", "OBJECT", "name", "Mutation")).isFalse()
        assertThat(hasValue(types, "kind", "OBJECT", "name", "Rating")).isFalse()
        assertThat(hasValue(types, "kind", "INPUT_OBJECT", "name", "BookID")).isTrue()
        assertThat(hasValue(types, "kind", "INPUT_OBJECT", "name", "BookInput")).isTrue()
        assertThat(hasValue(types, "kind", "INPUT_OBJECT", "name", "AuthorInput")).isTrue()
    }

    def "test introspection with multi scopes"() {
        given:
        def executionInput = ExecutionInput.newExecutionInput()
                .query(IntrospectionQuery.INTROSPECTION_QUERY)
                .context("Test.client4,Test.client2")
                .build()

        when:
        def result = graphql.execute(executionInput)

        then:
        assertThat(result.errors.size()).isEqualTo(0)

        GsonBuilder builder = new GsonBuilder()
        Gson gson = builder.create()
        JsonElement res = gson.toJsonTree(result.toSpecification())
        JsonObject jsonres = res.getAsJsonObject().get("data").getAsJsonObject().get("__schema").getAsJsonObject()
        assertThat(jsonres.size()).isEqualTo(4)
        assertThat(jsonres.get("queryType").toString()).isEqualTo("{\"name\":\"Query\"}")
        assertThat(jsonres.get("mutationType").toString()).isEqualTo("{\"name\":\"Mutation\"}")

        JsonArray types = jsonres.get("types").getAsJsonArray()
        assertThat(types.size()).isEqualTo(19)

        assertThat(hasValue(types, "kind", "OBJECT", "name", "Author")).isTrue()
        assertThat(hasValue(types, "kind", "OBJECT", "name", "Book")).isTrue()
        assertThat(hasValue(types, "kind", "OBJECT", "name", "Query")).isTrue()
        assertThat(hasValue(types, "kind", "OBJECT", "name", "Mutation")).isTrue()
        assertThat(hasValue(types, "kind", "INPUT_OBJECT", "name", "BookID")).isTrue()
        assertThat(hasValue(types, "kind", "INPUT_OBJECT", "name", "BookInput")).isTrue()
        assertThat(hasValue(types, "kind", "INPUT_OBJECT", "name", "AuthorInput")).isTrue()

        assertThat(CollectionUtils.isEqualCollection(getFields(types, "Query"), Arrays.asList("bookById"))).isTrue()
        assertThat(CollectionUtils.isEqualCollection(getFields(types, "Author"), Arrays.asList("firstName"))).isTrue()
        assertThat(CollectionUtils.isEqualCollection(getFields(types, "Book"), Arrays.asList("id", "name", "pageCount", "author"))).isTrue()
        assertThat(CollectionUtils.isEqualCollection(getFields(types, "Mutation"), Arrays.asList("createNewBookRecord", "updateBookRecord", "removeBookRecord"))).isTrue()
    }

    private boolean hasValue(JsonArray array, String key1, String value1, String key2, String value2) {
        for (JsonElement element : array) {
            JsonObject obj = element.getAsJsonObject()
            if (obj.get(key1).getAsString().equals(value1) && obj.get(key2).getAsString().equals(value2)) {
                return true
            }
        }
        return false
    }

    private List<String> getFields(JsonArray array, String typeName) {
        for (JsonElement element : array) {
            JsonObject obj = element.getAsJsonObject()
            if (obj.get("name").getAsString().equals(typeName)) {
                JsonArray fields = obj.get("fields").getAsJsonArray()
                return fields.stream().map { it.getAsJsonObject().get("name").getAsString() }.collect(Collectors.toList())
            }
        }
        return []
    }

}
