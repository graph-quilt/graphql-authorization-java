package com.intuit.graphql.authorization.util;

import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.Node;
import graphql.util.TraverserContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class QueryPathUtils {

  private QueryPathUtils(){}

  public static List<Object> getNodesAsPathList(TraverserContext<Node> context) {
    List<Node> nodes = new ArrayList<>(context.getParentNodes());
    Collections.reverse(nodes);
    nodes.add(context.thisNode());
    List<Object> pathList = nodes.stream()
        .filter(node -> node instanceof Field || node instanceof FragmentDefinition)
        .map(node -> (node instanceof Field) ? ((Field)node).getName() : ((FragmentDefinition)node).getName())
        .collect(Collectors.toList());

    return pathList;
  }

}