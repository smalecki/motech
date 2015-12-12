package org.motechproject.mds.annotations;

import org.motechproject.mds.util.Constants;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.RetentionPolicy;

/**
 * The <code>EntityExtension</code> annotation is used to point classes, that should be
 * mapped as Motech Dataservices Entities. The discovery logic for this annotation is done in
 * {@link org.motechproject.mds.annotations.internal.EntityExtensionProcessor}
 *
 * @see org.motechproject.mds.annotations.internal.EntityExtensionProcessor
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PersistenceCapable(identityType = IdentityType.DATASTORE, detachable = "true")
public @interface EntityExtension {

}
