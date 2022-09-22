package com.intuit.graphql.authorization.rules;


import java.util.Map;
import java.util.Set;

public interface RuleParser {

  Map<String, Set<String>> parseRule(String rule, InvalidFieldsCollector invalidFieldsCollector);
}
