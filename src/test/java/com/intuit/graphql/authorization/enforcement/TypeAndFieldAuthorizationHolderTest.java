package com.intuit.graphql.authorization.enforcement;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.intuit.graphql.authorization.config.AuthzClientConfiguration;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLSchema;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.MockitoRule;

@RunWith(MockitoJUnitRunner.class)

public class TypeAndFieldAuthorizationHolderTest {

  private AuthorizationHolder authorizationHolder;
  private AuthzClientConfiguration authzClientConfiguration;
  private GraphQLSchema schema;

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();
  GraphQLFieldsContainer queryType, bookType;
  GraphQLFieldDefinition bookInfo, authorInfo, ratingInfo;

  @Before
  public void init() throws IOException {

    URL url = Resources.getResource("testschema.graphqls");
    String sdl = Resources.toString(url, Charsets.UTF_8);
    schema = HelperBuildTestSchema.buildSchema(sdl);
    authzClientConfiguration = new HelperAuthzClientConfiguration();
    queryType = (GraphQLFieldsContainer) schema.getType("Query");
    bookType = (GraphQLFieldsContainer) schema.getType("Book");
    bookInfo = queryType.getFieldDefinition("bookById");
    authorInfo = bookType.getFieldDefinition("author");
    ratingInfo = bookType.getFieldDefinition("rating");

    this.authorizationHolder = new AuthorizationHolder(
        AuthzInstrumentation.getAuthorizationFactory(schema).parse(authzClientConfiguration.getQueriesByClient()));

  }

  @Test
  public void parsesCorrectConfiguration() {
    assertThat(authorizationHolder).isNotNull();
  }

  @Test
  public void unknownScopeHasNoPermissions() {
    Set<String> scopes = new HashSet<String>(Arrays.asList("Test.unknown"));
    TypeFieldPermissionVerifier verifier = authorizationHolder.getPermissionsVerifier(scopes, schema);
    assertThat(verifier.isPermitted(bookType)).isFalse();
    assertThat(verifier.isPermitted(queryType, bookInfo)).isFalse();
    assertThat(verifier.isPermitted(bookType, authorInfo)).isFalse();
  }

  @Test
  public void validScopeHasCorrectPermissions() {

    Set<String> scopes = new HashSet<String>(Arrays.asList("Test.client2"));
    TypeFieldPermissionVerifier verifier = authorizationHolder.getPermissionsVerifier(scopes, schema);
    assertThat(verifier.isPermitted(queryType)).isTrue();
    assertThat(verifier.isPermitted(bookType)).isTrue();
    assertThat(verifier.isPermitted(queryType, bookInfo)).isTrue();
    assertThat(verifier.isPermitted(bookType, authorInfo)).isTrue();
    assertThat(verifier.isPermitted(bookType, ratingInfo)).isFalse();
  }

  @Test
  public void multipleScopesHaveMorePermissions() {
    Set<String> scopes = new HashSet<String>(Arrays.asList("Test.client2", "Test.client1"));
    TypeFieldPermissionVerifier verifier = authorizationHolder.getPermissionsVerifier(scopes, schema);
    assertThat(verifier.isPermitted(queryType)).isTrue();
    assertThat(verifier.isPermitted(bookType)).isTrue();
    assertThat(verifier.isPermitted(queryType, bookInfo)).isTrue();
    assertThat(verifier.isPermitted(bookType, authorInfo)).isTrue();
    assertThat(verifier.isPermitted(bookType, ratingInfo)).isTrue();

  }

}
