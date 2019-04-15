package com.digibig.pinyin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.List;
import javassist.ClassClassPath;
import javassist.ClassMap;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.ConstPool;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.ParameterAnnotationsAttribute;
import org.springframework.beans.factory.annotation.Autowired;

class JavassistPinyinDynamicAdvise implements PinyinDynamicAdvise {

  @Override
  public Class advise(Class deletgate) throws Exception {
    ClassPool pool = ClassPool.getDefault();
    pool.appendClassPath(new ClassClassPath(deletgate));
    pool.appendClassPath(new ClassClassPath(Autowired.class));
    pool.appendClassPath(new ClassClassPath(PinyinTransformHandle.class));
        /*代理类一般以 $开头*/
    CtClass cc = pool.makeClass("com.digibig.javassit.$" + deletgate.getSimpleName());
        /*代理类继承原类*/
    cc.setSuperclass(pool.get(deletgate.getName()));
        /*添加PinyinTransformHandle 的 autowired的字段*/
    ConstPool cpool = cc.getClassFile().getConstPool();
    CtField pinyinTransformHandleField = new CtField(pool.getCtClass(PinyinTransformHandle.class.getName()), "pinyinTransformHandle", cc);
    pinyinTransformHandleField.setModifiers(Modifier.PRIVATE);
    javassist.bytecode.annotation.Annotation annotation = new javassist.bytecode.annotation.Annotation(Autowired.class.getName(), cpool);
    AnnotationsAttribute attr = new AnnotationsAttribute(cpool, AnnotationsAttribute.visibleTag);
    attr.addAnnotation(annotation);
    pinyinTransformHandleField.getFieldInfo().addAttribute(attr);
    cc.addField(pinyinTransformHandleField);

        /*添加className字段*/
    CtField classNameField = CtField.make("private final String className = \"" + getClassname(deletgate) + "\";", cc);
    cc.addField(classNameField);

        /*添加构造函数*/
    CtClass origin = pool.get(deletgate.getName());
    Constructor[] declaredConstructors = deletgate.getDeclaredConstructors();
    for (Constructor constructor : declaredConstructors) {
            /*只添加带参数的public的构造方法*/
      if (constructor.getModifiers() == java.lang.reflect.Modifier.PUBLIC) {
                /*原来的参数*/
        Parameter[] parameters = constructor.getParameters();
        StringBuilder body = new StringBuilder("super(");
        CtClass[] ctParameters = new CtClass[constructor.getParameterCount()];
        CtConstructor ctConstructor;
        if (parameters.length == 0) {
          ctConstructor = new CtConstructor(ctParameters, cc);
          body.append(");");
        } else {
          for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            pool.appendClassPath(new ClassClassPath(parameter.getType()));
            ctParameters[i] = pool.getCtClass(parameters[i].getType().getName());
            body.append("$").append(i + 1).append(",");
          }
          body.setCharAt(body.length() - 1, ')');
          body.append(";");
          CtConstructor originDeclaredConstructor = origin.getDeclaredConstructor(ctParameters);
          MethodInfo methodInfo = originDeclaredConstructor.getMethodInfo();
          ctConstructor = new CtConstructor(originDeclaredConstructor, cc, new ClassMap());
          List<AttributeInfo> attributes = methodInfo.getAttributes();
          for (AttributeInfo attribute : attributes) {
                        /*只保留参数注解和方法注解的描述*/
            if (attribute instanceof ParameterAnnotationsAttribute || attribute instanceof AnnotationsAttribute) {
              ctConstructor.getMethodInfo().addAttribute(attribute);
            }
          }
        }
        ctConstructor.setBody(body.toString());
        ctConstructor.setModifiers(Modifier.PUBLIC);
        cc.addConstructor(ctConstructor);
      }
    }


        /*添加preAdd方法*/
    CtMethod preAdd = new CtMethod(pool.get(Void.TYPE.getName()), "preAdd", new CtClass[]{pool.get("java.lang.Object")}, cc);
    preAdd.setModifiers(Modifier.PROTECTED);
    preAdd.setBody("{\n\tsuper.preAdd($1);\n\tpinyinTransformHandle.handle(className,$1);\n}");
    cc.addMethod(preAdd);

        /*添加preUpdate方法*/
    CtMethod preUpdate = new CtMethod(pool.get(Void.TYPE.getName()), "preUpdate", new CtClass[]{pool.get("java.lang.Object")}, cc);
    preUpdate.setModifiers(Modifier.PROTECTED);
    preUpdate.setBody("{\n\tsuper.preUpdate($1);pinyinTransformHandle.handle(className,$1);\n}");
    cc.addMethod(preUpdate);
    return cc.toClass();
  }
}
