package com.xin.graphdomainbackend.infrastructure.imagesearch.sub;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xin.graphdomainbackend.common.exception.BusinessException;
import com.xin.graphdomainbackend.common.exception.ErrorCode;
import com.xin.graphdomainbackend.infrastructure.imagesearch.model.ImageSearchResult;

import java.util.List;

import static com.xin.graphdomainbackend.infrastructure.imagesearch.sub.GetImageFirstUrlApi.getImageFirstUrl;


/**
 * 获取图片列表（step 3）
 */
public class GetImageListApi {

    public static List<ImageSearchResult> getImageList(String url) {
        // 发送get请求
        HttpResponse response = HttpUtil.createGet(url)
                .execute();
        // 获取相应内容
        int statusCode = response.getStatus();
        String body = response.body();
        if (statusCode == 200) { // 成功返回，提取图片的url
            JSONObject jsonObject = new JSONObject(body);
            if (!jsonObject.containsKey("data")) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未获取到图片列表");
            }
            JSONObject data = jsonObject.getJSONObject("data");
            if (!data.containsKey("list")) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未获取到图片列表");
            }
            JSONArray list = data.getJSONArray("list");
            List<ImageSearchResult> imageSearchResults = JSONUtil.toList(list, ImageSearchResult.class);

            return imageSearchResults;
        } else {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
        }
    }

    public static void main(String[] args) {
        // 请求目标 URL
        String url = GetImagePageUrl.getImagePageUrl("https://img.shetu66.com/2024/05/06/171498094959370632.png");
        String imageFirstUrl = getImageFirstUrl(url);
        List<ImageSearchResult> imageList = getImageList(imageFirstUrl);
        System.out.println("搜索成功，结果 URL：" + imageList);
    }
}
