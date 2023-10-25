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

import static org.assertj.core.api.Assertions.assertThat

class AuthorizationTest extends Specification{

    private AuthzInstrumentation authzInstrumentation
    private AuthzClientConfiguration authzClientConfiguration = new HelperAuthzClientConfiguration()
    private ScopeProvider scopeProvider = new HelperScopeProvider()
    private GraphQLSchema schema
    private GraphQL graphql
    private String mutationQuery
    private String fragmentsInMutationQuery

    @Before
    void setup() {

        mutationQuery = '''
        mutation {
          createNewBookRecord(input: {
            id: "Book-7",
            name: "New World",
            pageCount: 1001,
            author:{
                  id: "Author-7"
                  firstName: "Mickey",
                  lastName: "Mouse"
                }
          }) {
            id
            name
            pageCount
            author{
                  firstName
                  lastName
                }
          }
          updateBookRecord(input: {
              id: "book-3",
              name: "test updates",
              pageCount: 100
            }) {
              id
            }
          removeBookRecord(input: {
              id: "book-1"
            }) {
              id
            }
        }
        '''
        fragmentsInMutationQuery = '''
        mutation {
          createNewBookRecord(input: {
            id: "Book-7",
            name: "New World",
            pageCount: 1001,
            author:{
                  id: "Author-7"
                  firstName: "Mickey",
                  lastName: "Mouse"
                }
          }) {
            id
            name
            pageCount
            author{
                  ...nameFragment
                }
          }
          updateBookRecord(input: {
              id: "book-3",
              name: "test updates",
              pageCount: 100
        
            }) {
              id
            }
          removeBookRecord(input: {
              id: "book-1"
            }) {
              id
            }
        }
        fragment nameFragment on Author {
          firstName 
          lastName
        }
        '''

        String sdl = TestStaticResources.TEST_SCHEMA
        schema = HelperBuildTestSchema.buildSchema(sdl)

        authzInstrumentation = AuthzInstrumentation.builder()
                .configuration(authzClientConfiguration)
                .schema(schema)
                .scopeProvider(scopeProvider)
                .authzListener(null)
                .build()

        GraphQL.Builder builder = GraphQL.newGraphQL(schema)
        builder.instrumentation(authzInstrumentation)
        graphql = builder.build()
    }

    @Test
    void "test authorization with some redactions with list"() {

        given:
        def requestAllBooks = '''
        {
            allBooks {
                id
                name
                pageCount
                author {
                    firstName
                    lastName
                }
                rating {
                    comments
                    stars
                }
            }
        }
        '''
        def executionInput = ExecutionInput.newExecutionInput().query(requestAllBooks).context("Test.client6").build()

        when:
        def result = graphql.execute(executionInput)

        then:
        def errors = result.getErrors().collect { it.getMessage() }
        def data = result.getData().toString()

        expect:
        errors.contains("403 - Not authorized to access field=lastName of type=Author")
        errors.contains("403 - Not authorized to access field=pageCount of type=Book")
        errors.contains("403 - Not authorized to access field=rating of type=Book")

        data.contains("[id:book-1, name:Harry Potter and the Philosopher's Stone, author:[firstName:Joanne]]")
        data.contains("[id:book-2, name:Moby Dick, author:[firstName:Herman]]")
        data.contains("[id:book-3, name:Interview with the vampire, author:[firstName:Anne]]")
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
        def requestAllFieldsWithIntrospection = '''
        {
            bookById(id: "book-2") {
                __typename
                id
                name
                pageCount
                author {
                    __typename
                    firstName
                    lastName
                }
                rating {
                    __typename
                    comments
                    stars
                }
            }
        }
        '''
        def executionInput = ExecutionInput.newExecutionInput()
                .query(requestAllFieldsWithIntrospection)
                .context("Test.client2")
                .build()

        and:
        def result = graphql.execute(executionInput)
        def errors = result.errors
        def dataString = result.data.toString()

        expect:
        errors[0].message.contains("403 - Not authorized to access field=lastName of type=Author")
        errors[1].message.contains("403 - Not authorized to access field=rating of type=Book")
        dataString == "[bookById:[__typename:Book, id:book-2, name:Moby Dick, pageCount:635, author:[__typename:Author, firstName:Herman]]]"
    }

