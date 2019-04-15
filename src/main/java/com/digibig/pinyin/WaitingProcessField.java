package com.digibig.pinyin;

import java.lang.reflect.Field;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 待填充字段和目标字段的对应关系
 *
 * @author yueyakun
 * @date 2019/4/10 17:43
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WaitingProcessField {

  //需填充字段
  private Field field;
  //原字段
  private Field sourceField;
  //填充类型
  private PinyinType type;
}
