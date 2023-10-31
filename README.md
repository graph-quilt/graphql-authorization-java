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
* Authorization Error message for the fields it does not have access to. You can customize the error message by over-riding the
`getErrorMessage` method in the `ScopeProvider` interface. 
    
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

* Implement the AuthzClientConfiguration interface and provide the configuration for initialization. The configuration contains
  mappings of scopes represented by `id` to the `list of Queries` allowed by that `id`. The id can also represent clientids, 
  userids, scopes or roles.

* Add the AuthzInstrumentation defined in the library as an instrumentation when you create your GraphQL Instance. More on
  [graphql-java instrumentation](https://www.graphql-java.com/documentation/instrumentation/)
  
  If dgs framework is used, add the AuthzInstrumentation as a bean in the configuration class.

* The library provides a default implementation of the ScopeProvider interface. The default implementation uses the request-context
    to fetch the list of scopes associated with the request. The default implementation can be over-ridden by providing a custom
    implementation of the ScopeProvider interface.
  * Get scopes should be customized by overriding the `getScopes` method in the ScopeProvider interface.
  * Request-context information would be available at execution time. Request-context would have headers and that could be used
      to fetch the list of scopes associated with the request.
  * Error Message could be customized by overriding the `getErrorMessage` method in the ScopeProvider interface.
  
* AuthZlistener is an optional interface that can be implemented to listen to the authorization events. The listener can be used
  to log the authorization events or to send the events to a monitoring system. The listener can be added to the instrumentation
  by providing an implementation of the AuthzListener interface.
 
* AuthorizationExtensionProvider is an optional interface that can be implemented to provide custom authorization extensions.
  The extensions can be used to add custom authorization logic. The extensions can be added to the instrumentation by providing
  an implementation of the AuthorizationExtensionProvider interface.

 ```java
 GraphQL.newGraphQL(schema)
       .instrumentation(new AuthzInstrumentation(authzClientConfiguration, schema, scopeProvider,authzListener, authorizationExtensionProvider))
       .build();
 ```
### Example Implementation

Please refer to the [example service](https://github.com/graph-quilt/example-subgraphs/tree/main/name-service) where this library was used to
implement user permissions with userids. 

### Contributing

Read the [Contribution guide](./.github/CONTRIBUTING.md)
