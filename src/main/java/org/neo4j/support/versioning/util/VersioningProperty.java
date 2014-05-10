package org.neo4j.support.versioning.util;

import java.util.List;

/**
 * Holds all versioning-specific properties
 *
 * @author afillatre
 * @since 2.1
 */
public enum VersioningProperty {

    VALID_FROM_PROPERTY("__valid_from__"),
    VALID_TO_PROPERTY("__valid_to__"),
    DELETED_PROP_KEY("__deleted__");

    /**
     * Used for backward compatibility only, so versioning created by older version
     * of this plugin won't break. Enum name would suffice otherwise
     */
    private String name;

    private VersioningProperty(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static boolean isInternalProperty(String propertyName) {
        try {
            return valueOf(propertyName) != null;
        } catch (IllegalArgumentException e) {
            // Name doesn't match an existing enum
            return false;
        }
    }
}
