package com.intuit.graphql.authorization.extension;

import com.intuit.graphql.authorization.enforcement.TypeFieldPermissionVerifier;

/**
 * The class allows creating authorization logic in addition to
 * {@link TypeFieldPermissionVerifier}.
 *
 * To create an instance of AuthorizationExtension instance,  {@link AuthorizationExtensionProvider}.
 */
public interface AuthorizationExtension {

  FieldAuthorizationResult authorize(FieldAuthorizationEnvironment fieldAuthorizationEnvironment);
}
