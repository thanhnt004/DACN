package com.example.backend.service.cart;

import com.example.backend.dto.request.cart.CartItemRequest;
import com.example.backend.dto.request.cart.UpdateCartItemVariantRequest;
import com.example.backend.dto.response.cart.CartItemResponse;
import com.example.backend.dto.response.cart.CartResponse;
import com.example.backend.dto.response.catalog.VariantStockStatus;
import com.example.backend.dto.response.checkout.CheckoutItemDetail;
import com.example.backend.dto.response.checkout.CheckoutSession;
import com.example.backend.exception.BadRequestException;
import com.example.backend.exception.cart.CartItemNotFoundException;
import com.example.backend.exception.cart.CartNotFoundException;
import com.example.backend.exception.cart.InsufficientStockException;
import com.example.backend.exception.product.VariantNotFoundException;
import com.example.backend.mapper.cart.CartMapper;
import com.example.backend.model.User;
import com.example.backend.model.cart.Cart;
import com.example.backend.model.cart.CartItem;
import com.example.backend.model.product.Inventory;
import com.example.backend.model.product.ProductVariant;
import com.example.backend.repository.cart.CartItemRepository;
import com.example.backend.repository.cart.CartRepository;
import com.example.backend.repository.catalog.product.ProductVariantRepository;
import com.example.backend.repository.user.UserRepository;
import com.example.backend.util.AuthenUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository variantRepository;
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;
    private final AuthenUtil authenUtil;

    public CartResponse getOrCreateCart(Optional<UUID> guestCartId) {
        Optional<User> user = authenUtil.getAuthenUser();
        Cart cart = findOrCreateActiveCart(user, guestCartId);
        return buildCartResponse(cart);
    }

    private CartResponse buildCartResponse(Cart cart) {
        // Remove items with deleted variants before mapping
        cart.getItems().removeIf(item -> {
            try {
                ProductVariant variant = item.getVariant();
                return variant == null || variant.getDeletedAt() != null;
            } catch (jakarta.persistence.EntityNotFoundException e) {
                // Variant was deleted, remove this item
                cartItemRepository.delete(item);
                return true;
            }
        });
        
        // Sort items by createdAt to maintain consistent order (null-safe)
        cart.getItems().sort((a, b) -> {
            Instant aTime = a.getCreatedAt();
            Instant bTime = b.getCreatedAt();
            if (aTime == null && bTime == null) return 0;
            if (aTime == null) return 1;
            if (bTime == null) return -1;
            return aTime.compareTo(bTime);
        });
        
        CartResponse cartResponse = cartMapper.toDto(cart);

        // Handle null or empty items list
        if (cartResponse.getItems() == null || cartResponse.getItems().isEmpty()) {
            return cartResponse;
        }

        for (CartItemResponse itemResponse : cartResponse.getItems()) {
            CartItem cartItemEntity = cart.getItems().stream()
                    .filter(item -> item.getId().equals(itemResponse.getId()))
                    .findFirst()
                    .orElse(null);

            if (cartItemEntity != null && cartItemEntity.getVariant() != null) {
                ProductVariant variant = cartItemEntity.getVariant();
                Inventory inventory = variant.getInventory();
                int availableStock = inventory != null ? inventory.getAvailableStock() : 0;
                boolean inStock = availableStock > 0;

                VariantStockStatus stockStatus = VariantStockStatus.builder()
                        .inStock(inStock)
                        .availableQuantity(availableStock)
                        .message(inStock ? "In Stock" : "Out of Stock")
                        .build();

                itemResponse.setStockStatus(stockStatus);
            } else {
                itemResponse.setStockStatus(VariantStockStatus.builder()
                        .inStock(false)
                        .availableQuantity(0)
                        .message("Unknown")
                        .build());
            }
        }
        return cartResponse;
    }

    public CartResponse addItemToCart(@Valid CartItemRequest cartItemRequest, Optional<UUID> guestCartId) {
        Optional<User> user = authenUtil.getAuthenUser();
        Cart cart = findOrCreateActiveCart(user, guestCartId);
        ProductVariant variant = variantRepository.findById(cartItemRequest.getVariantId())
                .orElseThrow(() -> new VariantNotFoundException("Không tìm thấy biến thể sản phẩm"));
        cart = upsertItem(cart, variant, cartItemRequest.getQuantity());
        return buildCartResponse(cartRepository.save(cart));
    }

    public Cart findOrCreateActiveCart(Optional<User> user, Optional<UUID> guestCartId) {
        if (user.isPresent()) {
            return cartRepository.findByUserIdAndStatus(user.get().getId(), Cart.CartStatus.ACTIVE)
                    .orElseGet(() -> createNewCart(user.get()));
        } else if (guestCartId.isPresent()) {
            return cartRepository.findByIdAndUserIdIsNullAndStatus(guestCartId.get(), Cart.CartStatus.ACTIVE)
                    .orElseGet(() -> createNewCart(null));
        } else {
            return createNewCart(null);
        }
    }

    @Transactional
    public CartResponse mergeCarts(UUID guestCartId) {
        Optional<User> userOpt = authenUtil.getAuthenUser();
        if (userOpt.isEmpty()) {
            throw new BadRequestException("User must be logged in to merge carts");
        }
        User user = userOpt.get();
        Cart userCart = cartRepository.findByUserIdAndStatus(user.getId(), Cart.CartStatus.ACTIVE)
                .orElseGet(() -> createNewCart(user));

        Optional<Cart> guestCartOpt = cartRepository.findByIdAndUserIdIsNullAndStatus(guestCartId,
                Cart.CartStatus.ACTIVE);

        if (guestCartOpt.isEmpty()) {
            return buildCartResponse(userCart);
        }

        Cart guestCart = guestCartOpt.get();
        List<CartItem> guestItems = new ArrayList<>(guestCart.getItems());

        for (CartItem guestItem : guestItems) {
            Optional<CartItem> userItemOpt = cartItemRepository.findByCartAndVariant(userCart, guestItem.getVariant());
            int currentQuantityInCart = userItemOpt.map(CartItem::getQuantity).orElse(0);
            int newTotalQuantityInCart = currentQuantityInCart + guestItem.getQuantity();

            checkInventoryAvailability(guestItem.getVariant(), newTotalQuantityInCart);

            if (userItemOpt.isPresent()) {
                CartItem userItem = userItemOpt.get();
                userItem.setQuantity(newTotalQuantityInCart);
                cartItemRepository.save(userItem);
                guestCart.removeItem(guestItem);
                cartItemRepository.delete(guestItem);
            } else {
                guestCart.removeItem(guestItem);
                userCart.addItem(guestItem);
            }
        }

        cartRepository.delete(guestCart);
        return buildCartResponse(cartRepository.save(userCart));
    }

    @Transactional
    public CartResponse removeItemFromCart(UUID cartItemId, Optional<UUID> guestCartId) {
        Optional<User> user = authenUtil.getAuthenUser();
        Cart cart = findActiveCartOrThrow(user, guestCartId);

        CartItem itemToRemove = cartItemRepository.findByIdAndCart(cartItemId, cart)
                .orElseThrow(() -> new CartItemNotFoundException("Không tìm thấy sản phẩm trong giỏ hàng"));

        cart.removeItem(itemToRemove);
        Cart savedCart = cartRepository.save(cart);
        return buildCartResponse(savedCart);
    }

    @Transactional
    public CartResponse updateCartItemVariant(UUID cartItemId, UpdateCartItemVariantRequest request,
            Optional<UUID> guestCartId) {
        Optional<User> user = authenUtil.getAuthenUser();
        Cart cart = findActiveCartOrThrow(user, guestCartId);

        CartItem oldItem = cartItemRepository.findByIdAndCart(cartItemId, cart)
                .orElseThrow(() -> new CartItemNotFoundException("Không tìm thấy sản phẩm trong giỏ hàng"));

        ProductVariant newVariant = variantRepository.findById(request.getNewVariantId())
                .orElseThrow(() -> new VariantNotFoundException("Không tìm thấy phiên bản sản phẩm"));

        cart.removeItem(oldItem);
        cartItemRepository.delete(oldItem);
        cartItemRepository.flush();

        Cart updatedCart = upsertItem(cart, newVariant, request.getNewQuantity());
        return buildCartResponse(updatedCart);
    }

    public CartResponse removeAllItem(Optional<UUID> cartIdOptional) {
        Optional<User> user = authenUtil.getAuthenUser();
        Cart cart = findActiveCartOrThrow(user, cartIdOptional);
        cart.getItems().clear();
        Cart savedCart = cartRepository.save(cart);
        return buildCartResponse(savedCart);
    }

    private Cart createNewCart(User user) {
        Cart cart = Cart.builder()
                .user(user)
                .status(Cart.CartStatus.ACTIVE)
                .build();
        return cartRepository.save(cart);
    }

    private void checkInventoryAvailability(ProductVariant variant, int desiredQuantity) {
        Inventory inventory = variant.getInventory();
        if (inventory == null) {
            throw new VariantNotFoundException(
                    "Không tìm thấy thông tin tồn kho cho sản phẩm " + variant.getSku());
        }
        int availableStock = inventory.getAvailableStock();
        if (desiredQuantity > availableStock) {
            throw new InsufficientStockException(
                    "Không đủ hàng cho sản phẩm " + variant.getSku() +
                            ". Chỉ còn " + availableStock + " sản phẩm có sẵn.");
        }
    }

    private Cart findActiveCartOrThrow(Optional<User> user, Optional<UUID> guestCartId) {
        Optional<Cart> cartOpt;
        if (user.isPresent()) {
            cartOpt = cartRepository.findByUserIdAndStatus(user.get().getId(), Cart.CartStatus.ACTIVE);
        } else if (guestCartId.isPresent()) {
            cartOpt = cartRepository.findByIdAndUserIdIsNullAndStatus(guestCartId.get(), Cart.CartStatus.ACTIVE);
        } else {
            throw new CartNotFoundException("Không tìm thấy giỏ hàng");
        }

        return cartOpt.orElseThrow(() -> new CartNotFoundException("Không tìm thấy giỏ hàng đang hoạt động"));
    }

    private Cart upsertItem(Cart cart, ProductVariant variant, int quantityToAdd) {
        Inventory inventory = variant.getInventory();
        if (inventory == null) {
            throw new VariantNotFoundException("Không tìm thấy thông tin tồn kho cho variant ID: " + variant.getId());
        }
        int availableStock = inventory.getAvailableStock();

        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartAndVariant(cart, variant);
        int currentQuantityInCart = existingItemOpt.map(CartItem::getQuantity).orElse(0);
        int newTotalQuantityInCart = currentQuantityInCart + quantityToAdd;

        if (newTotalQuantityInCart > availableStock) {
            throw new InsufficientStockException(availableStock);
        }

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            existingItem.setQuantity(newTotalQuantityInCart);
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setVariant(variant);
            newItem.setQuantity(newTotalQuantityInCart);
            newItem.setUnitPriceAmount(variant.getPriceAmount());
            cart.addItem(newItem);
        }

        return cartRepository.save(cart);
    }

    @Transactional
    public void clearCart(CheckoutSession checkoutSession) {
        List<CheckoutItemDetail> items = checkoutSession.getItems();
        if (items == null || items.isEmpty()) {
            return;
        }

        // For BuyNow flow, cartId is null - skip cart clearing
        if (checkoutSession.getCartId() == null) {
            return;
        }

        Cart cart = cartRepository.findById(checkoutSession.getCartId())
                .orElseThrow(() -> new CartNotFoundException("Không tìm thấy giỏ hàng"));

        Set<UUID> idsToRemove = items.stream()
                .map(CheckoutItemDetail::getCartItemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        cart.getItems().removeIf(item -> idsToRemove.contains(item.getId()));
        cartRepository.save(cart);
    }
}

