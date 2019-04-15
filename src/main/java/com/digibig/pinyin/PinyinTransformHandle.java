package com.digibig.pinyin;

import com.github.promeg.pinyinhelper.Pinyin;
import com.github.promeg.pinyinhelper.PinyinMapDict;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * 自动填充拼音字段的处理类
 */
public class PinyinTransformHandle {

  private Logger log = LoggerFactory.getLogger(getClass());

  private Map<String, List<WaitingProcessField>> fieldMap = new ConcurrentHashMap<>();

  //todo 怎么维护多音字词典呢？这是个问题
  static {
    Pinyin.init(Pinyin.newConfig().with(new PinyinMapDict() {
      @Override
      public Map<String, String[]> mapping() {
        HashMap<String, String[]> map = new HashMap<>();
        map.put("重庆", new String[]{"CHONG", "QING"});
        return map;
      }
    }));
  }

  /**
   * 对对象的处理
   *
   * @param classname 类名
   * @param obj 实体对象
   */
  public void handle(String classname, Object obj) {
    log.info("自动填充有@Pinyin注解的字段");
    Class<?> clazz = obj.getClass();
    try {
      List<WaitingProcessField> processFields = fieldMap.get(classname);
      if (Objects.isNull(processFields)) {
        processFields = initProcessFields(clazz);
        fieldMap.put(classname, processFields);
      }
      for (WaitingProcessField wpf : processFields) {
        Field field = wpf.getField();
        Field sourceField = wpf.getSourceField();
        String sourceFieldValue = String.class.cast(sourceField.get(obj));
        if (!StringUtils.isEmpty(sourceFieldValue)) {
          switch (wpf.getType()) {
            case QUANPIN:
              field.set(obj, Pinyin.toPinyin(sourceFieldValue, "").toLowerCase());
              break;
            case ACRONUM:
              StringBuilder builder = new StringBuilder();
              String s = Pinyin.toPinyin(sourceFieldValue, ",");
              String[] words = s.split(",");
              for (String word : words) {
                builder.append(word.charAt(0));
              }
              field.set(obj, builder.toString().toLowerCase());
              break;
          }
        }
      }
      log.debug("填充后的实体对象为：{}", obj);
    } catch (Exception e) {
      log.error("自动填充拼音字段失败", e);
    }
  }

  //初始化实体类需要填充的字段和目标字段，存入map缓存
  private List<WaitingProcessField> initProcessFields(Class<?> clazz) throws NoSuchFieldException {
    List<WaitingProcessField> result = new ArrayList<>();
    Field[] fields = clazz.getDeclaredFields();
    for (Field field : fields) {
      ToPinyin toPinyin = field.getAnnotation(ToPinyin.class);
      if (Objects.nonNull(toPinyin)) {
        Field sourceField = clazz.getDeclaredField(toPinyin.sourceField());
        field.setAccessible(true);
        sourceField.setAccessible(true);
        result.add(new WaitingProcessField(field, sourceField, toPinyin.type()));
      }
    }
    log.debug("首次调用 初始化需填充field完毕");
    return result;
  }
}
