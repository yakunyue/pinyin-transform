package com.fxg.pinyin;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;
import javassist.Modifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

class PinyinTransformScanner extends ClassPathBeanDefinitionScanner {

  private Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * 基类的class
   */
  private static Class serviceClass;

  static {
    try {
      serviceClass = Class.forName("com.digibig.spring.service2.AbstractService");
    } catch (Exception e) {
      throw new Error(e);
    }
  }

  PinyinTransformScanner(BeanDefinitionRegistry registry) {
    super(registry, false);
  }

  /**
   * Calls the parent search that will search and register all the candidates. Then the registered objects are post processed to set them as SpiSelectorFactoryBeans
   */
  @Override
  public Set<BeanDefinitionHolder> doScan(String... basePackages) {
        /*添加拼音转换service 的过滤器*/
    addIncludeFilter(((metadataReader, metadataReaderFactory) -> {
      try {
        String className = metadataReader.getClassMetadata().getClassName();
        return isHanzi2PinyinService(Class.forName(className));
      } catch (Exception e) {
        return false;
      }
    }));
        /*添加package-info的过滤器*/
    addExcludeFilter((reader, factory) -> reader.getClassMetadata().getClassName().endsWith("package-info"));
    return super.doScan(basePackages);
  }

  /**
   * 是否是spring的注册bean
   *
   * @param beanDefinition bean的定义
   * @return 是否是托管给spring管理的Bean
   */
  @Override
  protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
    try {
      return isHanzi2PinyinService(Class.forName(beanDefinition.getBeanClassName()));
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * 扫描的过滤器
   */
  @Override
  protected boolean isCandidateComponent(MetadataReader metadataReader) throws IOException {
    super.isCandidateComponent(metadataReader);
    return true;
  }

  /**
   * 是否是需要汉字转拼音的service
   *
   * @param clazz 实体泛型
   */
  @SuppressWarnings("all")
  private boolean isHanzi2PinyinService(Class clazz) {
    try {
            /* 1.继承自AbstractService   2.非抽象类   3.有Service注解*/
      if (serviceClass.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers()) &&
          (clazz.getAnnotation(Service.class) != null || clazz.getAnnotation(Component.class) != null || clazz.getAnnotation(Repository.class) != null)) {
        //取service的泛型类
        Type type = clazz.getGenericSuperclass();
        ParameterizedType pt = ParameterizedType.class.cast(type);
        Class domainClass = Class.class.cast(pt.getActualTypeArguments()[0]);
        Field[] fields = domainClass.getDeclaredFields();
        for (Field field:fields){
          Annotation annotation = field.getAnnotation(ToPinyin.class);
          if (annotation != null) {
            return true;
          }
        }
      }
    } catch (Exception e) {
    }
    return false;
//        return AbstractService.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers()) && clazz.getAnnotation(Service.class) != null;
  }

  /**
   * spring 扫描会和之前的BeanDefinition进行比较 我们在这里进行替换className就能达到效果
   */
  @Override
  protected boolean isCompatible(BeanDefinition newDefinition, BeanDefinition existingDefinition) {
    String className = existingDefinition.getBeanClassName();
    boolean flag = super.isCompatible(newDefinition, existingDefinition);
    try {
      if (flag && isHanzi2PinyinService(Class.forName(className))) {
        JavassistPinyinDynamicAdvise pinyinDynamicAdvise = new JavassistPinyinDynamicAdvise();
        Class clazz = Class.forName(className);
        Class newClass = pinyinDynamicAdvise.advise(clazz);
                /*使用代理生成的class调换掉之前的*/
        existingDefinition.setBeanClassName(newClass.getName());
        logger.info("替换前className {} 替换后className {}", className, newClass.getName());
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return flag;
  }

  private Class getClassname(Class clazz) throws Exception {
    Type type = clazz.getGenericSuperclass();
    ParameterizedType pt = ParameterizedType.class.cast(type);
    Class domainClass = Class.class.cast(pt.getActualTypeArguments()[0]);
    return Class.forName(domainClass.getName());
  }
}
