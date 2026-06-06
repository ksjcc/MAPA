package com.web.yunpicturebackend.api.imagesearch.sub;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.web.yunpicturebackend.exception.BusinessException;
import com.web.yunpicturebackend.exception.ErrorCode;

import lombok.extern.slf4j.Slf4j;

/**
 * 获取图片列表接口的 Api（Step 2）
 */
@Slf4j
public class GetImageFirstUrlApi {
  public static String getImageFirstUrl(String url) {
    try {
      Document document = Jsoup.connect(url)
          .timeout(5000)
          .get();
      Elements scriptElements = document.getElementsByTag("script");
      for (Element script : scriptElements) {
        String scriptContent = script.html();
        if (scriptContent.contains("\"firstUrl\"")) {
          Pattern pattern = Pattern.compile("\"firstUrl\":\"(.*?)\"");
          Matcher matcher = pattern.matcher(scriptContent);
          if (matcher.find()) {
            String firstUrl = matcher.group(1);
            firstUrl = firstUrl.replace("\\/", "/");
            return firstUrl;
          }
        }
      }
    } catch (Exception e) {
      log.error("获取图片列表失败", e);
      throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取图片列表失败");
    }
    return null;
  }

  public static void main(String[] args) {
    // 请求目标 URL
    String url = "https://graph.baidu.com/s?card_key=&entrance=GENERAL&extUiData[isLogoShow]=1&f=all&isLogoShow=1&session_id=16250747570487381669&sign=1265ce97cd54acd88139901733452612&tpl_from=pc";
    String imageFirstUrl = getImageFirstUrl(url);
    System.out.println("搜索成功，结果 URL：" + imageFirstUrl);
  }
}
