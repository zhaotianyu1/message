package com.juphoon.chatbotmaap;

import java.util.ArrayList;
import java.util.Set;

public class RcsChatbotSearchKeywordBean {
    /**
     * 名字，比如唯品会
     */
    private String name;
    /**
     * 名字拼音的缩写，比如唯品会：[wph,wpk]
     */
    private Set<String> namePYSet;
    /**
     * 名字拼音集合，比如唯品会，[wei],[pin],[hui,kuai]
     */
    private ArrayList<Set<String>> namePinyinList = new ArrayList<>();

    public RcsChatbotSearchKeywordBean(String name, Set<String> namePYSet, ArrayList<Set<String>> namePinyinList) {
        this.name = name;
        this.namePYSet = namePYSet;
        this.namePinyinList = namePinyinList;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getNamePYSet() {
        return namePYSet;
    }

    public void setNamePYSet(Set<String> namePYSet) {
        this.namePYSet = namePYSet;
    }

    public ArrayList<Set<String>> getNamePinyinList() {
        return namePinyinList;
    }

    public void setNamePinyinList(ArrayList<Set<String>> namePinyinList) {
        this.namePinyinList = namePinyinList;
    }
}
