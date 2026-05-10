package com._202510007517.major_assignment.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import net.logstash.logback.composite.loggingevent.MessageJsonProvider;

import java.io.IOException;

/**
 * JSON 输出路径下的敏感字段打码 Provider（R4.2 / Property 6）。
 *
 * <p>继承自 {@link MessageJsonProvider}，仅覆写 {@code writeTo} 流程：将
 * {@link ILoggingEvent#getFormattedMessage()} 先经 {@link MaskingConverter#mask(String)} 处理，
 * 再以同样字段名写入 JSON，从而保证控制台（{@code %mask} pattern）与 JSON 文件两条输出路径
 * 遵循同一掩码规则。</p>
 *
 * <p>注册方式（{@code logback-spring.xml}）：</p>
 * <pre>{@code
 *   <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
 *     <providers>
 *       ...
 *       <provider class="com._202510007517.major_assignment.logging.MessageMaskingJsonProvider"/>
 *       ...
 *     </providers>
 *   </encoder>
 * }</pre>
 */
public class MessageMaskingJsonProvider extends MessageJsonProvider {

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        String original = event.getFormattedMessage();
        String masked = MaskingConverter.mask(original);
        generator.writeStringField(getFieldName(), masked);
    }
}
