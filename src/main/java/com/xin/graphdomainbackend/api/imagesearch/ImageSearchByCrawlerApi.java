package com.xin.graphdomainbackend.api.imagesearch;

import com.xin.graphdomainbackend.api.imagesearch.model.ImageSearchResult;
import com.xin.graphdomainbackend.api.imagesearch.sub.GetImageFirstUrlApi;
import com.xin.graphdomainbackend.api.imagesearch.sub.GetImageListApi;
import com.xin.graphdomainbackend.api.imagesearch.sub.GetImagePageUrl;

import java.util.List;

/**
 * 通过百度识图，上传url，返回相似图片
 */
public class ImageSearchByCrawlerApi {

    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = GetImagePageUrl.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        List<ImageSearchResult> imageSearchResults = GetImageListApi.getImageList(imageFirstUrl);

        return imageSearchResults;
    }
}
