<div align="center">

![graphql-authorization-java](./graphql-authorization-java.png)

</div>

<div style="text-align: center;">A powerful library for securing a GraphQL service using attribute level access control. </div>

-----

![Master Build](https://github.com/graph-quilt/graphql-authorization-java/actions/workflows/main.yml/badge.svg)


## Introduction

This library enforces access control on GraphQL queries by checking for allowed types and fields. A GraphQL query that 
has access to some of the requested fields/types will return:
* Requested fields it has access to
* Authorization Error message for the fields it does not have access to
    
```json lines
 "errors": [
    {
      "message": "403 - Not authorized to access field=accountId of type=AccountType",
      ...
    },
```

## Getting Started 

#### Maven coordinates:

```xml
  <dependency>
    <groupId>com.intuit.graphql</groupId>
    <artifactId>graphql-authorization-java</artifactId>
    <version>${latest.version}</version>
</dependency>
```

### Usage

* Implement the AuthzConfiguration interface and provide the configuration for initialization. The configuration contains
  mappings of scopes represented by `id` to the `list of Queries` allowed by that `id`. The id can also represent clientids, 
  userids, scopes or roles.

* Implement the PrincipleFetcher interface to get the request-context information at execution time. The PrincipleFetcher
  is invoked at runtime to fetch the list of scopes associated with the request.

* Add the AuthzInstrumentation defined in the library as an instrumentation when you create your GraphQL Instance. More on
  [graphql-java instrumentation](https://www.graphql-java.com/documentation/instrumentation/)

 ```java
 GraphQL.newGraphQL(schema)
       .instrumentation(new AuthzInstrumentation(authzConfiguration, schema, principleFetcher))
       .build();
 ```

### Contributing

Read the [Contribution guide](./.github/CONTRIBUTING.md)
