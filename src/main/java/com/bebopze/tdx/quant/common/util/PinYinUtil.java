package com.bebopze.tdx.quant.common.util;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.*;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;


/**
 * 拼音
 *
 * @author: bebopze
 * @date: 2025/5/12
 */
@Slf4j
public class PinYinUtil {


    private static final HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();


    static {
        format.setCaseType(HanyuPinyinCaseType.UPPERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        format.setVCharType(HanyuPinyinVCharType.WITH_V);
    }


    /**
     * 汉字 -> 全拼     （中国 -> ZHONGGUO）
     *
     * @param chinese 汉字
     * @return
     */
    public static String toFullPinyin(String chinese) {
        StringBuilder pinyin = new StringBuilder();

        for (char c : chinese.toCharArray()) {
            if (isChinese(c)) {

                try {

                    String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, format);
                    if (pinyinArray != null) {
                        pinyin.append(pinyinArray[0]);
                    }

                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    e.printStackTrace();
                }


            } else {
                pinyin.append(c);
            }
        }


        return pinyin.toString();
    }


    /**
     * 汉字 -> 首字母       （中国 -> ZG）
     *
     * @param chinese
     * @return
     */
    public static String toFirstLetters(String chinese) {

        StringBuilder initials = new StringBuilder();

        for (char c : chinese.toCharArray()) {
            if (isChinese(c)) {

                try {

                    String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, format);
                    if (pinyinArray != null) {
                        initials.append(pinyinArray[0].charAt(0));
                    }

                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    e.printStackTrace();
                }


            } else {

                initials.append(Character.toUpperCase(c));
            }
        }


        return initials.toString();
    }


    /**
     * 判断是否为 中文字符
     *
     * @param c
     * @return
     */
    private static boolean isChinese(char c) {
        return String.valueOf(c).matches("[\\u4E00-\\u9FA5]");
    }


    public static void main(String[] args) {

        String str = "通达信";

        System.out.println(toFullPinyin(str));
        System.out.println(toFirstLetters(str));
    }
}