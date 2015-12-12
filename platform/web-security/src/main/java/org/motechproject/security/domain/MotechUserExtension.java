package org.motechproject.security.domain;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.motechproject.commons.date.util.DateUtil;
import org.motechproject.mds.annotations.*;
import org.motechproject.mds.util.SecurityMode;
import org.motechproject.security.constants.PermissionNames;

import javax.jdo.annotations.Unique;
import java.util.List;
import java.util.Locale;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * Entity that represents Motech user
 */
@EntityExtension
@Entity(recordHistory = true)
@Access(value = SecurityMode.PERMISSIONS, members = {PermissionNames.MANAGE_USER_PERMISSION})
public class MotechUserExtension extends MotechUser {

    @Field
    private String pseudo;

    public String getPseudo() {
        return pseudo;
    }

    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }
}
