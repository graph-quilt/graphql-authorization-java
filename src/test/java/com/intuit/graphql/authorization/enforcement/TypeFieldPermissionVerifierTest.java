package com.intuit.graphql.authorization.enforcement;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class TypeFieldPermissionVerifierTest {

  PermissionVerifier permissionsVerifier = new PermissionVerifier() {};

  @Mock
  GraphQLType type;

  @Mock
  GraphQLFieldDefinition fieldDefinition;


  String mutationName;


  @Test
  public void isPermittedTypeReturnsFalseByDefault() {
    assertThat(permissionsVerifier.isPermitted(type)).isFalse();

  }

  @Test
  public void isPermittedTypeAndFieldReturnsFalseByDefault() {
    assertThat(permissionsVerifier.isPermitted(type, fieldDefinition)).isFalse();
  }
}
