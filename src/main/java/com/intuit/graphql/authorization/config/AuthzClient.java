package com.intuit.graphql.authorization.config;

import lombok.Data;

@Data
public class AuthzClient {

  private String id;
  private String description;
  private ClientAuthorizationType type;

  public enum ClientAuthorizationType implements RuleType {
    OFFLINE("offline"),
    ONLINE("online");

    private String name;

    ClientAuthorizationType(String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }
  }
}
