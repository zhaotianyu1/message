package com.juphoon.chatbotmaap.chatbotcard;

public class ChatbotCardCSSBean {
    public ChatbotCardCSSBean.MessageBean messageBean;
    public ChatbotCardCSSBean.TitleBean titleBean;
    public ChatbotCardCSSBean.DescriptionBean descriptionBean;
    public ChatbotCardCSSBean.SuggestionsBean suggestionsBean;

    public ChatbotCardCSSBean(ChatbotCardCSSBean.MessageBean messageBean, ChatbotCardCSSBean.TitleBean titleBean, ChatbotCardCSSBean.DescriptionBean descriptionBean, ChatbotCardCSSBean.SuggestionsBean suggestionsBean) {
        this.messageBean = messageBean;
        this.titleBean = titleBean;
        this.descriptionBean = descriptionBean;
        this.suggestionsBean = suggestionsBean;
    }

    public ChatbotCardCSSBean() {
        messageBean = new ChatbotCardCSSBean.MessageBean();
        titleBean = new ChatbotCardCSSBean.TitleBean();
        descriptionBean = new ChatbotCardCSSBean.DescriptionBean();
        suggestionsBean = new ChatbotCardCSSBean.SuggestionsBean();
    }

    public class MessageBean {
        public String color;
        public String fontSize;
        public String fontFamily;
        public String fontWeight;
        public String textAlign;
        public String backgroudColor;
        public String backgroudImage;
    }

    public class TitleBean {
        public String color;
        public String fontSize;
        public String fontWeight;
        public String fontFamily;
        public String textAlign;
    }

    public class DescriptionBean {
        public String color;
        public String fontSize;
        public String fontWeight;
        public String fontFamily;
        public String textAlign;
    }

    public class SuggestionsBean {
        public String color;
        public String fontSize;
        public String fontWeight;
        public String fontFamily;
        public String textAlign;
    }
}