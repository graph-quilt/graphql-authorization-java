package com.intuit.graphql.authorization.enforcement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.intuit.graphql.authorization.config.ApiScopesProperties;
import com.intuit.graphql.authorization.config.AuthzConfiguration;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class HelperConfigFetcher implements AuthzConfiguration {


  @Override
  public ApiScopesProperties getPermissions() {
    try {
      return readPermissions(new FileInputStream("src/test/resources/application-apiscopes.yml"));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    return null;
  }

  public ApiScopesProperties readPermissions(InputStream inputStream) {
    try {
      ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
      mapper.findAndRegisterModules();
      ApiScopesProperties properties = mapper.readValue(inputStream, ApiScopesProperties.class);
      return properties;
    } catch (Exception e) {
      return null;
    }
  }

}
