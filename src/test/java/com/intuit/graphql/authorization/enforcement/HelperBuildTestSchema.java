package com.intuit.graphql.authorization.enforcement;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

public class HelperBuildTestSchema {

  static HelperGraphQLDataFetchers helperGraphQLDataFetchers = new HelperGraphQLDataFetchers();

  public static GraphQLSchema buildSchema(String sdl) {
    TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(sdl);
    RuntimeWiring runtimeWiring = buildWiring();
    SchemaGenerator schemaGenerator = new SchemaGenerator();
    return schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);
  }

  public static RuntimeWiring buildWiring() {
    return RuntimeWiring.newRuntimeWiring()
        .type(newTypeWiring("Query")
            .dataFetcher("bookById", helperGraphQLDataFetchers.getBookByIdDataFetcher()))
        .type(newTypeWiring("Query")
            .dataFetcher("allBooks", helperGraphQLDataFetchers.allBooksDataFetcher()))
        .type(newTypeWiring("Book")
            .dataFetcher("author", helperGraphQLDataFetchers.getAuthorDataFetcher()))
        .type(newTypeWiring("Book")
            .dataFetcher("rating", helperGraphQLDataFetchers.getRatingDataFetcher()))
        .type(newTypeWiring("Mutation")
            .dataFetcher("createNewBookRecord", helperGraphQLDataFetchers.addBookDataFetcher()))
        .type(newTypeWiring("Mutation")
            .dataFetcher("updateBookRecord", helperGraphQLDataFetchers.updateBookDataFetcher()))
        .type(newTypeWiring("Mutation")
            .dataFetcher("removeBookRecord", helperGraphQLDataFetchers.removeBookDataFetcher()))
        .build();
  }
}
