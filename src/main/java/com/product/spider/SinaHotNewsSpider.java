package com.product.spider;


import cn.hutool.http.HttpUtil;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.product.constant.NewsTypeConstant;

import com.product.entity.ArticleNews;
import com.product.service.ArticleNewsService;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
//import com.gargoylesoftware.htmlunit.WebClient;
//import com.gargoylesoftware.htmlunit.html.HtmlPage;
////import org.jeecg.modules.biz.article.news.constant.NewsTypeConstant;
////import org.jeecg.modules.biz.article.news.domain.entity.HotNewsEntity;
////import org.jeecg.modules.biz.article.news.service.IHotNewsService;
////import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;



/**
 * @ClassName : NewsSpider
 * @Description : 新闻爬虫
 * @Author : ZQCN
 * @Date: 2025-02-17 11:25
 */
@Service
public class SinaHotNewsSpider {

    @Autowired
    private ArticleNewsService hotNewsService;

    /**
     * 新闻来源
     */
    private static final String SOURCE = "新浪新闻";
    /**
     * 新闻采集url
     */
    private static final String SINA_URL = "https://news.sina.com.cn/hotnews/#8";

    /**
     * 对应新闻板块div标签的id
     */
    public static final Map<String, String> SINA_NEWS_TAG = new HashMap<String, String>() {{
        put(NewsTypeConstant.SINA_DOMESTIC_NEWS, "Con21");
        put(NewsTypeConstant.SINA_FOREIGN_NEWS, "Con31");
        put(NewsTypeConstant.SINA_SOCIETY_NEWS, "Con41");
        put(NewsTypeConstant.SINA_SPORTS_NEWS, "Con51");
        put(NewsTypeConstant.SINA_FINANCE_NEWS, "Con71");
        put(NewsTypeConstant.SINA_AMUSEMENT_NEWS, "Con81");
        put(NewsTypeConstant.SINA_TECHNOLOGY_NEWS, "Con61");
    }};


