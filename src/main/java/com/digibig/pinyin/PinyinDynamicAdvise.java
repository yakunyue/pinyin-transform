package com.digibig.pinyin;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 对class的代码增强
 * 重新生成新的class
 */
@FunctionalInterface
public interface PinyinDynamicAdvise {

    /**
     * @param deletgate 委托的 class
     * @return 新的class
     */
    Class advise(Class deletgate) throws Exception;

    /**
     * 获取泛型的classname
     *
     * @param clazz
     * @return
     */
    @SuppressWarnings("all")
    default String getClassname(Class clazz) {
        Type type = clazz.getGenericSuperclass();
        ParameterizedType pt = ParameterizedType.class.cast(type);
        Class domainClass = Class.class.cast(pt.getActualTypeArguments()[0]);
        return domainClass.getName();
    }
}
