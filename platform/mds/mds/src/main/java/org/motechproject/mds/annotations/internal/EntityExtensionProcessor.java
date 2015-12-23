package org.motechproject.mds.annotations.internal;

import org.apache.commons.lang.StringUtils;
import org.motechproject.mds.annotations.Access;
import org.motechproject.mds.annotations.Entity;
import org.motechproject.mds.annotations.EntityExtension;
import org.motechproject.mds.annotations.ReadAccess;
import org.motechproject.mds.domain.MdsEntity;
import org.motechproject.mds.domain.MdsVersionedEntity;
import org.motechproject.mds.dto.*;
import org.motechproject.mds.ex.entity.EntityNotExtendException;
import org.motechproject.mds.ex.entity.EntityNotFoundException;
import org.motechproject.mds.helper.EntityDefaultFieldsHelper;
import org.motechproject.mds.javassist.MotechClassPool;
import org.motechproject.mds.reflections.ReflectionsUtil;
import org.motechproject.mds.service.EntityService;
import org.motechproject.mds.service.TypeService;
import org.motechproject.mds.util.Constants;
import org.motechproject.mds.util.SecurityMode;
import org.motechproject.osgi.web.util.BundleHeaders;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import javax.jdo.annotations.Version;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.*;

import static org.motechproject.mds.util.Constants.AnnotationFields.*;

/**
 * The <code>EntityExtensionProcessor</code> provides a mechanism for processing
 * {@link EntityExtension} annotation of a single
 * class with {@link org.motechproject.mds.annotations.Entity} annotation.
 *
 * @see EntityExtension
 * @see org.motechproject.mds.annotations.Entity
 */
