package org.motechproject.email.domain;

import org.motechproject.mds.annotations.CrudEvents;
import org.motechproject.mds.annotations.Entity;
import org.motechproject.mds.annotations.EntityExtension;
import org.motechproject.mds.annotations.Field;
import org.motechproject.mds.event.CrudEventType;

/**
 * The <code>EmailRecordExtension</code> class represents a record of a sent Email.
 * This class is exposed as an {@link Entity} through
 * Motech Data Services.
 *
 * @see org.motechproject.mds.annotations
 */
@EntityExtension
@Entity(recordHistory = true)
@CrudEvents(CrudEventType.NONE)
public class EmailRecordExtension extends EmailRecord {

    @Field
    private Long providerId;

    @Field
    private String provider;

    public Long getProviderId() {
        return providerId;
    }

    public void setProviderId(Long providerId) {
        this.providerId = providerId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }
}