    /**
     * 新浪社会新闻爬虫
     *
     * @param
     * @return
     * @Author: ZQCN
     * @Updator: ZQCN
     * @Date 2025/2/17 11:28
     */
    public void sinaNewsSpider(String type) {
        try {
            // 创建WebClient对象
            WebClient client = new WebClient();
            // 开启自动解析JavaScript
            client.getOptions().setJavaScriptEnabled(true);
            client.getOptions().setCssEnabled(false);
            client.getOptions().setThrowExceptionOnScriptError(false);
            // 打开网页，获取HtmlPage对象
            client.waitForBackgroundJavaScript(30000);
            // 页面源代码
            HtmlPage htmlPage = client.getPage(SINA_URL);
            // HTML字符串
            String htmlStr = htmlPage.asXml();
            // 解析HTML字符串
            Document doc = Jsoup.parse(htmlStr);
            // 定位到id为Con41的div
            String divId = SINA_NEWS_TAG.get(type);
            Element con41Div = doc.getElementById(divId);
            if (con41Div != null) {
                // 获取该div下的所有表格行
                Elements rows = con41Div.select("table tbody tr");
                // 热度索引：同一分类内列表越靠前热度越高
                int hotIndex = 0;
                for (Element row : rows) {
                    // 确保是数据行
                    if (row.select("td").size() > 0) {
                        // 获取包含标题和链接的单元格
                        Element titleColumn = row.select("td").get(1);
                        // 日期
                        Element dateColumn = row.select("td").get(3);
                        // 媒体
                        Element mediaColumn = row.select("td").get(2);
                        if (titleColumn != null && dateColumn != null) {
                            // 提取链接和标题
                            Element link = titleColumn.select("a").first();
                            if (link != null) {
                                // 标题
                                String title = link.text();
                                // 链接
                                String detailHref = link.attr("href");
                                // 新闻日期(格式：02-18 00:57)
                                String dateStr = dateColumn.text().trim();
                                // 媒体
                                String media = mediaColumn.html();
                                // 获取文章详情
                                String detailHtmlStr = HttpUtil.get(detailHref);
                                Document detailDoc = Jsoup.parse(detailHtmlStr);
                                Element articleDiv = detailDoc.getElementById("article");
                                /**
                                 * 科技新闻文章详情id:artibody
                                 * 国内新闻文章详情id:article
                                 * 国际新闻文章详情id:article
                                 * 社会新闻文章详情id:article
                                 * 体育新闻文章详情id:artibody
                                 * 娱乐新闻文章详情id:artibody
                                 */
                                if (articleDiv == null) {
                                    articleDiv = detailDoc.getElementById("artibody");
                                    if (articleDiv == null) {
                                        continue;
                                    }
                                }


                                Elements children = articleDiv.children();
                                String str = "";
                                Integer picCount = 0;
                                String picList = "";
                                for (Element child : children) {
                                    if (child.tagName().equalsIgnoreCase("p")) {
                                        // 提取p标签的文本
                                        str += child.text();
                                    }
                                    else if (child.tagName().equalsIgnoreCase("div") && child.hasClass("img_wrapper")) {
                                        // 提取img_wrapper div内的img标签的src属性
                                        Element img = child.selectFirst("img");
                                        if (img != null) {
                                            str += "{"+ picCount +"}";
                                            picList += img.attr("src")+";";
                                            picCount++;
                                        }
                                    }
                                }

                                ArticleNews hotNewsEntity = new ArticleNews();
                                hotNewsEntity.setSource(SOURCE);
                                hotNewsEntity.setTitle(title);
                                hotNewsEntity.setContent(str);
                                hotNewsEntity.setUrl(detailHref);
                                hotNewsEntity.setType(type);
                                hotNewsEntity.setMedia(media);
                                hotNewsEntity.setPicCount(picCount);
                                hotNewsEntity.setPicList(picList);
                                hotNewsEntity.setWordNum(str.length());
                                hotNewsEntity.setNewsDate(parseNewsDate(dateStr));
                                hotNewsEntity.setHot(Math.max(1, 100 - hotIndex));
                                hotIndex++;
                                hotNewsService.updateOrSaveSinaNewsBy(hotNewsEntity);
                            }
                        }
                    }
                }
            }
            else {
                System.out.println("未找到ID为" + divId + "的div");
            }
        } catch (IOException e) {
            System.out.println("新浪新闻采集失败；" + e.getMessage());
        }
    }

    /**
     * 解析新浪新闻日期字符串为 LocalDateTime。
     * 新浪页面日期格式为「MM-dd HH:mm」（如 02-18 00:57），
     * 这里补上当前年份；解析失败返回 null。
     */
    private LocalDateTime parseNewsDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            String[] parts = dateStr.trim().split("\\s+");
            if (parts.length < 2) {
                return null;
            }
            String[] md = parts[0].split("-");
            String[] hm = parts[1].split(":");
            if (md.length != 2 || hm.length < 2) {
                return null;
            }
            int month = Integer.parseInt(md[0].trim());
            int day = Integer.parseInt(md[1].trim());
            int hour = Integer.parseInt(hm[0].trim());
            int minute = Integer.parseInt(hm[1].trim());
            int year = LocalDate.now().getYear();
            return LocalDateTime.of(year, month, day, hour, minute);
        } catch (Exception e) {
            return null;
        }
    }


    @Async
    public void sinaProcess() {
        System.out.println("开始爬取新浪新闻，时间:" + new Date());
        for (String type : SinaHotNewsSpider.SINA_NEWS_TAG.keySet()) {
            System.out.println("爬取【"+ type +"】中...,时间:" +   new Date());
            this.sinaNewsSpider(type);
            System.out.println("【"+ type + "】爬取结束,时间:"+   new Date());
        }
        System.out.println("结束爬取新浪新闻，时间:" + new Date());
    }
}
