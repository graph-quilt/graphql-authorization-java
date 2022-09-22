package com.intuit.graphql.authorization.util;

import graphql.schema.FieldCoordinates;
import java.util.Set;
import java.util.StringJoiner;
import org.apache.commons.collections4.CollectionUtils;

public class FieldCoordinatesFormattingUtil {

  private static final CharSequence DELIMITER_COMMA = ",";

  private FieldCoordinatesFormattingUtil() {
  }

  public static String toString(Set<FieldCoordinates> invalidFields) {
    if (CollectionUtils.isEmpty(invalidFields)) {
      return "";
    }
    StringJoiner stringJoiner = new StringJoiner(DELIMITER_COMMA);
    invalidFields.forEach(fc -> stringJoiner.add(fc.toString()));
    return stringJoiner.toString();
  }

}
