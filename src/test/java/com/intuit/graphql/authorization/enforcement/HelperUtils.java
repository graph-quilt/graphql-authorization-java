package com.intuit.graphql.authorization.enforcement;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.io.Resources;
import lombok.SneakyThrows;

public class HelperUtils {

  private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory())
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  public static ObjectMapper yamlMapper() {
    return YAML_MAPPER;
  }

  @SneakyThrows
  public static byte[] read(String path) {
    return Resources.toByteArray(Resources.getResource(path));
  }

  public static String readString(String path) {
    return new String(read(path));
  }
}
