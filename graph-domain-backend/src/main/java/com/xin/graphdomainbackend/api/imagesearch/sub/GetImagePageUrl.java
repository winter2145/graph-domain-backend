package com.xin.graphdomainbackend.api.imagesearch.sub;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.xin.graphdomainbackend.exception.BusinessException;
import com.xin.graphdomainbackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 获取以图搜图页面地址（step 1）
 */
@Slf4j
public class GetImagePageUrl {

    public static String getImagePageUrl(String imageUrl) {

        // 1.准备请求参数
        Map<String, Object> formData = new HashMap<>();
        try {
            imageUrl =  URLEncoder.encode(imageUrl, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            log.error("URL 编码失败", e);
        }
        formData.put("image", imageUrl);
        formData.put("tn", "pc");
        formData.put("from", "pc");
        formData.put("image_source", "PC_UPLOAD_URL");
        // 获取当前时间戳
        long uptime = System.currentTimeMillis();
        // 拼接 post 地址
        String urlString =  "https://graph.baidu.com/upload?uptime=" + uptime;
        String acsToken = "1748752153388_1748756234147_f4ofPFQStfIvh/nDs3Vr09MBc+Dg6ORX4PZFW7pphA88XakA7KeCE8kPJNCCS93gSIUJ89h+FOw9QJvuGqiLohPnHC1FXaqosfpNFwTWqLoVomX3pcAajomOsi8bNeos1m2510f7uTOfISAWCS1hgbd/BUaRVrSJbKnSzt5X7PEpcv95jIlpHK9NvcdRNQmB4Ny1OnFInrXF6DxAfk0bG5Dz8miOt9I3cbBislTsq8bgX81s0U3UacsjQsW6xJKlDeFArfy0SjSj/l31N0DbpLDa6fHEI2ijovU85SZK5Fm3cZwaxVfecQfQbBpyrjvabLcuB4rRHEsDAjR9tLpsdJQMrCKkt7jTeLmu+KoR23NBh/g1gdCzjCJfC0EzLgeeGhovsnYTihHaMhfhDxS1QJqwHsHHCARZ8vk/wS3LxN9NcrelO7bpy/jgeGnz8tEcMoxZAtNuijnF7NbkYc7hUQsbLS5245NLgnpOFoXXUJQ=";

        try {
            // 2. 发送请求
            HttpResponse httpResponse = HttpRequest.post(urlString)
                    .form(formData)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .header("Referer", "https://graph.baidu.com/pcpage/index?tpl_from=pc")
                    .header("Acs-Token", acsToken)
                    .timeout(5000)
                    .execute();
            if (httpResponse.getStatus() != HttpStatus.HTTP_OK) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
        // 3. 处理响应结果
        String body = httpResponse.body();
        Map<String, Object> result = JSONUtil.toBean(body, Map.class);
        if (result == null || !Integer.valueOf(0).equals(result.get("status"))) {
            log.error("Status: " + httpResponse.getStatus());
            log.error("Body: " + httpResponse.body());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
        }
        Map<String, Object> data = ((Map<String, Object>)result.get("data"));
        String rawUrl = (String) data.get("url");
        String searchResultUrl = URLUtil.decode(rawUrl, StandardCharsets.UTF_8);

        // 如果 URL 为空
        if (StrUtil.isBlank(searchResultUrl)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未返回有效的结果地址");
        }
        return searchResultUrl;

        } catch (Exception e) {
            log.error("调用百度以图搜图接口失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜索失败");
        }
    }

    public static void main(String[] args) {
        // 请求目标url
        String url = "https://img.shetu66.com/2024/05/06/171498094959370632.png";
        String imagePageUrl = getImagePageUrl(url);
        System.out.println("百度以图搜图接口 : " +  imagePageUrl);

    }
}
