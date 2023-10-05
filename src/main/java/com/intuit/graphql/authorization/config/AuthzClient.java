package com.intuit.graphql.authorization.config;

import lombok.Data;

/**
 * This class represents the client calling your GraphQL API. You can extend this class
 * to add more attributes for your client.
 */
@Data
public class AuthzClient {
  /*
  The scopeid or clientid or appid of your client.
   */
  private String id;
}
