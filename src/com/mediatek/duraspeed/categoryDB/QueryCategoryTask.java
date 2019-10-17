/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 *
 * MediaTek Inc. (C) 2018. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */
package com.mediatek.duraspeed.categoryDB;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.mediatek.duraspeed.presenter.APPCategory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.util.Locale;

public class QueryCategoryTask {
    private final String TAG = "QueryCategoryTask";
    private final String mUserAgent = "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; " +
            "en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2";

    private int mCommonExceptionCount = 0;
    private Context mContext;

    public QueryCategoryTask(Context context) {
        mContext = context;
    }

    // Get the country/region either from the SIM ID or from locale
    private String getSimOrDefaultLocaleCountry() {
        TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context
                .TELEPHONY_SERVICE);
        String country = telephonyManager.getSimCountryIso();
        if (!TextUtils.isEmpty(country)) {
            return country.toUpperCase();
        }
        return Locale.getDefault().getCountry();
    }

    public APPCategory queryFromWeb(String packageName) throws Exception {
        mCommonExceptionCount = 0;
        String country = getSimOrDefaultLocaleCountry();
        boolean isLocaleCN = "CN".equals(country);
        APPCategory result;
        if (isLocaleCN) {
            result = queryFromQQ(packageName);
        } else {
            result = queryFromGoogle(packageName);
        }

        if (result == APPCategory.UNKNOWN) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (isLocaleCN) {
                result = queryFromQQ(packageName);
            } else {
                result = queryFromGoogle(packageName);
            }
        }
        if (result == APPCategory.UNKNOWN && mCommonExceptionCount == 4) {
            throw new Exception("Common exception happens 4 times in one query");
        }
        return result;
    }

    private APPCategory queryFromGoogle(String packageName) {
        APPCategory result = APPCategory.UNKNOWN;
        try {
            Document doc = null;
            String url = "https://play.google.com/store/apps/details?id=" + packageName + "&hl=en";
            try {
                doc = Jsoup.connect(url)
                        .header("User-Agent", mUserAgent)
                        .timeout(20 * 1000)
                        .get();
            } catch (org.jsoup.HttpStatusException e) {
                Log.e(TAG, "HttpStatusException in queryFromGoogle", e);
                mCommonExceptionCount++;
            } catch (SocketTimeoutException e) {
                Log.e(TAG, "SocketTimeoutException in queryFromGoogle", e);
                mCommonExceptionCount++;
            } catch (IOException e) {
                Log.e(TAG, "IOException in queryFromGoogle", e);
            }
            if (doc == null) {
                return result;
            }
            Element categoryClass = doc.getElementsByClass("document-subtitles").first();
            String subCategory = categoryClass.select("span[itemprop]").last().text().trim();
            Element categoryParent = categoryClass.getElementsByTag("a").last();
            String topCategoey = categoryParent.attr("href");
            result = parseCategoryFromGoogle(topCategoey, subCategory);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Exception in queryFromGoogle", e);
            return result;
        }
    }

    private APPCategory parseCategoryFromGoogle(String topCategory, String subCategory) {
        APPCategory category = APPCategory.UNKNOWN;
        if (subCategory.equals("Action")
                || subCategory.equals("Adventure")
                || subCategory.equals("Arcade")
                || subCategory.equals("Board")
                || subCategory.equals("Card")
                || subCategory.equals("Casino")
                || subCategory.equals("Casual")
                || subCategory.equals("Educational")
                || subCategory.equals("Music")
                || subCategory.equals("Puzzle")
                || subCategory.equals("Racing")
                || subCategory.equals("Role Playing")
                || subCategory.equals("Simulation")
                || subCategory.equals("Strategy")
                || subCategory.equals("Trivia")
                || subCategory.equals("Word")) {
            category = APPCategory.GAME;
        } else if (subCategory.equals("Books & Reference")) {
            category = APPCategory.BOOKS_REFERENCE;
        } else if (subCategory.equals("Business")) {
            category = APPCategory.BUSINESS;
        } else if (subCategory.equals("Comics")) {
            category = APPCategory.COMICS;
        } else if (subCategory.equals("Communication")) {
            category = APPCategory.COMMUNICATION;
        } else if (subCategory.equals("Education")) {
            category = APPCategory.EDUCATION;
        } else if (subCategory.equals("Entertainment")) {
            category = APPCategory.ENTERTAINMENT;
        } else if (subCategory.equals("Finance")) {
            category = APPCategory.FINANCE;
        } else if (subCategory.equals("Health & Fitness")) {
            category = APPCategory.HEALTH_FITNESS;
        } else if (subCategory.equals("Libraries & Demo")) {
            category = APPCategory.LIBRARIES_DEMO;
        } else if (subCategory.equals("Lifestyle")) {
            category = APPCategory.LIFESTYLE;
        } else if (subCategory.equals("Media & Video")) {
            category = APPCategory.MEDIA_VIDEO;
        } else if (subCategory.equals("Medical")) {
            category = APPCategory.MEDICAL;
        } else if (subCategory.equals("Music & Audio")) {
            category = APPCategory.MUSIC_AUDIO;
        } else if (subCategory.equals("News & Magazines")) {
            category = APPCategory.NEWS_MAGAZINES;
        } else if (subCategory.equals("Personalization")) {
            category = APPCategory.PERSONALIZATION;
        } else if (subCategory.equals("Photography")) {
            category = APPCategory.PHOTOGRAPHY;
        } else if (subCategory.equals("Productivity")) {
            category = APPCategory.PRODUCTIVITY;
        } else if (subCategory.equals("Shopping")) {
            category = APPCategory.SHOPPING;
        } else if (subCategory.equals("Social")) {
            category = APPCategory.SOCIAL;
        } else if (subCategory.equals("Sports")) {
            // both APP and Game have sports category, need check topCategory
            if (topCategory.contains("GAME")) {
                category = APPCategory.GAME;
            } else {
                category = APPCategory.SPORTS;
            }
        } else if (subCategory.equals("Tools")) {
            category = APPCategory.TOOLS;
        } else if (subCategory.equals("Transportation")) {
            category = APPCategory.TRANSPORTATION;
        } else if (subCategory.equals("Travel & Local")) {
            category = APPCategory.TRAVEL_LOCAL;
        } else if (subCategory.equals("Weather")) {
            category = APPCategory.WEATHER;
        }

        return category;
    }

    private APPCategory queryFromQQ(String packageName) {
        APPCategory result = APPCategory.UNKNOWN;
        try {
            Document doc = null;
            String url = "http://sj.qq.com/myapp/detail.htm?apkName=" + packageName;
            try {
                doc = Jsoup.connect(url)
                        .header("User-Agent", mUserAgent)
                        .timeout(20 * 1000)
                        .get();
            } catch (org.jsoup.HttpStatusException e) {
                Log.e(TAG, "HttpStatusException queryFromQQ", e);
                mCommonExceptionCount++;
            } catch (SocketTimeoutException e) {
                Log.e(TAG, "SocketTimeoutException in queryFromQQ", e);
                mCommonExceptionCount++;
            } catch (IOException e) {
                Log.e(TAG, "IOException in queryFromQQ", e);
            }
            if (doc == null) {
                return result;
            }
            Elements categoryClass = doc.getElementsByClass("det-type-box");
            Elements category = categoryClass.select("a[href]");
            String subCategory = category.text().trim();
            result = parseCategoryFromQQ(subCategory);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Exception in queryFromQQ", e);
            return result;
        }
    }

    private APPCategory parseCategoryFromQQ(String subCategory) throws
            UnsupportedEncodingException {
        APPCategory category = APPCategory.UNKNOWN;
        if (subCategory.equals("休闲益智")
                || subCategory.equals("网络游戏")
                || subCategory.equals("动作冒险")
                || subCategory.equals("棋牌中心")
                || subCategory.equals("飞行射击")
                || subCategory.equals("经营策略")
                || subCategory.equals("角色扮演")
                || subCategory.equals("体育竞技")
                || subCategory.equals("游戏")) {
            category = APPCategory.GAME;
        } else if (subCategory.equals("视频")) {
            category = APPCategory.MEDIA_VIDEO;
        } else if (subCategory.equals("音乐")) {
            category = APPCategory.MUSIC_AUDIO;
        } else if (subCategory.equals("购物")) {
            category = APPCategory.SHOPPING;
        } else if (subCategory.equals("阅读")) {
            category = APPCategory.BOOKS_REFERENCE;
        } else if (subCategory.equals("导航")) {
            category = APPCategory.TRANSPORTATION;
        } else if (subCategory.equals("社交")) {
            category = APPCategory.SOCIAL;
        } else if (subCategory.equals("摄影")) {
            category = APPCategory.PHOTOGRAPHY;
        } else if (subCategory.equals("新闻")) {
            category = APPCategory.NEWS_MAGAZINES;
        } else if (subCategory.equals("工具")) {
            category = APPCategory.TOOLS;
        } else if (subCategory.equals("美化")) {
            category = APPCategory.PHOTOGRAPHY;
        } else if (subCategory.equals("教育")) {
            category = APPCategory.EDUCATION;
        } else if (subCategory.equals("生活")) {
            category = APPCategory.LIFESTYLE;
        } else if (subCategory.equals("安全")) {
            category = APPCategory.TOOLS;
        } else if (subCategory.equals("旅游")) {
            category = APPCategory.TRAVEL_LOCAL;
        } else if (subCategory.equals("儿童")) {
            category = APPCategory.EDUCATION;
        } else if (subCategory.equals("理财")) {
            category = APPCategory.FINANCE;
        } else if (subCategory.equals("系统")) {
            category = APPCategory.TOOLS;
        } else if (subCategory.equals("健康")) {
            category = APPCategory.HEALTH_FITNESS;
        } else if (subCategory.equals("娱乐")) {
            category = APPCategory.ENTERTAINMENT;
        } else if (subCategory.equals("办公")) {
            category = APPCategory.BUSINESS;
        } else if (subCategory.equals("通讯")) {
            category = APPCategory.COMMUNICATION;
        } else if (subCategory.equals("出行")) {
            category = APPCategory.TRANSPORTATION;
        }
        return category;
    }
}
