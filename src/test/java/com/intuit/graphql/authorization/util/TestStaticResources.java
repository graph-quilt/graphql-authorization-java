package com.intuit.graphql.authorization.util;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;

public class TestStaticResources {

  public static String TEST_SCHEMA = "";
  public static String TEST_RULE_QUERY = "";

  static {
    try {
      TEST_SCHEMA = Resources.toString(Resources.getResource("testschema.graphqls"), Charsets.UTF_8);
      TEST_RULE_QUERY = Resources.toString(Resources.getResource("test_rule_query.graphql"), Charsets.UTF_8);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private TestStaticResources() {
  }

}
