package com.fxg.pinyin;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

/**
 * 拼音转换启动类的注解
 */
@Documented
@Target(ElementType.TYPE)
/*导入scanner和拼音转换的处理类，客户端避免使用@ComponentScan注解*/
@Import({PinyinTransformRegister.class, PinyinTransformHandle.class})
@Retention(RetentionPolicy.RUNTIME)
public @interface EnablePinyinTransform {

  String[] value() default "com.digibig";
}
