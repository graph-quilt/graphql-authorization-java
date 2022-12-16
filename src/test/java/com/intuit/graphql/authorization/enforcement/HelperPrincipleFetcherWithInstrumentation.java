package com.intuit.graphql.authorization.enforcement;

import static com.intuit.graphql.authorization.util.InstrumentDataFetcherAction.DEFAULT;
import static com.intuit.graphql.authorization.util.InstrumentDataFetcherAction.RETURN_NULL_DATA;

import com.intuit.graphql.authorization.util.InstrumentDataFetcherAction;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.schema.DataFetcher;

public class HelperPrincipleFetcherWithInstrumentation extends HelperPrincipleFetcher {

  @Override
  public InstrumentDataFetcherAction instrumentDataFetcher(DataFetcher<?> dataFetcher,
      InstrumentationFieldFetchParameters parameters) {

    if (parameters.getEnvironment().getField().getName().equals("pageCount")) {
      return RETURN_NULL_DATA;
    }

    return DEFAULT;
  }

}
