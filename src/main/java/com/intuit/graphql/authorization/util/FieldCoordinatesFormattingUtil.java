package com.intuit.graphql.authorization.util;

import graphql.schema.FieldCoordinates;
import java.util.Set;
import java.util.StringJoiner;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class FieldCoordinatesFormattingUtil {

  private static final String DELIMITER_COMMA = ",";

  private FieldCoordinatesFormattingUtil() {
  }

  public static String toString(Set<FieldCoordinates> invalidFields) {
    if (CollectionUtils.isEmpty(invalidFields)) {
      return "";
    }
    return StringUtils.join(invalidFields.iterator(), DELIMITER_COMMA);
  }

}
