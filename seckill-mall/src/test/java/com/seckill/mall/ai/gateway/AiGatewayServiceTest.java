package com.seckill.mall.ai.gateway;

import com.seckill.mall.ai.gateway.service.AiGatewayService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * AI 网关服务测试：mock ChatClient 链式调用，验证 call/stream 行为。
 * <p>不真实调用大模型，仅验证 AiGatewayService 对 ChatClient 的编排正确。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：AiGatewayServiceTest.java
 * 邮箱：nj651217@163.com
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AI网关服务测试")
class AiGatewayServiceTest {

    @Mock
    private ChatClient chatClient;
    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;
    @Mock
    private ChatClient.StreamResponseSpec streamResponseSpec;

    private AiGatewayService aiGatewayService;

    @Test
    @DisplayName("call：正常调用应返回模型响应内容")
    void call_shouldReturnContent() {
        // given —— mock 链式调用：prompt().system().user().toolContext().call().content()
        given(chatClient.prompt()).willReturn(requestSpec);
        given(requestSpec.system(any(String.class))).willReturn(requestSpec);
        given(requestSpec.user(any(String.class))).willReturn(requestSpec);
        given(requestSpec.toolContext(any(Map.class))).willReturn(requestSpec);
        given(requestSpec.call()).willReturn(callResponseSpec);
        given(callResponseSpec.content()).willReturn("模拟响应");

        aiGatewayService = new AiGatewayService(chatClient);

        // when
        String result = aiGatewayService.call("你是一个测试助手", "你好", "test");

        // then
        assertThat(result).isEqualTo("模拟响应");
    }

    @Test
    @DisplayName("stream：正常调用应返回流式响应 Flux")
    void stream_shouldReturnFluxContent() {
        // given —— mock 链式调用：prompt().system().user().toolContext().stream().content()
        Flux<String> expectedFlux = Flux.just("你好", "，", "世界");
        given(chatClient.prompt()).willReturn(requestSpec);
        given(requestSpec.system(any(String.class))).willReturn(requestSpec);
        given(requestSpec.user(any(String.class))).willReturn(requestSpec);
        given(requestSpec.toolContext(any(Map.class))).willReturn(requestSpec);
        given(requestSpec.stream()).willReturn(streamResponseSpec);
        given(streamResponseSpec.content()).willReturn(expectedFlux);

        aiGatewayService = new AiGatewayService(chatClient);

        // when
        Flux<String> result = aiGatewayService.stream("你是一个测试助手", "你好", "test");

        // then —— 验证 Flux 元素顺序与内容（collectList().block() 同步收集）
        List<String> collected = result.collectList().block();
        assertThat(collected).containsExactly("你好", "，", "世界");
    }

    @Test
    @DisplayName("构造注入：ChatClient 注入后服务实例应可用")
    void constructor_shouldInjectChatClient() {
        // given & when
        aiGatewayService = new AiGatewayService(chatClient);

        // then —— 仅验证实例构造成功，不触发真实调用
        assertThat(aiGatewayService).isNotNull();
    }
}