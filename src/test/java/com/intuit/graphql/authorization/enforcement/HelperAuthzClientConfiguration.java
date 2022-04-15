package com.intuit.graphql.authorization.enforcement;

import com.intuit.graphql.authorization.config.AuthzClient;
import com.intuit.graphql.authorization.config.AuthzClientConfiguration;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HelperAuthzClientConfiguration implements AuthzClientConfiguration {

  //Queries By Client
  public static Map<AuthzClient, List<String>> queriesByClient = new HashMap<>();

  static {

    try {
      AuthzClient client1 = HelperUtils
          .yamlMapper().readValue(HelperUtils.read("mocks.graphqlauthz/client/client1.yml"), AuthzClient.class);
      AuthzClient client2 = HelperUtils
          .yamlMapper().readValue(HelperUtils.read("mocks.graphqlauthz/client/client2.yml"), AuthzClient.class);
      AuthzClient client3 = HelperUtils
          .yamlMapper().readValue(HelperUtils.read("mocks.graphqlauthz/client/client3.yml"), AuthzClient.class);
      AuthzClient client4 = HelperUtils
          .yamlMapper().readValue(HelperUtils.read("mocks.graphqlauthz/client/client4.yml"), AuthzClient.class);
      AuthzClient client5 = HelperUtils
          .yamlMapper().readValue(HelperUtils.read("mocks.graphqlauthz/client/client5-pa.yml"), AuthzClient.class);
      AuthzClient client6 = HelperUtils
          .yamlMapper().readValue(HelperUtils.read("mocks.graphqlauthz/client/client6.yml"), AuthzClient.class);

      queriesByClient.put(client1, Collections.singletonList(
          HelperUtils.readString("mocks.graphqlauthz/client/client1-permissions.graphql")
      ));
      queriesByClient.put(client2, Arrays.asList(
          HelperUtils.readString("mocks.graphqlauthz/client/client2-permissions-query.graphql"),
          HelperUtils.readString("mocks.graphqlauthz/client/client2-permissions-mutation.graphql")
      ));
      queriesByClient.put(client3, Arrays.asList(
          HelperUtils.readString("mocks.graphqlauthz/client/client3-permissions-query.graphql")));
      queriesByClient.put(client4, Arrays.asList(
          HelperUtils.readString("mocks.graphqlauthz/client/client4-permissions-query.graphql"),
          HelperUtils.readString("mocks.graphqlauthz/client/client4-permissions-mutation1.graphql"),
          HelperUtils.readString("mocks.graphqlauthz/client/client4-permissions-mutation2.graphql")
      ));
      queriesByClient.put(client5, Arrays.asList(
          HelperUtils.readString("mocks.graphqlauthz/client/client5-permissions-query.graphql"),
          HelperUtils.readString("mocks.graphqlauthz/client/client5-permissions-mutation1.graphql"),
          HelperUtils.readString("mocks.graphqlauthz/client/client5-permissions-mutation2.graphql")
      ));
      queriesByClient.put(client6, Arrays.asList(
          HelperUtils.readString("mocks.graphqlauthz/client/client6-permissions-query.graphql")
      ));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public Map<AuthzClient, List<String>> getQueriesByClient() {
    return queriesByClient;
  }
}
