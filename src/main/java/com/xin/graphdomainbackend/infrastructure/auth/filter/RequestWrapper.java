package com.xin.graphdomainbackend.infrastructure.auth.filter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 包装请求，使 InputStream 可以重复读取
 *
 * @author pine
 */
@Slf4j
public class RequestWrapper extends HttpServletRequestWrapper {

    private final byte[] cachedBody;  // 改用字节数组存储

    public RequestWrapper(HttpServletRequest request) {
        super(request);
        try {
            // 使用 Spring 工具类高效读取字节流
            this.cachedBody = StreamUtils.copyToByteArray(request.getInputStream());
        } catch (IOException e) {
            // 记录错误日志并抛出运行时异常
            log.error("读取请求体失败: {}", e.getMessage());
            throw new RuntimeException("无法读取请求体", e);
        }
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cachedBody);
        return new CachedBodyServletInputStream(byteArrayInputStream);
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(
                new InputStreamReader(
                        new ByteArrayInputStream(cachedBody),
                        StandardCharsets.UTF_8
                )
        );
    }

    /**
     * 实现可重复读取的ServletInputStream
     */
    private static class CachedBodyServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream inputStream;

        public CachedBodyServletInputStream(ByteArrayInputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public int read() {
            return inputStream.read();
        }

        @Override
        public boolean isFinished() {
            // 正确实现：当没有更多数据可读时返回true
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            // 数据已完全缓存在内存中，始终就绪
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            // 同步读取场景不需要实现
            throw new UnsupportedOperationException("不支持异步读取");
        }
    }

    /**
     * 获取请求体字符串（UTF-8编码）
     */
    public String getBody() {
        return new String(cachedBody, StandardCharsets.UTF_8);
    }

    /**
     * 获取原始字节数据
     */
    public byte[] getBodyAsBytes() {
        return cachedBody;
    }
}