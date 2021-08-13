package com.juphoon.chatbotmaap;

import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * 字符转拼音的帮助类
 */
public class PinyinHelper {
    private static PinyinHelper instance;
    private Properties properties = null;


    private PinyinHelper() {
        initResource();
    }

    public static String[] getUnformattedHanyuPinyinStringArray(char ch) {
        return getInstance().getHanyuPinyinStringArray(ch);
    }

    public static PinyinHelper getInstance() {
        if (instance == null) {
            instance = new PinyinHelper();
        }
        return instance;
    }

    private void initResource() {
        try {
            final String resourceName = "/assets/unicode_to_hanyu_pinyin.txt";
//          final String resourceName = "/assets/unicode_py.ini";
            properties = new Properties();
            properties.load(getResourceInputStream(resourceName));

        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private BufferedInputStream getResourceInputStream(String resourceName) {
        return new BufferedInputStream(PinyinHelper.class.getResourceAsStream(resourceName));
    }

    private String[] getHanyuPinyinStringArray(char ch) {
        String pinyinRecord = getHanyuPinyinRecordFromChar(ch);

        if (null != pinyinRecord) {
            int indexOfLeftBracket = pinyinRecord.indexOf(Field.LEFT_BRACKET);
            int indexOfRightBracket = pinyinRecord.lastIndexOf(Field.RIGHT_BRACKET);

            String stripedString = pinyinRecord.substring(indexOfLeftBracket
                    + Field.LEFT_BRACKET.length(), indexOfRightBracket);

            return stripedString.split(Field.COMMA);

        } else
            return null;

    }

    private String getHanyuPinyinRecordFromChar(char ch) {
        int codePointOfChar = ch;
        String codepointHexStr = Integer.toHexString(codePointOfChar).toUpperCase();
        String foundRecord = properties.getProperty(codepointHexStr);
        return foundRecord;
    }

    /**
     * 汉字转简拼
     */
    public static class PinYin4j {
        private static final String SPACE = "&&";//分隔符

        /**
         * 获取关键字,用 && 分隔
         *
         * @param name 名称
         * @param serviceDescription 服务描述
         * @return
         */
        public static String getSearchKeyword(String name, String serviceDescription) {
            if (TextUtils.isEmpty(name) && TextUtils.isEmpty(serviceDescription)) {
                return null;
            }
            StringBuilder stringBuilder = new StringBuilder();

            if (!TextUtils.isEmpty(name)) {
                //获取 key
                stringBuilder.append(name);
                stringBuilder.append(SPACE);
                //获取 key 拼音首字母
                for (String s : getPinyin(name, true)) {
                    stringBuilder.append(s);
                    stringBuilder.append(SPACE);
                }
                //获取 key 拼音全拼
                for (String s : getPinyin(name, false)) {
                    stringBuilder.append(s);
                    stringBuilder.append(SPACE);
                }
            }
            if (!TextUtils.isEmpty(serviceDescription)) {
                //多音字过多可能会导致内存溢出，暂时不存服务描述的拼音和首字母
                stringBuilder.append(serviceDescription);
            }
            Log.d("RcsIm", stringBuilder.toString());
            //处理关键词中可能出现的sql语句通配符，? 转换成 [?]，_转换成 [_]
            return stringBuilder.toString().replace("?", "[?]").replace("_", "[_]");
        }

        /**
         * 获取拼音字母集合
         *
         * @param src 要转换的字符串
         * @param isFirstCharacterSet true 首字母集合；false 全拼集合
         * @return Set<String>
         */
        public static Set<String> getPinyin(String src, boolean isFirstCharacterSet) {
            char[] srcChar;
            srcChar = src.toCharArray();

            //1:多少个汉字
            //2:每个汉字多少种读音
            String[][] temp = new String[src.length()][];
            for (int i = 0; i < srcChar.length; i++) {
                char c = srcChar[i];
                // 是中文或者a-z或者A-Z转换拼音(我的需求，是保留中文或者a-z或者A-Z)
                if (String.valueOf(c).matches("[\\u4E00-\\u9FA5]+")) {
                    String[] t = PinyinHelper.getUnformattedHanyuPinyinStringArray(c);
                    temp[i] = new String[t.length];
                    for (int j = 0; j < t.length; j++) {
                        if (isFirstCharacterSet) {
                            temp[i][j] = t[j].substring(0, 1);//获取汉字首字母
                        } else {
                            temp[i][j] = t[j].substring(0, 1);//获取汉字全拼
                            temp[i][j] = t[j].replaceAll("\\d+", "");//去掉声调
                        }
                    }
                } else {
                    temp[i] = new String[]{String.valueOf(srcChar[i])};
                }

            }
            String[] pingyinArray = paiLie(temp);
            return array2Set(pingyinArray);//为了去掉重复项
        }

        /**
         * 获取拼音列表集合，一个字符是一个集合，一个集合有几种读音，例如唯品会：[wei],[pin],[kuai,hui]
         *
         * @param src
         * @return
         */
        public static ArrayList<Set<String>> getPinyinList(String src) {
            char[] srcChar;
            srcChar = src.toCharArray();

            //1:多少个汉字
            //2:每个汉字多少种读音
            String[][] temp = new String[src.length()][];
            for (int i = 0; i < srcChar.length; i++) {
                char c = srcChar[i];
                // 是中文或者a-z或者A-Z转换拼音(我的需求，是保留中文或者a-z或者A-Z)
                if (String.valueOf(c).matches("[\\u4E00-\\u9FA5]+")) {
                    String[] t = PinyinHelper.getUnformattedHanyuPinyinStringArray(c);
                    temp[i] = new String[t.length];
                    for (int j = 0; j < t.length; j++) {
                        temp[i][j] = t[j].substring(0, 1);//获取汉字全拼
                        temp[i][j] = t[j].replaceAll("\\d+", "");//去掉声调
                    }
                } else {
                    temp[i] = new String[]{String.valueOf(srcChar[i])};
                }

            }

            ArrayList<Set<String>> list = new ArrayList<>();
            for (int i = 0; i < temp.length; i++) {
                Set<String> strings = new HashSet<>();
                for (int j = 0; j < temp[i].length; j++) {
                    strings.add(temp[i][j]);
                }
                list.add(strings);
            }
            return list;
        }

        /*
         * 求2维数组所有排列组合情况
         * 比如:{{1,2},{3},{4},{5,6}}共有2中排列,为:1345,1346,2345,2346
         */
        private static String[] paiLie(String[][] str) {
            int max = 1;
            for (int i = 0; i < str.length; i++) {
                max *= str[i].length;
            }
            String[] result = new String[max];
            for (int i = 0; i < max; i++) {
                String s = "";
                int temp = 1;      //注意这个temp的用法。
                for (int j = 0; j < str.length; j++) {
                    temp *= str[j].length;
                    s += str[j][i / (max / temp) % str[j].length];
                }
                result[i] = s;
            }

            return result;
        }

        public static <T extends Object> Set<T> array2Set(T[] tArray) {
            Set<T> tSet = new HashSet<T>(Arrays.asList(tArray));
            // TODO 没有一步到位的方法，根据具体的作用，选择合适的Set的子类来转换。
            return tSet;
        }

    }

    class Field {
        static final String LEFT_BRACKET = "(";
        static final String RIGHT_BRACKET = ")";
        static final String COMMA = ",";
    }
}