@Component
public class EntityExtensionProcessor extends AbstractListProcessor<EntityExtension, EntityDto> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityExtensionProcessor.class);

    private EntityService entityService;
    private TypeService typeService;
    private FieldProcessor fieldProcessor;
    private UIFilterableProcessor uiFilterableProcessor;
    private UIDisplayableProcessor uiDisplayableProcessor;
    private RestIgnoreProcessor restIgnoreProcessor;
    private RestOperationsProcessor restOperationsProcessor;
    private CrudEventsProcessor crudEventsProcessor;
    private NonEditableProcessor nonEditableProcessor;
    private List<EntityProcessorOutput> processingResult;

    @Override
    public Class<EntityExtension> getAnnotationType() {
        return EntityExtension.class;
    }

    @Override
    public List<EntityProcessorOutput> getProcessingResult() {
        return processingResult;
    }

    @Override
    protected Set<? extends AnnotatedElement> getElementsToProcess() {
        return ReflectionsUtil.getClasses(getAnnotationType(), getBundle());
    }

    @Override
    protected void process(AnnotatedElement element) {
        EntityProcessorOutput entityProcessorOutput = null;

        Class clazz = (Class) element;
        Class<Entity> ann = ReflectionsUtil.getAnnotationClass(clazz, Entity.class);
        Annotation annotation = AnnotationUtils.findAnnotation(clazz, ann);

        Class<EntityExtension> annExt = ReflectionsUtil.getAnnotationClass(clazz, EntityExtension.class);
        Annotation annotationExt = AnnotationUtils.findAnnotation(clazz, annExt);

        if (null != annotationExt) {
            String className = clazz.getName();

            boolean recordHistory = Boolean.parseBoolean(ReflectionsUtil.getAnnotationValue(annotation, HISTORY));
            boolean nonEditable = Boolean.parseBoolean(ReflectionsUtil.getAnnotationValue(annotation, NON_EDITABLE));

            String extendedClassName = clazz.getSuperclass().getName();
            if (Object.class.getName().equalsIgnoreCase(extendedClassName)) {
                throw new EntityNotExtendException(className);
            }

            EntityDto entity;
            RestOptionsDto restOptions;
            TrackingDto tracking;
            Collection<FieldDto> existFields;
            Collection<FieldDto> fields;

            for (EntityProcessorOutput epo : processingResult) {
                if (epo.getEntityProcessingResult().getClassName().equals(extendedClassName)) {
                    entityProcessorOutput = epo;
                    break;
                }
            }

            if (entityProcessorOutput == null) {
                LOGGER.debug("Extended entity {} should already exist", extendedClassName);
                throw new EntityNotFoundException(extendedClassName);
            } else {
                LOGGER.debug("DDE for {} already exists, updating if necessary", extendedClassName);

                entity = entityProcessorOutput.getEntityProcessingResult();
                restOptions = entityProcessorOutput.getRestProcessingResult();
                tracking = entityProcessorOutput.getTrackingProcessingResult();
                existFields = entityProcessorOutput.getFieldProcessingResult();
            }

            if (!tracking.isModifiedByUser()) {
                tracking.setRecordHistory(recordHistory);
                tracking.setNonEditable(nonEditable);
            }
            entity.setExtensionClass(className);

            setSecurityOptions(element, entity);

            // per entity maxFetchDepth that will be passed to the Persistence Manager
            setMaxFetchDepth(entity, annotation);

            entityProcessorOutput.setEntityProcessingResult(entity);

            fields = findFields(clazz, entity, existFields);

            String versionField = getVersionFieldName(clazz);
            addVersionMetadata(fields, versionField);
            addDefaultFields(entity, fields);

            restOptions = processRestOperations(clazz, restOptions);
            restOptions = findRestFields(clazz, restOptions, fields);

            updateUiChangedFields(fields, className);
            updateResults(entityProcessorOutput, clazz, fields, restOptions, tracking, versionField);

            add(entity);
            for (EntityProcessorOutput epo : processingResult) {
                if (epo.getEntityProcessingResult().getClassName().equals(extendedClassName)) {
                    processingResult.remove(epo);
                    processingResult.add(entityProcessorOutput);
                    break;
                }
            }
            MotechClassPool.registerDDE(entity.getExtensionClass());
        } else {
            LOGGER.debug("Did not find EntityExtension annotation in class: {}", clazz.getName());
        }
    }

    private void addVersionMetadata(Collection<FieldDto> fields, String versionField) {
        if (StringUtils.isNotBlank(versionField)) {
            for (FieldDto fieldDto : fields) {
                if (fieldDto.getBasic().getName().equals(versionField)) {
                    fieldDto.addMetadata(new MetadataDto(Constants.MetadataKeys.VERSION_FIELD, Constants.Util.TRUE));
                }
            }
        }
    }

    private String getVersionFieldName(Class clazz) {
        if (MdsVersionedEntity.class.getName().equalsIgnoreCase(clazz.getSuperclass().getName())) {
            return "instanceVersion";
        }

        Class<Version> verAnn = ReflectionsUtil.getAnnotationClass(clazz, Version.class);
        Version versionAnnotation = AnnotationUtils.findAnnotation(clazz, verAnn);
        if (versionAnnotation != null && versionAnnotation.extensions().length !=0 && versionAnnotation.extensions()[0].key().equals("field-name")) {
            return versionAnnotation.extensions()[0].value();
        }

        return null;
    }

    private void setMaxFetchDepth(EntityDto entity, Annotation annotation) {
        int maxFetchDepth = Integer.parseInt(ReflectionsUtil.getAnnotationValue(annotation, MAX_FETCH_DEPTH));
        if (maxFetchDepth != Constants.FetchDepth.MDS_DEFAULT) {
            entity.setMaxFetchDepth(maxFetchDepth);
        }
    }

    private void addDefaultFields(EntityDto entity, Collection<FieldDto> fields) {
        if (!MdsEntity.class.getName().equalsIgnoreCase(entity.getSuperClass()) && !MdsVersionedEntity.class.getName().equalsIgnoreCase(entity.getSuperClass())) {
            fields.addAll(EntityDefaultFieldsHelper.defaultFields(typeService));
        }
    }

    private void updateResults(EntityProcessorOutput entityProcessorOutput, Class<?> clazz, Collection<FieldDto> fields,
                               RestOptionsDto restOptions, TrackingDto tracking, String versionField) {
        entityProcessorOutput.setFieldProcessingResult(fields);

        entityProcessorOutput.setUiFilterableProcessingResult(findFilterableFields(clazz));
        entityProcessorOutput.setUiDisplayableProcessingResult(findDisplayedFields(clazz));
        entityProcessorOutput.setTrackingProcessingResult(processCrudEvents(clazz, tracking));

        entityProcessorOutput.setRestProcessingResult(restOptions);

        Map<String, Boolean> nonEditableFields = findNonEditableFields(clazz);
        //we must set non editable for version field
        if (StringUtils.isNotBlank(versionField)) {
            nonEditableFields.put(versionField, true);
        }
        entityProcessorOutput.setNonEditableProcessingResult(nonEditableFields);
    }

    private void updateUiChangedFields(Collection<FieldDto> fieldsToUpdate, String entityClassName) {
        if (entityService.getEntityByClassName(entityClassName) != null) {
            List<FieldDto> currentFields = entityService.getEntityFieldsByClassName(entityClassName);
            for (FieldDto field : fieldsToUpdate) {
                FieldDto currentField = getCurrentField(currentFields, field.getBasic().getName());
                if (currentField != null && currentField.isUiChanged()) {
                    field.setUiFilterable(currentField.isUiFilterable());
                    field.setUiChanged(currentField.isUiChanged());
                }
            }
        }
    }

    private FieldDto getCurrentField(List<FieldDto> currentFields, String fieldName) {
        for (FieldDto field : currentFields) {
            if (field.getBasic().getName().equals(fieldName)) {
                return field;
            }
        }
        return null;
    }

    protected void execute(Bundle bundle, List<EntityProcessorOutput> processingResult) {
        this.processingResult = processingResult;
        super.execute(bundle);
    }


    @Override
    protected void afterExecution() {
        LOGGER.debug("Execution complete for bundle {}", getBundle().getSymbolicName());
    }

    private RestOptionsDto processRestOperations(Class clazz, RestOptionsDto restOptions) {
        restOperationsProcessor.setClazz(clazz);
        restOperationsProcessor.setRestOptions(restOptions);
        restOperationsProcessor.execute(getBundle());
        return restOperationsProcessor.getProcessingResult();
    }

    private TrackingDto processCrudEvents(Class clazz, TrackingDto tracking) {
        crudEventsProcessor.setClazz(clazz);
        crudEventsProcessor.setTrackingDto(tracking);
        crudEventsProcessor.execute(getBundle());
        return crudEventsProcessor.getProcessingResult();
    }

    private Collection<FieldDto> findFields(Class clazz, EntityDto entity, Collection<FieldDto> existFields) {
        fieldProcessor.setClazz(clazz);
        fieldProcessor.setEntity(entity);
        fieldProcessor.setExistFields(existFields);
        fieldProcessor.execute(getBundle());
        return fieldProcessor.getProcessingResult();
    }

    private Collection<String> findFilterableFields(Class clazz) {
        uiFilterableProcessor.setClazz(clazz);
        uiFilterableProcessor.execute(getBundle());
        return uiFilterableProcessor.getProcessingResult();
    }

    private Map<String, Long> findDisplayedFields(Class clazz) {
        uiDisplayableProcessor.setClazz(clazz);
        uiDisplayableProcessor.execute(getBundle());
        return uiDisplayableProcessor.getProcessingResult();
    }

    private RestOptionsDto findRestFields(Class clazz, RestOptionsDto restOptions, Collection<FieldDto> fields) {
        restIgnoreProcessor.setClazz(clazz);
        restIgnoreProcessor.setRestOptions(restOptions);
        restIgnoreProcessor.setFields(new ArrayList<>(fields));
        restIgnoreProcessor.execute(getBundle());
        return restIgnoreProcessor.getProcessingResult();
    }

    private Map<String, Boolean> findNonEditableFields(Class clazz) {
        nonEditableProcessor.setClazz(clazz);
        nonEditableProcessor.execute(getBundle());
        return nonEditableProcessor.getProcessingResult();
    }

    private void setSecurityOptions(AnnotatedElement element, EntityDto entity) {
        Access access = element.getAnnotation(Access.class);
        ReadAccess readAccess = element.getAnnotation(ReadAccess.class);
        if (null != access && !entity.isSecurityOptionsModified()) {
            Set<String> securityMembers = returnSecurityMembersForSecurityMode(access.value(), access.members(), "Access");
            entity.setSecurityMode(access.value());
            entity.setSecurityMembers(securityMembers);
            if(null == readAccess) {
                entity.setReadOnlySecurityMode(SecurityMode.NO_ACCESS);
            }
        }

        if(null != readAccess && !entity.isSecurityOptionsModified()) {
            Set<String> readOnlySecurityMembers = returnSecurityMembersForSecurityMode(readAccess.value(), readAccess.members(), "ReadAccess");
            entity.setReadOnlySecurityMode(readAccess.value());
            if(entity.getSecurityMode() == SecurityMode.EVERYONE) {
                entity.setSecurityMode(SecurityMode.NO_ACCESS);
            }
            entity.setReadOnlySecurityMembers(readOnlySecurityMembers);
        }
    }

    private Set<String> returnSecurityMembersForSecurityMode(SecurityMode securityMode, String[] securityMembersArray, String annotationName) {
        Boolean hasMembers = securityMembersArray != null && securityMembersArray.length > 0;
        Set<String> securityMembers;
        if (securityMode == SecurityMode.USERS || securityMode == SecurityMode.PERMISSIONS) {
            if (hasMembers) {
                securityMembers = new HashSet<String>(Arrays.asList(securityMembersArray));
            } else {
                throw new IllegalArgumentException(
                        "Failed to process " + annotationName+ " annotation: the security mode is set to "
                                + securityMode + " but there are no members specified."
                );
            }
        } else {
            securityMembers = new HashSet<String>();
            if (hasMembers) {
                throw new IllegalArgumentException(
                        "Failed to process " + annotationName + " annotation: the members attribute can be only used with USERS or PERMISSIONS security mode."
                );
            }
        }
        return securityMembers;
    }

    @Autowired
    public void setEntityService(EntityService entityService) {
        this.entityService = entityService;
    }

    @Autowired
    public void setTypeService(TypeService typeService) {
        this.typeService = typeService;
    }

    @Autowired
    public void setFieldProcessor(FieldProcessor fieldProcessor) {
        this.fieldProcessor = fieldProcessor;
    }

    @Autowired
    public void setUIFilterableProcessor(UIFilterableProcessor uiFilterableProcessor) {
        this.uiFilterableProcessor = uiFilterableProcessor;
    }

    @Autowired
    public void setUIDisplayableProcessor(UIDisplayableProcessor uiDisplayableProcessor) {
        this.uiDisplayableProcessor = uiDisplayableProcessor;
    }

    @Autowired
    public void setRestIgnoreProcessor(RestIgnoreProcessor restIgnoreProcessor) {
        this.restIgnoreProcessor = restIgnoreProcessor;
    }

    @Autowired
    public void setRestOperationsProcessor(RestOperationsProcessor restOperationsProcessor) {
        this.restOperationsProcessor = restOperationsProcessor;
    }

    @Autowired
    public void setCrudEventsProcessor(CrudEventsProcessor crudEventsProcessor) {
        this.crudEventsProcessor = crudEventsProcessor;
    }

    @Autowired
    public void setNonEditableProcessor(NonEditableProcessor nonEditableProcessor) {
        this.nonEditableProcessor = nonEditableProcessor;
    }
}