package org.motechproject.mds.ex.entity;

import org.motechproject.mds.ex.MdsException;

/**
 * The <code>EntityNotExtendException</code> exception signals a situation in which
 * an entity not extend any entity
 */
public class EntityNotExtendException extends MdsException {

    /**
     * Constructs a new EntityNotFoundException with <i>mds.error.entityNotFound</i> as
     * a message key.
     * @param entityName the name of entity not found
     */
    public EntityNotExtendException(String entityName) {
        super(entityName + " not extend mds entity", null, "mds.error.entityNotExtend");
    }
}