    @Test
    void "test authz happy case"() {
        given:
        def requestWithAllowedFields = '''
        {
            bookById(id: "book-2") {
                id
                name
                pageCount
                author {
                    firstName
                }
            }
        }
        '''
        def executionInput = ExecutionInput.newExecutionInput()
                .query(requestWithAllowedFields)
                .context("Test.client2")
                .build();

        and:
        def result = graphql.execute(executionInput)
        def dataString = result.data.toString()

        expect:
        result.errors.size() == 0
        dataString == "[bookById:[id:book-2, name:Moby Dick, pageCount:635, author:[firstName:Herman]]]"
    }

    @Test
    void "test authz happy case all fields"() {
        given:
        def requestAllFields = '''
        {
            bookById(id: "book-2") {
                __typename
                id
                name
                pageCount
                author {
                    __typename
                    firstName
                    lastName
                }
                rating {
                    __typename
                    comments
                    stars
                }
            }
        }
        '''
        def executionInput = ExecutionInput.newExecutionInput()
                .query(requestAllFields)
                .context("Test.client1")
                .build()

        and:
        def result = graphql.execute(executionInput)
        def dataString = result.data.toString()

        expect:
        result.errors.size() == 0
        dataString == "[bookById:[__typename:Book, id:book-2, name:Moby Dick, pageCount:635, author:[__typename:Author, firstName:Herman, lastName:Melville], rating:[__typename:Rating, comments:Excellent, stars:5]]]"
    }

    @Test
    void "test authz happy case all fields with fragments"() {
        given:
        def requestWithFragments = '''
        {
            bookById(id: "book-3") {
                id
                name
                pageCount
                author {
                    ...nameFragment
                }
                rating {
                    comments
                    stars
                }
            }
        }
        fragment nameFragment on Author {
            firstName
            lastName
        }
        '''
        def executionInput = ExecutionInput.newExecutionInput()
                .query(requestWithFragments)
                .context("Test.client1")
                .build()

        and:
        def result = graphql.execute(executionInput)
        def dataString = result.data.toString()

        expect:
        result.errors.size() == 0
        dataString == "[bookById:[id:book-3, name:Interview with the vampire, pageCount:371, author:[firstName:Anne, lastName:Rice], rating:[comments:OK, stars:3]]]"

    }

    @Test
    void "test no Authz"() {
        given:
        def requestAllFields = '''
        {
            bookById(id: "book-2") {
                __typename
                id
                name
                pageCount
                author {
                    __typename
                    firstName
                    lastName
                }
                rating {
                    __typename
                    comments
                    stars
                }
            }
        }
        '''
        def executionInput = ExecutionInput.newExecutionInput()
                .query(requestAllFields)
                .context("")
                .build()

        and:
        def result = graphql.execute(executionInput)
        def errors = result.errors

        expect:
        errors.size() == 1
        errors[0].getMessage() == "403 - Not authorized to access field=bookById of type=Query"
    }

    @Test
    void "test authz with invalid scope"() {
        given:
        def requestAllFields = '''
        {
            bookById(id: "book-2") {
                __typename
                id
                name
                pageCount
                author {
                    __typename
                    firstName
                    lastName
                }
                rating {
                    __typename
                    comments
                    stars
                }
            }
        }
        '''
        def executionInput = ExecutionInput.newExecutionInput()
                .query(requestAllFields)
                .context("INV001")
                .build()

        and:
        def result = graphql.execute(executionInput)
        def errors = result.errors

        expect:
        errors.size() == 1
        errors[0].getMessage() == "403 - Not authorized to access field=bookById of type=Query"
    }

