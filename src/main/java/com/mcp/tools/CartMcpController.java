package com.mcp.tools;


import com.mcp.entity.CartItem;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mcp")
public class CartMcpController {

    private com.mcp.respository.CartItemRepository cartItemRepository;

    public CartMcpController(com.mcp.respository.CartItemRepository cartItemRepository) {
        this.cartItemRepository = cartItemRepository;
    }

    //tools
    // catalog service
    private static final Map<String, Double> PRODUCTS = Map.of(
            "iPhone", 79999.0,
            "MacBook Air", 129999.0,
            "Boat Airdopes", 1999.0
    );

    @Tool(name = "addToCart",
            description = "Add a product to the shopping cart. If the product already exists, it updates the quantity.")
    public String addToCart(@ToolParam String productName, @ToolParam int quantity){

        if(!PRODUCTS.containsKey(productName)){
            return "product not found";
        }

        Double price = PRODUCTS.get(productName);

        CartItem cartItem=cartItemRepository.findByProductId(productName);

        if(cartItem==null){
            cartItem = new CartItem();
            cartItem.setProductId(productName);
            cartItem.setProductName(productName);
            cartItem.setQuantity(quantity);
        }else{
            cartItem.setQuantity(cartItem.getQuantity()+quantity);
        }
        cartItem.setPrice(cartItem.getQuantity()*price);
        cartItemRepository.save(cartItem);
        return quantity + " " + productName + " added to cart. Total price: " + (cartItem.getPrice());
    }

    @Tool(
            name = "removeCart",
            description = "Remove a product from the shopping cart."
    )
    public String removeCart(@ToolParam String productName){
        cartItemRepository.deleteByProductId(productName);
        return productName + " removed from cart.";
    }

    @Tool(
            name = "getCarts",
            description = "Retrieve the current shopping cart items."
    )
    public List<CartItem> getCarts(){
        return cartItemRepository.findAll();
    }

    @Tool(
            name = "getCartTotal",
            description = "Calculate the total price of items in the shopping cart."
    )
    public double getCartTotal() {
        return cartItemRepository.findAll().stream()
                .mapToDouble(CartItem::getPrice)
                .sum();
    }

}
