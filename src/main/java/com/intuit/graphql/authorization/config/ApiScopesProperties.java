package com.intuit.graphql.authorization.config;

import java.util.List;
import lombok.Data;

@Deprecated
@Data
public class ApiScopesProperties {

  private List<ApiScopeRuleSet> apiscopes;


  @Data
  public static class ApiScopeRuleSet {

    private Id id;
    private String name;
    private String description;
    private List<Rule> rules;
  }

  @Data
  public static class Id {

    private String scope;
    private ScopeType type = ScopeType.OAUTH2;
  }

  @Data
  public static class Rule {

    private RuleType type = RuleType.GRAPHQL_QUERY;
    private Operator operator = Operator.OR;
    private List<String> values;
  }


  public enum RuleType {
    GRAPHQL_QUERY
  }

  public enum Operator {
    OR,
    AND
  }

  public enum ScopeType {
    OAUTH2,
    OTHER
  }
}