    @Test
    void "test authz multi scopes"() {
        given:
        def requestAllFields = '''
        {
            bookById(id: "book-2") {
                __typename
                id
                name
                pageCount
                author {
                    __typename
                    firstName
                    lastName
                }
                rating {
                    __typename
                    comments
                    stars
                }
            }
        }
        '''
        def executionInput = ExecutionInput.newExecutionInput()
                .query(requestAllFields)
                .context("Test.client3,Test.client2")
                .build()

        and:
        def result = graphql.execute(executionInput)
        def errors = result.errors
        def dataString = result.data.toString()

        expect:
        errors[0].getMessage().contains("403 - Not authorized to access field=lastName of type=Author")
        dataString == "[bookById:[__typename:Book, id:book-2, name:Moby Dick, pageCount:635, author:[__typename:Author, firstName:Herman], rating:[__typename:Rating, comments:Excellent, stars:5]]]"
    }

    @Test
    void "test authz with invalid field"() {
        given:
        def requestWithInvalidFields = '''
        {
            bookById(id: "book-2") {
                id
                userName
                pageCount
                author {
                    firstName
                }
            }
        }
        '''
        def executionInput = ExecutionInput.newExecutionInput()
                .query(requestWithInvalidFields)
                .context("Test.client2")
                .build()

        and:
        def result = graphql.execute(executionInput)
        def errors = result.errors

        expect:
        errors.size() == 1
        result.data == null
        errors[0].getMessage().contains("Validation error (FieldUndefined@[bookById/userName]) : Field 'userName' in type 'Book' is undefined")
    }

    @Test
    void "test authz with mutation"() {
        given:
        def executionInput = ExecutionInput.newExecutionInput()
                .query(mutationQuery)
                .context("Test.client4")
                .build()

        and:
        def result = graphql.execute(executionInput)
        def errors = result.errors

        expect:
        errors.size() == 3
        errors[0].getMessage().contains("403 - Not authorized to access field=pageCount of type=Book")
        errors[1].getMessage().contains("403 - Not authorized to access field=lastName of type=Author")
        errors[2].getMessage().contains("403 - Not authorized to access field=updateBookRecord of type=Mutation")
        result.data.toString() == "[createNewBookRecord:[id:Book-7, name:New World, author:[firstName:Mickey]], removeBookRecord:[id:book-1]]"
    }

    @Test
    void "test authz with mutation multi scopes"() {
        given:
        def executionInput = ExecutionInput.newExecutionInput()
                .query(mutationQuery)
                .context("Test.client4,Test.client2")
                .build()

        and:
        def result = graphql.execute(executionInput)
        def errors = result.errors

        expect:
        result.data.toString() == "[createNewBookRecord:[id:Book-7, name:New World, pageCount:1001, author:[firstName:Mickey]], updateBookRecord:[id:book-3], removeBookRecord:[id:book-1]]"
        errors.size() == 1
        errors[0].getMessage().contains("403 - Not authorized to access field=lastName of type=Author")
    }

    @Test
    public void "test authz with mutation multi scopes2"() {
        given:
        def executionInput = ExecutionInput.newExecutionInput()
                .query(mutationQuery)
                .context("CCC03,Test.client2")
                .build()

        and:
        def result = graphql.execute(executionInput)
        def errors = result.errors

        expect:
        result.data.toString() == "[updateBookRecord:[id:book-3]]"
        errors.size() == 2
        errors[0].getMessage().contains("403 - Not authorized to access field=createNewBookRecord of type=Mutation")
        errors[1].getMessage().contains("403 - Not authorized to access field=removeBookRecord of type=Mutation")
    }

