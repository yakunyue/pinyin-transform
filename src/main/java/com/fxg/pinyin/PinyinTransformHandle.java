package com.fxg.pinyin;

import com.github.promeg.pinyinhelper.Pinyin;
import com.github.promeg.pinyinhelper.PinyinMapDict;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

/**
 * 自动填充拼音字段的处理类
 */
public class PinyinTransformHandle {

  private Logger log = LoggerFactory.getLogger(getClass());

  private Map<String, List<WaitingProcessField>> fieldMap = new ConcurrentHashMap<>();

  /**
   * 在初始化块中加载多音字词典
   */ {
    log.info("开始加载多音字词典");
    Pinyin.init(Pinyin.newConfig().with(new PinyinMapDict() {
      @Override
      public Map<String, String[]> mapping() {
        BufferedReader br = null;
        HashMap<String, String[]> map = new HashMap<>();
        try {
          ClassPathResource resource = new ClassPathResource("duoyinzi.txt");
          br = new BufferedReader(new InputStreamReader(resource.getInputStream(), "UTF-8"));
          String line;
          while ((line = br.readLine()) != null) {
            String[] arr = line.split("#");
            map.put(arr[0], arr[1].split(" "));
          }
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          if (br != null) {
            try {
              br.close();
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        }
        return map;
      }
    }));
    log.info("多音字词典加载完毕");
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
        //限定原字段类型必须为String，否则不处理
        if (sourceField.getType().equals(String.class)) {
          field.setAccessible(true);
          sourceField.setAccessible(true);
          result.add(new WaitingProcessField(field, sourceField, toPinyin.type()));
        }
      }
    }
    log.debug("首次调用 初始化需填充field完毕");
    return result;
  }
}
