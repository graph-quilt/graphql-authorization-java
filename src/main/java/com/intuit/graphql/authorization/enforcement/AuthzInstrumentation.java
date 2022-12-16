package com.intuit.graphql.authorization.enforcement;

import static com.intuit.graphql.authorization.util.InstrumentDataFetcherAction.RETURN_NULL_DATA;

import com.intuit.graphql.authorization.config.AuthzClientConfiguration;
import com.intuit.graphql.authorization.extension.AuthorizationExtension;
import com.intuit.graphql.authorization.extension.AuthorizationExtensionProvider;
import com.intuit.graphql.authorization.extension.DefaultAuthorizationExtension;
import com.intuit.graphql.authorization.extension.DefaultAuthorizationExtensionProvider;
import com.intuit.graphql.authorization.rules.AuthorizationHolderFactory;
import com.intuit.graphql.authorization.rules.QueryRuleParser;
import com.intuit.graphql.authorization.util.GraphQLUtil;
import com.intuit.graphql.authorization.util.InstrumentDataFetcherAction;
import com.intuit.graphql.authorization.util.PrincipleFetcher;
import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphQLError;
import graphql.analysis.QueryTransformer;
import graphql.execution.ExecutionContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimpleInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationCreateStateParameters;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.language.FragmentDefinition;
import graphql.language.SelectionSet;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.StaticDataFetcher;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

@Slf4j
public class AuthzInstrumentation extends SimpleInstrumentation {

  private static final AuthzListener DEFAULT_AUTHZ_LISTENER = new SimpleAuthZListener();
  private static final AuthorizationExtensionProvider DEFAULT_AUTH_EXTENSION_PROVIDER = new DefaultAuthorizationExtensionProvider();
  private final AuthorizationHolder authorizationHolder;
  private final PrincipleFetcher principleFetcher;
  private AuthzListener authzListener = DEFAULT_AUTHZ_LISTENER;
  private final AuthorizationExtensionProvider authorizationExtensionProvider;

  public AuthzInstrumentation(AuthzClientConfiguration configuration, GraphQLSchema schema,
      PrincipleFetcher principleFetcher, AuthzListener authzListener,
      AuthorizationExtensionProvider authorizationExtensionProvider) {
    if (configuration.getQueriesByClient().isEmpty()) {
      throw new IllegalArgumentException("Clients missing from AuthZClientConfiguration");
    }

    this.authorizationHolder = new AuthorizationHolder(
        getAuthorizationFactory(schema).parse(configuration.getQueriesByClient()));
    this.principleFetcher = principleFetcher;
    this.authzListener = (Objects.nonNull(authzListener)) ? authzListener : DEFAULT_AUTHZ_LISTENER;
    this.authorizationExtensionProvider = authorizationExtensionProvider;
  }

  public AuthzInstrumentation(AuthzClientConfiguration configuration, GraphQLSchema schema,
      PrincipleFetcher principleFetcher, AuthzListener authzListener) {
    this(configuration, schema, principleFetcher, authzListener, DEFAULT_AUTH_EXTENSION_PROVIDER);
  }

  static AuthorizationHolderFactory getAuthorizationFactory(GraphQLSchema graphQLSchema) {
    QueryRuleParser queryRuleParser = new QueryRuleParser(graphQLSchema);
    return new AuthorizationHolderFactory(queryRuleParser);
  }

  @Override
  public AuthzInstrumentationState createState(InstrumentationCreateStateParameters parameters) {
    //
    // instrumentation state is passed during each invocation of an Instrumentation method
    // and allows you to put stateful data away and reference it during the query execution
    //
    Set<String> scopes = principleFetcher.getScopes(parameters.getExecutionInput().getContext());

    //TODO: externalize enforcement open or close decision
    boolean enforce =
        !principleFetcher.authzEnforcementExemption(parameters.getExecutionInput().getContext())
            && CollectionUtils.isNotEmpty(scopes);

    authzListener.onCreatingState(enforce, parameters.getSchema(), parameters.getExecutionInput());
    return new AuthzInstrumentationState(
        authorizationHolder.getPermissionsVerifier(scopes, parameters.getSchema()),
        parameters.getSchema(), scopes, enforce);
  }


  @Override
  public ExecutionContext instrumentExecutionContext(ExecutionContext executionContext,
      InstrumentationExecutionParameters parameters) {
    AuthzInstrumentationState state = parameters.getInstrumentationState();
    AuthorizationExtension authorizationExtension = this.authorizationExtensionProvider.getAuthorizationExtension(executionContext, parameters);
    ExecutionContext enforcedExecutionContext = shouldEnforceAuthorization(state, authorizationExtension)
      ? getAuthzExecutionContext(executionContext, state, authorizationExtension)
      : executionContext;
    authzListener.onEnforcement(state.isEnforce(), executionContext, enforcedExecutionContext);
    return enforcedExecutionContext;
  }

