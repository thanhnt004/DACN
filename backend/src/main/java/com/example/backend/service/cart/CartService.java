package com.example.backend.service.cart;

import com.example.backend.dto.request.cart.CartItemRequest;
import com.example.backend.dto.request.cart.UpdateCartItemVariantRequest;
import com.example.backend.dto.response.cart.CartItemResponse;
import com.example.backend.dto.response.cart.CartResponse;
import com.example.backend.dto.response.catalog.VariantStockStatus;
import com.example.backend.dto.response.checkout.CheckoutItemDetail;
import com.example.backend.dto.response.checkout.CheckoutSession;
import com.example.backend.excepton.BadRequestException;
import com.example.backend.excepton.ConflictException;
import com.example.backend.excepton.NotFoundException;
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
        CartResponse cartResponse = cartMapper.toDto(cart);

        // Map variant info from the entity directly to avoid N+1 queries
        Map<UUID, CartItem> cartItemMap = new HashMap<>();
        if (cart.getItems() != null) {
            for (CartItem item : cart.getItems()) {
                if (item.getVariant() != null) {
                    cartItemMap.put(item.getVariant().getId(), item);
                }
            }
        }

        for (CartItemResponse itemResponse : cartResponse.getItems()) {
            // Find the corresponding entity item to get the variant
            // We use the variant ID from the response to find the original entity item (or
            // rather its variant)
            // But wait, itemResponse has variantId.

            // Optimization: Use the variant from the loaded Cart entity instead of querying
            // DB
            // We need to find the CartItem entity that corresponds to this itemResponse.
            // Assuming itemResponse.getId() matches CartItem.getId()

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
                // Fallback if something is inconsistent, though shouldn't happen
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
        // Logic to add item to cart goes here
        // Lấy variant
        ProductVariant variant = variantRepository.getReferenceById(cartItemRequest.getVariantId());
        // Upsert item
        cart = upsertItem(cart, variant, cartItemRequest.getQuantity());

        return buildCartResponse(cartRepository.save(cart));
    }

    public Cart findOrCreateActiveCart(Optional<User> user, Optional<UUID> guestCartId) {
        if (user.isPresent()) {
            return cartRepository.findByUserIdAndStatus(user.get().getId(), Cart.CartStatus.ACTIVE)
                    .orElseGet(() -> createNewCart(user.get()));
        } else if (guestCartId.isPresent()) {
            return cartRepository.findByIdAndUserIdIsNullAndStatus(guestCartId.get(), Cart.CartStatus.ACTIVE)
                    .orElseGet(() -> createNewCart(null)); // ID sai -> Tạo giỏ mới
        } else {
            return createNewCart(null);
        }
    }

    @Transactional
    public CartResponse mergeCarts(UUID guestCartId) {

        // 1. Lấy (hoặc tạo) giỏ hàng của USER
        Optional<User> userOpt = authenUtil.getAuthenUser();
        if (userOpt.isEmpty()) {
            throw new BadRequestException("User must be logged in to merge carts");
        }
        User user = userOpt.get();
        Cart userCart = cartRepository.findByUserIdAndStatus(user.getId(), Cart.CartStatus.ACTIVE)
                .orElseGet(() -> createNewCart(user)); // Tạo cart mới nếu user chưa có

        // 2. Lấy giỏ hàng của GUEST
        Optional<Cart> guestCartOpt = cartRepository.findByIdAndUserIdIsNullAndStatus(guestCartId,
                Cart.CartStatus.ACTIVE);

        if (guestCartOpt.isEmpty()) {
            return buildCartResponse(userCart);
        }

        Cart guestCart = guestCartOpt.get();
        // Create a copy of the list to avoid ConcurrentModificationException when
        // modifying the list during iteration
        List<CartItem> guestItems = new ArrayList<>(guestCart.getItems());

        // 3. Chuyển các item từ giỏ guest sang giỏ user
        for (CartItem guestItem : guestItems) {
            Optional<CartItem> userItemOpt = cartItemRepository.findByCartAndVariant(userCart, guestItem.getVariant());
            int currentQuantityInCart = userItemOpt.map(CartItem::getQuantity).orElse(0);
            int newTotalQuantityInCart = currentQuantityInCart + guestItem.getQuantity();

            // Check inventory for the TOTAL quantity
            checkInventoryAvailability(guestItem.getVariant(), newTotalQuantityInCart);

            if (userItemOpt.isPresent()) {
                CartItem userItem = userItemOpt.get();
                userItem.setQuantity(newTotalQuantityInCart);
                cartItemRepository.save(userItem);

                // Remove from guest cart since we merged it into existing item
                guestCart.removeItem(guestItem);
                // We can delete the guest item explicitly as it's no longer needed
                cartItemRepository.delete(guestItem);
            } else {
                // Move item to user cart
                guestCart.removeItem(guestItem);
                userCart.addItem(guestItem); // Re-parenting
                // No need to save guestItem explicitly, saving userCart will cascade
            }
        }

        // 4. Lưu giỏ hàng user (đã gộp) và Xóa giỏ hàng guest
        // If guestCart is empty now, we can delete it.
        // Even if not empty (should be empty), we delete it.
        cartRepository.delete(guestCart);
        return buildCartResponse(cartRepository.save(userCart)); // Lưu cart user
    }

    @Transactional
    public CartResponse removeItemFromCart(UUID cartItemId, Optional<UUID> guestCartId) {
        Optional<User> user = authenUtil.getAuthenUser();
        // 1. Tìm giỏ hàng (nếu không thấy sẽ báo lỗi)
        Cart cart = findActiveCartOrThrow(user, guestCartId);

        // 2. Tìm item trong giỏ hàng đó
        CartItem itemToRemove = cartItemRepository.findByIdAndCart(cartItemId, cart)
                .orElseThrow(() -> new NotFoundException("CartItem not found in this cart"));
        // 3. Xóa item khỏi giỏ hàng
        cart.removeItem(itemToRemove);

        // 4. Lưu lại giỏ hàng và trả về DTO
        Cart savedCart = cartRepository.save(cart);
        return buildCartResponse(savedCart);
    }

    @Transactional
    public CartResponse updateCartItemVariant(UUID cartItemId, UpdateCartItemVariantRequest request,
            Optional<UUID> guestCartId) {
        Optional<User> user = authenUtil.getAuthenUser();
        // 1. Tìm giỏ hàng
        Cart cart = findActiveCartOrThrow(user, guestCartId);

        // 2. Tìm item CŨ mà người dùng muốn đổi
        CartItem oldItem = cartItemRepository.findByIdAndCart(cartItemId, cart)
                .orElseThrow(() -> new RuntimeException("CartItem not found in this cart"));

        // 3. Tìm thông tin variant MỚI
        ProductVariant newVariant = variantRepository.findById(request.getNewVariantId())
                .orElseThrow(() -> new RuntimeException("New Variant not found"));

        // *** Logic "Đổi Variant" phức tạp ***
        // Nếu variant mới (size L) giống variant cũ (size M) -> đây chỉ là update
        // quantity.
        // Nếu variant mới (size L) khác variant cũ (size M):
        // a. Ta phải xóa item cũ (size M).
        // b. Ta phải "UPSERT" (thêm/gộp) item mới (size L).

        // 4. Xóa item CŨ khỏi giỏ hàng
        // (Hibernate sẽ xóa nó khi transaction kết thúc)
        cart.removeItem(oldItem);
        // Quan trọng: Phải flush để xóa ngay, tránh lỗi unique constraint
        cartItemRepository.delete(oldItem);
        cartItemRepository.flush();

        // 5. [TÁI SỬ DỤNG LOGIC]
        // Gọi lại hàm 'addItemToCart' nội bộ (hoặc tái cấu trúc logic check)
        // để "Upsert" variant MỚI vào.
        // Đây là cách an toàn nhất để đảm bảo:
        // - Kiểm tra tồn kho cho variant MỚI.
        // - Gộp số lượng nếu variant MỚI đã có sẵn trong giỏ.

        // Tái cấu trúc nhẹ logic từ 'addItemToCart'
        Cart updatedCart = upsertItem(cart, newVariant, request.getNewQuantity());

        return buildCartResponse(updatedCart);
    }

    public CartResponse removeAllItem(Optional<UUID> cartIdOptional) {
        Optional<User> user = authenUtil.getAuthenUser();
        // 1. Tìm giỏ hàng (nếu không thấy sẽ báo lỗi)
        Cart cart = findActiveCartOrThrow(user, cartIdOptional);

        // 2. Xóa tất cả item khỏi giỏ hàng
        cart.getItems().clear();

        // 3. Lưu lại giỏ hàng và trả về DTO
        Cart savedCart = cartRepository.save(cart);
        return buildCartResponse(savedCart);
    }

    /**
     * Create a new cart for the given user.
     * If user is null, the cart is created for a guest.
     */
    private Cart createNewCart(User user) {
        Cart cart = Cart.builder()
                .user(user)
                .status(Cart.CartStatus.ACTIVE)
                .build();
        return cartRepository.save(cart);
    }

    // Kiểm tra tồn kho
    private void checkInventoryAvailability(ProductVariant variant, int desiredQuantity) {
        Inventory inventory = variant.getInventory();
        if (inventory == null) {
            // If inventory record is missing, assume 0 stock or throw error.
            // Assuming 0 stock is safer.
            throw new BadRequestException(
                    "Không tìm thấy thông tin tồn kho cho sản phẩm " + variant.getSku());
        }
        int availableStock = inventory.getAvailableStock();
        if (desiredQuantity > availableStock) {
            throw new BadRequestException(
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
            throw new NotFoundException("Cart not found"); // Không thể xóa/update nếu ko có cart
        }

        return cartOpt.orElseThrow(() -> new RuntimeException("Active cart not found"));
    }

    private Cart upsertItem(Cart cart, ProductVariant variant, int quantityToAdd) {
        // 1. Lấy thông tin tồn kho
        Inventory inventory = variant.getInventory();
        if (inventory == null) {
            throw new NotFoundException("Inventory not found for variant ID: " + variant.getId());
        }
        int availableStock = inventory.getAvailableStock();

        // 2. Tìm item_đã_có và tính toán số lượng
        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartAndVariant(cart, variant);

        int currentQuantityInCart = existingItemOpt.map(CartItem::getQuantity).orElse(0);
        int newTotalQuantityInCart = currentQuantityInCart + quantityToAdd;

        // 3. Kiểm tra tồn kho
        if (newTotalQuantityInCart > availableStock) {
            throw new ConflictException(
                    "Không đủ hàng. Chỉ còn " + availableStock + " sản phẩm có sẵn.");
        }

        // 4. Logic Upsert
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
            cart.addItem(newItem); // Thêm vào list
        }

        return cartRepository.save(cart); // Lưu toàn bộ
    }

    @Transactional
    public void clearCart(CheckoutSession checkoutSession) {
        List<CheckoutItemDetail> items = checkoutSession.getItems();
        if (items == null || items.isEmpty()) {
            return; // Không có gì để xóa
        } else {
            Cart cart = cartRepository.findById(checkoutSession.getCartId())
                    .orElseThrow(() -> new NotFoundException("Cart not found"));

            Set<UUID> idsToRemove = items.stream()
                    .map(CheckoutItemDetail::getCartItemId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            cart.getItems().removeIf(item -> idsToRemove.contains(item.getId()));
            cartRepository.save(cart);
        }
    }
}
