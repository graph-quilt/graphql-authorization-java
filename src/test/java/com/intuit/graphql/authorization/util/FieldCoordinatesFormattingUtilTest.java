package com.intuit.graphql.authorization.util;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import graphql.schema.FieldCoordinates;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class FieldCoordinatesFormattingUtilTest {

  @Test
  public void toStringTestEmpty() {
    String actual = FieldCoordinatesFormattingUtil.toString(Collections.emptySet());
    assertThat(actual).isEqualTo("");
  }

  @Test
  public void toStringTestNullInput() {
    String actual = FieldCoordinatesFormattingUtil.toString(null);
    assertThat(actual).isEqualTo("");
  }

  @Test
  public void toStringTestOneFieldCoordinate() {
    Set<FieldCoordinates> fieldCoordinatesSet = new HashSet<>();
    fieldCoordinatesSet.add(FieldCoordinates.coordinates("TestParentType", "testFieldName"));
    String actual = FieldCoordinatesFormattingUtil.toString(fieldCoordinatesSet);
    assertThat(actual).isEqualTo("TestParentType.testFieldName");
  }

  @Test
  public void toStringTestMoreThatOneFieldCoordinate() {
    Set<FieldCoordinates> fieldCoordinatesSet = new HashSet<>();
    fieldCoordinatesSet.add(FieldCoordinates.coordinates("TestParentType1", "testFieldName1"));
    fieldCoordinatesSet.add(FieldCoordinates.coordinates("TestParentType2", "testFieldName2"));
    String actual = FieldCoordinatesFormattingUtil.toString(fieldCoordinatesSet);
    assertThat(actual).isEqualTo("TestParentType1.testFieldName1,TestParentType2.testFieldName2");
  }

}
