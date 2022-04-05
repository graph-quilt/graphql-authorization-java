package com.intuit.graphql.authorization.enforcement;

import static org.assertj.core.api.Assertions.assertThat;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TypeFieldPermissionVerifierTest {

  PermissionVerifier permissionsVerifier = new PermissionVerifier() {
  };

  @Mock
  GraphQLType type;

  @Mock
  GraphQLFieldDefinition fieldDefinition;

  @Test
  public void isPermittedTypeReturnsFalseByDefault() {
    assertThat(permissionsVerifier.isPermitted(type)).isFalse();
  }

  @Test
  public void isPermittedTypeAndFieldReturnsFalseByDefault() {
    assertThat(permissionsVerifier.isPermitted(type, fieldDefinition)).isFalse();
  }

  @Test
  public void isPermittedInputTypeReturnsFalseByDefault() {
    assertThat(new TypeFieldPermissionVerifier(null, null)
        .isPermitted(Scalars.GraphQLString, fieldDefinition))
        .isTrue();
  }

}
