package com.digibig.pinyin;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;

/**
 * spring先调用 ResourceLoaderAware.setResourceLoader 再调用ImportBeanDefinitionRegistrar的registerBeanDefinitions注册bean
 */
class PinyinTransformRegister implements ImportBeanDefinitionRegistrar, ResourceLoaderAware {

  private Logger logger = LoggerFactory.getLogger(getClass());
  /**
   * 资源加载器
   */
  private ResourceLoader resourceLoader;

  @Override
  public void setResourceLoader(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  @Override
  public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
    String[] packages = new String[]{"com.digibig"};
    try {
            /*获取启动类的注解 ，在接口上使用@mport，metadata是StandardAnnotationMetadata（本次使用）
             * 如果在实体类上 使用 @Configuration和@Import,metadata是AnnotationMetadataReadingVisitor
             * */
      AnnotationAttributes annoAttrs = AnnotationAttributes.fromMap(
          metadata.getAnnotationAttributes(EnablePinyinTransform.class.getName(), true));
            /*获取拼音转换扫描的包路径*/
      packages = annoAttrs.getStringArray("value");
    } catch (Exception e) {
      logger.warn("获取拼音转换扫描包路径时发生错误", e);
    }
    if (packages != null) {
      Arrays.stream(packages).forEach(str -> logger.info("获取拼音转换扫描包路径： {}", str));
    }
    PinyinTransformScanner scanner = new PinyinTransformScanner(registry);
    scanner.setResourceLoader(this.resourceLoader);
    try {
      scanner.doScan(packages);
    } catch (Exception ex) {
      logger.error("Could not determine auto-configuration package, es index scanning disabled.", ex);
    }
  }
}
