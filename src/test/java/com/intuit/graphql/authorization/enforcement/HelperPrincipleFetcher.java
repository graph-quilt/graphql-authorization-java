package com.intuit.graphql.authorization.enforcement;

import com.intuit.graphql.authorization.util.PrincipleFetcher;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class HelperPrincipleFetcher implements PrincipleFetcher {

  public Set<String> getScopes(Object o) {
    Set<String> hashSet;
    String s = o.toString();
    if (s.length() > 1) {
      hashSet = new HashSet<String>(Arrays.asList(s
          .split(",")));
    } else {
      hashSet = new HashSet<String>();
    }
    return hashSet;
  }


}