  private boolean shouldEnforceAuthorization(AuthzInstrumentationState state,  AuthorizationExtension authorizationExtension) {
    return state.isEnforce() || !(authorizationExtension instanceof DefaultAuthorizationExtension);
  }

  private ExecutionContext getAuthzExecutionContext(ExecutionContext executionContext,
      AuthzInstrumentationState state, AuthorizationExtension authorizationExtension) {
    log.info("Authorization is enabled");
    ExecutionContext restrictedContext = executionContext
        .transform(executionContextBuilder -> executionContextBuilder
            .operationDefinition(executionContext.getOperationDefinition()
                .transform(operationDefinitionBuilder ->
                    operationDefinitionBuilder
                        .selectionSet(redactSelectionSet(executionContext, state, authorizationExtension))))
            .fragmentsByName(redactFragments(executionContext, state, authorizationExtension))
        );
    log.info("Restricted executionContext created");
    return restrictedContext;
  }


  @Override
  public CompletableFuture<ExecutionResult> instrumentExecutionResult(ExecutionResult executionResult,
      InstrumentationExecutionParameters parameters) {
    AuthzInstrumentationState instrumentationState = parameters.getInstrumentationState();
    List<GraphQLError> graphQLErrors = executionResult.getErrors();
    if (CollectionUtils.isNotEmpty(instrumentationState.getAuthzErrors())) {
      graphQLErrors = Stream.concat(graphQLErrors.stream(), instrumentationState.getAuthzErrors().stream())
          .collect(Collectors.toList());
    }
    if (executionResult.getData() == null) {
      return CompletableFuture.completedFuture(new ExecutionResultImpl(graphQLErrors));
    }
    return CompletableFuture.completedFuture(
        new ExecutionResultImpl(executionResult.getData(), graphQLErrors, executionResult.getExtensions()));
  }


  private QueryTransformer.Builder initQueryTransformerBuilder(ExecutionContext executionContext) {
    return QueryTransformer.newQueryTransformer()
        .schema(executionContext.getGraphQLSchema())
        .variables(executionContext.getVariables())
        .fragmentsByName(executionContext.getFragmentsByName());
  }

  Map<String, FragmentDefinition> redactFragments(ExecutionContext executionContext, AuthzInstrumentationState state, AuthorizationExtension authorizationExtension) {
    //treat each fragment as root and redact based on configuration
    return executionContext.getFragmentsByName().values().stream().map(entry ->
        redactFragment(entry, executionContext, state, authorizationExtension))
        .collect(Collectors.toMap(FragmentDefinition::getName, Function.identity()));
  }

  FragmentDefinition redactFragment(FragmentDefinition fragmentDefinition, ExecutionContext executionContext,
      AuthzInstrumentationState state, AuthorizationExtension authorizationExtension) {
    fragmentDefinition.getTypeCondition();
    QueryTransformer queryTransformer = initQueryTransformerBuilder(executionContext)
        .root(fragmentDefinition)
        .rootParentType(executionContext.getGraphQLSchema().getQueryType())
        .build();

    return (FragmentDefinition)
        queryTransformer.transform(new RedactingVisitor(state, executionContext, authzListener,
            authorizationExtension));
  }


  SelectionSet redactSelectionSet(ExecutionContext executionContext, AuthzInstrumentationState state, AuthorizationExtension authorizationExtension) {
    GraphQLObjectType rootType = GraphQLUtil.getRootTypeFromOperation(executionContext.getOperationDefinition(),
        executionContext.getGraphQLSchema());

    QueryTransformer transformer = initQueryTransformerBuilder(executionContext)
        .rootParentType(rootType)
        .root(executionContext.getOperationDefinition().getSelectionSet())
        .build();

    return (SelectionSet) transformer.transform(new RedactingVisitor(state, executionContext, authzListener,
        authorizationExtension));
  }


  @Override
  public DataFetcher<?> instrumentDataFetcher(DataFetcher<?> dataFetcher,
      InstrumentationFieldFetchParameters parameters) {

    InstrumentDataFetcherAction action = principleFetcher.instrumentDataFetcher(dataFetcher, parameters);
    if (action == RETURN_NULL_DATA) {
      return new StaticDataFetcher(null);
    } else {
      AuthzInstrumentationState state = parameters.getInstrumentationState();
      return state.isEnforce() ? new IntrospectionRedactingDataFetcher(dataFetcher, state)
         : dataFetcher;
    }
  }

  @Data
  @RequiredArgsConstructor
  static class AuthzInstrumentationState implements InstrumentationState {

    private final TypeFieldPermissionVerifier typeFieldPermissionVerifier;
    private final GraphQLSchema graphQLSchema;
    private final Set<String> scopes;
    private final boolean enforce;
    private List<GraphQLError> authzErrors = new LinkedList<>();
  }

}
