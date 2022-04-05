package com.intuit.graphql.authorization.config;

import lombok.Data;
import lombok.Getter;

@Data
public class AuthzClient {

  private String id;
  private String description;
  private ClientAuthorizationType type;

  public enum ClientAuthorizationType implements RuleType {
    OFFLINE("offline"),
    ONLINE("online");

    @Getter
    private String name;

    ClientAuthorizationType(String name) {
      this.name = name;
    }
  }
}
