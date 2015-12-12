package org.motechproject.mds.ex.object;

import org.motechproject.mds.ex.MdsException;

public class ObjectNotContainFieldException extends MdsException {

    public ObjectNotContainFieldException(Throwable cause) {
        super("Unable to find field in object", cause, "mds.error.objectNotContainField");
    }
}
