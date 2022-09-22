package com.intuit.graphql.authorization.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.FieldCoordinates;
import java.util.List;

public class JsonUtil {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  private JsonUtil() {
  }

  public static String toJson(List<FieldCoordinates> invalidFields) throws JsonProcessingException {
    return objectMapper.writeValueAsString(invalidFields);
  }

}
