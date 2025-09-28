package com.mcp.tools;


import com.mcp.entity.CartItem;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@RestController
@RequestMapping("/mcp")
public class CartMcpController {

    private static final Map<String, Double> PRODUCTS = Map.of(
            "iPhone", 79999.0,
            "MacBook Air", 129999.0,
            "Boat Airdopes", 1999.0
    );

    private final com.mcp.respository.CartItemRepository cartItemRepository;
    private final Map<String, Function<Map<String,Object>, Object>> tools = new ConcurrentHashMap<>();

    public CartMcpController(com.mcp.respository.CartItemRepository repository) {
        this.cartItemRepository = repository;

        // Register tools
        tools.put("addToCart", input -> {
            String productName = (String) input.get("productName");
            int quantity = (Integer) input.get("quantity");

            if (!PRODUCTS.containsKey(productName)) return "Product not found";

            CartItem cartItem = cartItemRepository.findByProductId(productName);
            if (cartItem == null) {
                cartItem = new CartItem();
                cartItem.setProductId(productName);
                cartItem.setProductName(productName);
                cartItem.setQuantity(quantity);
            } else {
                cartItem.setQuantity(cartItem.getQuantity() + quantity);
            }

            cartItem.setPrice(cartItem.getQuantity() * PRODUCTS.get(productName));
            cartItemRepository.save(cartItem);
            return quantity + " " + productName + " added. Total: " + cartItem.getPrice();
        });

        tools.put("removeCart", input -> {
            String productName = (String) input.get("productName");
            cartItemRepository.deleteByProductId(productName);
            return productName + " removed.";
        });

        tools.put("getCarts", input -> cartItemRepository.findAll());
        tools.put("getCartTotal", input -> cartItemRepository.findAll()
                .stream().mapToDouble(CartItem::getPrice).sum());
    }

    // SSE endpoint for OpenAI MCP handshake
    @GetMapping(path = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sse() {
        SseEmitter emitter = new SseEmitter();

        new Thread(() -> {
            try {
                // Build tool list
                List<Map<String, String>> toolList = List.of(
                        Map.of("name", "addToCart", "description", "Add a product to the shopping cart."),
                        Map.of("name", "removeCart", "description", "Remove a product from the shopping cart."),
                        Map.of("name", "getCarts", "description", "Retrieve all cart items."),
                        Map.of("name", "getCartTotal", "description", "Get total price of cart items.")
                );

                Map<String, Object> serverInfo = Map.of(
                        "event", "server_info",
                        "server_label", "hybris-cart-mcp",
                        "tools", toolList
                );

                // Send as proper SSE
                emitter.send(SseEmitter.event()
                        .name("server_info")
                        .data(serverInfo)
                        .id("1"));
                // Keep the connection open
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }


    // Tool invocation endpoint
    @PostMapping("/tools/{toolName}")
    public Object callTool(@PathVariable String toolName, @RequestBody Map<String,Object> input) {
        if (tools.containsKey(toolName)) {
            return tools.get(toolName).apply(input);
        }
        return Map.of("status","error","message","Tool not found");
    }
}
