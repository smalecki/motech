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
public class EntityExtensionProcessor extends EntityProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityExtensionProcessor.class);

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

    protected void execute(Bundle bundle, SchemaHolder schemaHolder, List<EntityProcessorOutput> processingResult) {
        this.processingResult = processingResult;
        super.execute(bundle, schemaHolder, false);
    }

    private Collection<FieldDto> findFields(Class clazz, EntityDto entity, Collection<FieldDto> existFields) {
        fieldProcessor.setExistFields(existFields);
        return findFields(clazz, entity);
    }
}