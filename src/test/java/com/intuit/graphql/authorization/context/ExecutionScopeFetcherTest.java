package com.intuit.graphql.authorization.context;

import static org.junit.Assert.assertTrue;

import com.intuit.graphql.authorization.util.ScopeProvider;
import graphql.schema.GraphQLObjectType;
import org.junit.Test;
import org.mockito.Mock;

public class ExecutionScopeFetcherTest {

  ScopeProvider scopeProvider = new ScopeProvider() {
  };

  @Mock
  GraphQLObjectType graphQLObjectType;


  @Test
  public void getScopesByDefault() {

    assertTrue(scopeProvider.getScopes(graphQLObjectType).isEmpty());

  }

}
