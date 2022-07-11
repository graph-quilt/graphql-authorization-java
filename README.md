<div align="center">

![graphql-authorization-java](./graphql-authorization-java-logo.png)

</div>

# GraphQL Authorization Library

This library responsible for enforcing access control for schema types and fields

-----


[Builds](https://app.circleci.com/pipelines/github/intuit/graphql-authorization-java)    

[![CircleCI](https://circleci.com/gh/intuit/graphql-authorization-java/tree/master.svg?style=shield)](https://app.circleci.com/pipelines/github/intuit/graphql-authorization-java)

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

See the [release tab](https://github.com/graphql-authorization-java/releases) for
the latest information on releases.


### Compatibility:

 * Java 8, 
 * GraphQL-Java V13

### Contributing

Read the [Contribution guide](./.github/CONTRIBUTING.md)
