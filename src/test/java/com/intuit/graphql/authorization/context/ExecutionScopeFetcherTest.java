package com.intuit.graphql.authorization.context;

import com.intuit.graphql.authorization.util.PrincipleFetcher;
import graphql.schema.GraphQLObjectType;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.*;

public class ExecutionScopeFetcherTest {

  PrincipleFetcher principleFetcher = new PrincipleFetcher() {
  };

  @Mock
  GraphQLObjectType graphQLObjectType;


  @Test
  public void getScopesByDefault() {

    assertTrue(principleFetcher.getScopes(graphQLObjectType).isEmpty());

  }

}