    def "test authz with mutation no access"() {
        given:
        def executionInput = ExecutionInput.newExecutionInput()
                .query(mutationQuery)
                .context("CCC03")
                .build()

        and:
        def result = graphql.execute(executionInput)
        def errors = result.errors

        expect:
        errors[0].message .contains("403 - Not authorized to access field=createNewBookRecord of type=Mutation")
        errors[1].message.contains("403 - Not authorized to access field=updateBookRecord of type=Mutation")
        errors[2].message.contains("403 - Not authorized to access field=removeBookRecord of type=Mutation")
        result.data.toString() == "[:]"
    }

    def "test authz with mutation no scope"() {
        given:
        def executionInput = ExecutionInput.newExecutionInput()
                .query(mutationQuery)
                .context("")
                .build()

        and:
        def result = graphql.execute(executionInput)
        def errors = result.errors

        expect:
        errors[0].message.contains("403 - Not authorized to access field=createNewBookRecord of type=Mutation")
        errors[1].message.contains("403 - Not authorized to access field=updateBookRecord of type=Mutation")
        errors[2].message.contains("403 - Not authorized to access field=removeBookRecord of type=Mutation")
        result.data.toString() == "[:]"
    }

    def "test authz with mutation and fragments"() {
        given:
        def executionInput = ExecutionInput.newExecutionInput()
                .query(fragmentsInMutationQuery)
                .context("Test.client4")
                .build()

        and:
        def result = graphql.execute(executionInput)
        def errors = result.errors

        expect:
        errors.size() == 3
        errors[1].message.contains("403 - Not authorized to access field=updateBookRecord of type=Mutation")
        errors[0].message.contains("403 - Not authorized to access field=pageCount of type=Book")
        errors[2].message.contains("403 - Not authorized to access field=lastName of type=Author")
        result.data.toString() == "[createNewBookRecord:[id:Book-7, name:New World, author:[firstName:Mickey]], removeBookRecord:[id:book-1]]"
    }

    def "test authz with mutation non oauth2"() {
        given:
        def executionInput = ExecutionInput.newExecutionInput()
                .query(fragmentsInMutationQuery)
                .context("Test.client5")
                .build()

        and:
        def result = graphql.execute(executionInput)
        def errors = result.errors

        expect:
        errors.size() == 3
        errors[1].message.contains("403 - Not authorized to access field=updateBookRecord of type=Mutation")
        errors[0].message.contains("403 - Not authorized to access field=pageCount of type=Book")
        errors[2].message.contains("403 - Not authorized to access field=lastName of type=Author")
        result.data.toString() == "[createNewBookRecord:[id:Book-7, name:New World, author:[firstName:Mickey]], removeBookRecord:[id:book-1]]"
    }

    def "test introspection with test client2"() {
        given:
        def executionInput = ExecutionInput.newExecutionInput()
                .query(IntrospectionQuery.INTROSPECTION_QUERY)
                .context("Test.client2")
                .build()

        and:
        def result = graphql.execute(executionInput)
        def errors = result.errors
        GsonBuilder builder = new GsonBuilder()
        Gson gson = builder.create()
        JsonElement res = gson.toJsonTree(result.toSpecification())
        JsonObject jsonres = res.getAsJsonObject().get("data").getAsJsonObject().get("__schema").getAsJsonObject()
        JsonArray types = jsonres.get("types").getAsJsonArray()

        expect:
        errors.size() == 0
        jsonres.size() == 4
        jsonres.get("queryType").toString() == "{\"name\":\"Query\"}"
        jsonres.get("mutationType").toString() =="{\"name\":\"Mutation\"}"
        types.size() == 19

        hasValue(types, "kind", "OBJECT", "name", "Author").booleanValue()
        hasValue(types, "kind", "OBJECT", "name", "Book").booleanValue()
        hasValue(types, "kind", "OBJECT", "name", "Query").booleanValue()
        hasValue(types, "kind", "OBJECT", "name", "Mutation").booleanValue()
        hasValue(types, "kind", "INPUT_OBJECT", "name", "BookID").booleanValue()
        hasValue(types, "kind", "INPUT_OBJECT", "name", "BookInput").booleanValue()
        hasValue(types, "kind", "INPUT_OBJECT", "name", "AuthorInput").booleanValue()

        getFields(types, "Query") == ["bookById"]
        getFields(types, "Author") == ["firstName"]
        getFields(types, "Book") == ["id", "name", "pageCount", "author"]
        getFields(types, "Mutation") == ["updateBookRecord"]
    }

