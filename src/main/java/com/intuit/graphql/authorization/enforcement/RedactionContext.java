package com.intuit.graphql.authorization.enforcement;

import graphql.language.Field;
import graphql.schema.FieldCoordinates;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RedactionContext {
    Field field;
    FieldCoordinates fieldCoordinates;
}
