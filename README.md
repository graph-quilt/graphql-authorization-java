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
      "message": "403 - Not authorized to access field=amendBalanceDueAmt of type=Irs1040Type",
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

* Implement the AuthzConfiguration interface and provide the configuration for initialization.

* Implement the PrincipleFetcher interface to get the request-context information at execution time.
* Add the AuthzInstrumentation defined in the library as an instrumentation when you create your GraphQL Instance 

 ```
builder.instrumentation(new AuthzInstrumentation(authzConfiguration, graphQLSchema, principleFetcher))
 ```

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
