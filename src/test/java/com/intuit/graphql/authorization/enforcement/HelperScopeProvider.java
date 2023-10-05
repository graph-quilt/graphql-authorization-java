package com.intuit.graphql.authorization.enforcement;

import com.intuit.graphql.authorization.util.ScopeProvider;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;

public class HelperScopeProvider implements ScopeProvider {
  public Set<String> getScopes(Object o) {
    return new HashSet(Arrays.asList(StringUtils.split(o.toString(),",")));
  }

}