    def "test introspection without scope"() {
        given:
        def executionInput = ExecutionInput.newExecutionInput()
                .query(IntrospectionQuery.INTROSPECTION_QUERY)
                .context("")
                .build()

        and:
        def result = graphql.execute(executionInput)
        GsonBuilder builder = new GsonBuilder()
        Gson gson = builder.create()
        JsonElement res = gson.toJsonTree(result.toSpecification())
        JsonObject jsonres = res.getAsJsonObject().get("data").getAsJsonObject().get("__schema").getAsJsonObject()
        JsonArray types = jsonres.get("types").getAsJsonArray()

        expect:
        result.errors.size() == 0
        jsonres.size() == 4
        jsonres.get("queryType").toString() == "{\"name\":\"Query\"}"
        jsonres.get("mutationType").toString() == "{\"name\":\"Mutation\"}"
        types.size() == 15
        ! hasValue(types, "kind", "OBJECT", "name", "Author").booleanValue()
        ! hasValue(types, "kind", "OBJECT", "name", "Book").booleanValue()
        ! hasValue(types, "kind", "OBJECT", "name", "Query").booleanValue()
        ! hasValue(types, "kind", "OBJECT", "name", "Mutation").booleanValue()
        ! hasValue(types, "kind", "OBJECT", "name", "Rating").booleanValue()
        hasValue(types, "kind", "INPUT_OBJECT", "name", "BookID").booleanValue()
        hasValue(types, "kind", "INPUT_OBJECT", "name", "BookInput").booleanValue()
        hasValue(types, "kind", "INPUT_OBJECT", "name", "AuthorInput").booleanValue()
    }

    def "test introspection with multi scopes"() {
        given:
        def executionInput = ExecutionInput.newExecutionInput()
                .query(IntrospectionQuery.INTROSPECTION_QUERY)
                .context("Test.client4,Test.client2")
                .build()

        and:
        def result = graphql.execute(executionInput)
        GsonBuilder builder = new GsonBuilder()
        Gson gson = builder.create()
        JsonElement res = gson.toJsonTree(result.toSpecification())
        JsonObject jsonres = res.getAsJsonObject().get("data").getAsJsonObject().get("__schema").getAsJsonObject()
        JsonArray types = jsonres.get("types").getAsJsonArray()

        expect:
        result.errors.size() == 0
        jsonres.size() == 4
        jsonres.get("queryType").toString() == "{\"name\":\"Query\"}"
        jsonres.get("mutationType").toString() == "{\"name\":\"Mutation\"}"
        types.size() == 19
        hasValue(types, "kind", "OBJECT", "name", "Author").booleanValue()
        hasValue(types, "kind", "OBJECT", "name", "Book").booleanValue()
        hasValue(types, "kind", "OBJECT", "name", "Query").booleanValue()
        hasValue(types, "kind", "OBJECT", "name", "Mutation").booleanValue()
        hasValue(types, "kind", "INPUT_OBJECT", "name", "BookID").booleanValue()
        hasValue(types, "kind", "INPUT_OBJECT", "name", "BookInput").booleanValue()
        hasValue(types, "kind", "INPUT_OBJECT", "name", "AuthorInput").booleanValue()
        getFields(types, "Query") == ["bookById"]
        getFields(types, "Author") == ["firstName"]
        getFields(types, "Book") == ["id", "name", "pageCount", "author"]
        getFields(types, "Mutation") == ["createNewBookRecord", "updateBookRecord", "removeBookRecord"]
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
