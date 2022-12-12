package com.intuit.graphql.authorization.extension;

public class DefaultAuthorizationExtension implements AuthorizationExtension {

  @Override
  public FieldAuthorizationResult authorize(
      FieldAuthorizationEnvironment fieldAuthorizationEnvironment) {
    return FieldAuthorizationResult.ALLOWED_FIELD_AUTH_RESULT;
  }
}
