package com.mcp;

import com.mcp.tools.CartMcpController;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class HybrisMcpServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(HybrisMcpServerApplication.class, args);
    }

    @Bean
    public List<ToolCallback> RegisterTools(CartMcpController cartMcpController) {
        return List.of(ToolCallbacks.from(cartMcpController));
    }
}
