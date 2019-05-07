package com.fxg.pinyin;

import com.github.promeg.pinyinhelper.Pinyin;
import com.hankcs.hanlp.HanLP;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

/**
 * 准备用文件流清洗出TinyPinyin能用的多音字词库
 *
 * @author yueyakun
 * @date 2019/5/7 10:19
 */

public class Dictionary {

  public static void main(String[] args) {
//    make_duoyinzi_store();
//    make_duoyinzi_dictionary();
  }

  //生成多音字字典，每个词占一行
  private static void make_duoyinzi_dictionary() {
    BufferedReader br = null;
    BufferedWriter bw = null;
    try {
      File file = new File("F:\\duoyinzi2-1.txt");
      br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));

      bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File("F:\\duoyinzi2-2.txt")), "UTF-8"));
      String words;
      ArrayList<Object> list = new ArrayList<>();
      while ((words = br.readLine()) != null) {
        words = words.trim();
        //利用list去一下重
        if (!list.contains(words)) {
          //将词语的拼音拼在词语后面，用#隔开，拼音之间用空格隔开
          String pinyinString = HanLP.convertToPinyinString(words, " ", false);
          String result = words + "#" + pinyinString;
          System.out.println(result);
          list.add(result);
          bw.newLine();
          bw.write(result);
        }
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

      if (bw != null) {
        try {
          bw.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  //生成多音字词库，每个词占一行
  private static void make_duoyinzi_store() {
    BufferedReader br = null;
    BufferedWriter bw = null;
    try {
      File file = new File("F:\\duoyinzi2.txt");
      br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));

      bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File("F:\\duoyinzi2-1.txt"))));
      String line;
      while ((line = br.readLine()) != null) {
        line = line.trim();

        char[] chars = line.toCharArray();
        for (int i = 0; i < chars.length; i++) {
          if (!Pinyin.isChinese(chars[i])) {
            chars[i] = ' ';
          }
        }
        String newLine = String.valueOf(chars);
        newLine = newLine.trim();
        newLine = newLine.replaceAll(" +", " ");
        if (newLine.length() > 1) {
          //将处理好的多音字（不含拼音）写入文档
          String[] arr = newLine.split(" ");
          for (int i = 0; i < arr.length; i++) {
            if (arr[i].length() > 1) {
              System.out.println(arr[i]);
              bw.newLine();
              bw.write(arr[i]);
            }
          }
        }
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

      if (bw != null) {
        try {
          bw.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

}
