<div align="center">

![graphql-authorization-java](./graphql-authorization-java-logo.png)

</div>

# GraphQL Authorization Library

This library responsible for enforcing access control for schema types and fields

-----


[Builds](https://app.circleci.com/pipelines/github/graph-quilt/graphql-authorization-java)    

[![CircleCI](https://circleci.com/gh/graph-quilt/graphql-authorization-java/tree/master.svg?style=shield)](https://app.circleci.com/pipelines/github/graph-quilt/graphql-authorization-java)

### Description

This library enabled access control for accessing types and fields when making a graphql request.

It currently supports queries for 3rd party clients.


*  A graphQL request with valid access returns all the fields requested 
*  A graphQL request that has access to some of the requested fields/types returns:
   <ul><ul><li> Requested fields it has access to</li>
   <li>Error message for the fields it does not have access to 
    

    ```
    "errors": [
    {
      "message": "403 - Not authorized to access field=amendBalanceDueAmt of type=Irs1040Type",
      "locations": [
        {
          "line": 45,
          "column": 15
        }
      ],
      "extensions": {
        "classification": "DataFetchingException"
      }
    },
    ```

### Usage

* Implement the AuthzConfiguration interface and provide the configuration at initialization.
* Implement the PrincipleFetcher interface to get the request-context information at execution time.
* Add the AuthzInstrumentation defined in the library as an instrumentation when you create your GraphQL Instance 

 ```
builder.instrumentation(new AuthzInstrumentation(authzConfiguration, graphQLSchema, principleFetcher))
 ```

#### Maven coordinates:

```xml
  <dependency>
    <groupId>com.intuit.graphql</groupId>
    <artifactId>graphql-authorization-java</artifactId>
    <version>${latest.version}</version>
</dependency>
```

See the [release tab](https://github.com/graph-quilt/graphql-authorization-java/releases) for
the latest information on releases.

#### Authorization Extension

With Authorization extension, users of this library can implement custom authorization in addition
Type-Field based access control list.  

There are two classes that needs to be implemented:

1. AuthorizationExtensionProvider - main purpose of this class is AuthorizationExtension object creation 
   using the `getAuthorizationExtension()` method.  This method is called during `AuthzInstrumentation.instrumentExecutionContent()`.
   The `getAuthorizationExtension()` has direct access to `ExecutionContext` object which may be useful if object creation
   depends on request related data .e.g HTTP headers.    
2. AuthorizationExtension - contains the custom authorization logic which has access to data related 
   to a field selection being authorized. 

Finally, in order to use the authorization extension, the implemented `AuthorizationExtensionProvider` 
can be passed on `AuthzInstrumentation` constructor.

### Compatibility:

 * Java 8, 
 * GraphQL-Java V13

### Contributing

Read the [Contribution guide](./.github/CONTRIBUTING.md)
