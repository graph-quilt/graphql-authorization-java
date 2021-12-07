package com.intuit.graphql.authorization.config;

import lombok.Data;

@Data
public class AuthzClient {
  private String id;
  private String description;
  private ClientAuthorizationType type;

  public enum ClientAuthorizationType {
    OFFLINE
  }
}
