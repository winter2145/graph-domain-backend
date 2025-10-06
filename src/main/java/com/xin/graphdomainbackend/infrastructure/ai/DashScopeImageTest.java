package com.xin.graphdomainbackend.infrastructure.ai;

import com.alibaba.cloud.ai.dashscope.image.DashScopeImageOptions;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.boot.CommandLineRunner;

// @Component  测试绘画模型使用
@Deprecated
@Slf4j
public class DashScopeImageTest implements CommandLineRunner {

    @Resource
    private ImageModel imageModel;   // 注意是 ImageModel

    @Override
    public void run(String... args) throws Exception {
        String prompt = "一只可爱的机器猫，赛博朋克风格，4K";
        
        System.out.println("开始调用图像生成服务，提示词: " + prompt);

        try {
            // 显式指定模型名称，确保使用正确的模型
            ImagePrompt imagePrompt = new ImagePrompt(prompt, DashScopeImageOptions.builder()
                            .withModel("qwen-image-plus")
                            .withN(1)
                            .build());
            
            System.out.println("发送的请求参数: " + imagePrompt);
            
            ImageResponse resp = imageModel.call(imagePrompt);
            
            // 添加详细的空值检查和错误处理
            if (resp == null) {
                System.err.println("图像生成失败: 响应对象为空");
                return;
            }
            if (resp.getResults() == null || resp.getResults().isEmpty()) {
                System.err.println("图像生成失败: 响应结果为空或无生成结果");
                return;
            }

            String url = resp.getResult().getOutput().getUrl();
            System.out.println("图片地址：" + url);

        } catch (Exception e) {
            log.error("图像生成失败: ", e);
        }
    }
}