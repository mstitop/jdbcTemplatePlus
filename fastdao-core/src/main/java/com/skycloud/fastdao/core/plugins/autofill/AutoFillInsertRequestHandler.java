/**
 * @(#)AutoFillInsertRequestHandler.java, 10月 13, 2019.
 * <p>
 * Copyright 2019 fenbi.com. All rights reserved.
 * FENBI.COM PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.skycloud.fastdao.core.plugins.autofill;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.skycloud.fastdao.core.ast.request.InsertRequest;
import com.skycloud.fastdao.core.mapping.ColumnMapping;
import com.skycloud.fastdao.core.mapping.RowMapping;
import com.skycloud.fastdao.core.plugins.PluggableHandler;
import com.skycloud.fastdao.core.reflection.MetaClass;
import com.skycloud.fastdao.core.reflection.MetaField;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author yuntian
 */
public class AutoFillInsertRequestHandler implements PluggableHandler<InsertRequest> {

    private Map<Class, Map<String, AutoFillHandler>> autoFillHandlerMap = Maps.newHashMap();

    @Override
    public InsertRequest handle(InsertRequest pluggable, Class clazz) {
        Map<String, AutoFillHandler> fieldAutoFillHandlerMap = autoFillHandlerMap.get(clazz);
        if (fieldAutoFillHandlerMap == null) {
            fieldAutoFillHandlerMap = fillAutoFillHandlerMap(clazz);
        }
        Set<String> prepareUpdate = pluggable.getInsertFields().entrySet().stream()
                .filter(entry -> entry.getValue() != null).map(Entry::getKey)
                .collect(Collectors.toSet());
        Set<String> needUpdateFields = Sets.newHashSet(fieldAutoFillHandlerMap.keySet());
        needUpdateFields.removeAll(prepareUpdate);
        for (String needUpdateField : needUpdateFields) {
            AutoFillHandler handler = fieldAutoFillHandlerMap.get(needUpdateField);
            pluggable.addInsertField(needUpdateField, handler.handle(pluggable));
        }
        return pluggable;
    }

    private Map<String, AutoFillHandler> fillAutoFillHandlerMap(Class clazz) {
        MetaClass metaClass = MetaClass.of(clazz);
        RowMapping rowMapping = RowMapping.of(clazz);
        Map<String, AutoFillHandler> fieldAutoFillHandlerMap = Maps.newHashMap();
        for (MetaField metaField : metaClass.metaFields()) {
            AutoFill autoFill = metaField.getAnnotation(AutoFill.class);
            if (autoFill == null) {
                continue;
            }
            ColumnMapping columnMapping = rowMapping.getColumnMappingByFieldName(metaField.getFieldName());
            if (columnMapping == null) {
                continue;
            }
            fieldAutoFillHandlerMap.put(columnMapping.getColumnName(), createHandler(autoFill));
        }
        autoFillHandlerMap.put(clazz, fieldAutoFillHandlerMap);
        return fieldAutoFillHandlerMap;
    }

    private AutoFillHandler createHandler(AutoFill autoFill) {
        boolean match = false;
        for (AutoFillOperation operation : autoFill.onOperation()) {
            if (operation == AutoFillOperation.INSERT) {
                match = true;
            }
        }
        if (!match) {
            return null;
        }
        if (autoFill.fillValue() == AutoFillValueEnum.NOW) {
            return new NowDateAutoFillHandler();
        }
        if (autoFill.handler() != AutoFillHandler.class) {
            try {
                return autoFill.handler().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("create autoFill handler fail");
            }
        }
        throw new RuntimeException("autoFill annotation exception");
    }


}