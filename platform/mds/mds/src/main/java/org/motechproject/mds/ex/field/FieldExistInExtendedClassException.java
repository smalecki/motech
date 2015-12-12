package org.motechproject.mds.ex.field;

import org.motechproject.mds.ex.MdsException;

/**
 * This exception signals that a given field name in extension class, already defines in extended class
 */
public class FieldExistInExtendedClassException extends MdsException {

    /**
     * @param fieldName field name of the entity
     * @param clazz extension class name of the entity
     * @param superclass extended class name of the entity
     */
    public FieldExistInExtendedClassException(String fieldName, Class clazz, Class superclass) {
        super("Field " + fieldName + " defines in " + clazz.getName() +" already exists in " + superclass.getName(), null, "mds.error.fieldExistInExtendedClass");
    }
}
