package com.example.backend.service.shipping;

import com.example.backend.dto.ghn.CreateOrderData;
import com.example.backend.dto.ghn.CreateShippingOrder;
import com.example.backend.dto.response.checkout.CheckoutItemDetail;
import com.example.backend.dto.response.checkout.CheckoutSession;
import com.example.backend.dto.response.shipping.ShipmentResponse;
import com.example.backend.dto.response.shipping.ShippingOption;
import com.example.backend.dto.response.user.UserAddress;
import com.example.backend.exception.BadRequestException;
import com.example.backend.exception.shipping.ShippingNotFoundException;
import com.example.backend.model.order.Order;
import com.example.backend.model.order.Shipment;
import com.example.backend.model.order.ShipmentItem;
import com.example.backend.repository.shipping.ShipmentRepository;
import com.example.backend.service.user.UserManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ShippingService {
    private final GHNService ghnService;
    private final UserManagerService userManagerService;
    private final ShipmentRepository shipmentRepository;
    public List<ShippingOption> getAvailableShippingOptions(Optional<UserAddress> userAddressOps, List<CheckoutItemDetail> items) {
        return ghnService.getShippingOptions(items, userAddressOps);
    }
    @Transactional
    public Shipment createShipmentForOrder(Order order, CheckoutSession checkoutSession) {
        List<Shipment> shipments = shipmentRepository.findAllByOrderId(order.getId());
        if (!shipments.isEmpty()) {
            throw new BadRequestException("Đơn hàng đã có đơn vận chuyển.");
        }
        List<ShipmentItem> shipmentItems = createShipmentItemsFromOrder(order);
        
        // Get service level from selected shipping method
        String serviceLevel = null;
        if (checkoutSession.getSelectedShippingMethod() != null) {
            serviceLevel = checkoutSession.getSelectedShippingMethod().getServiceLevel();
        }
        
        Shipment shipment = Shipment.builder()
                .order(order)
                .carrier("GHN")
                .serviceLevel(serviceLevel)
                .status("CREATED")
                .build();
        shipment.addAllItems(shipmentItems);
        shipmentRepository.save(shipment);
        return shipment;
    }

    @Transactional
    public Shipment createShipmentForOrderManual(Order order) {
        List<Shipment> shipments = shipmentRepository.findAllByOrderId(order.getId());
        if (!shipments.isEmpty()) {
            throw new BadRequestException("Đơn hàng đã có đơn vận chuyển.");
        }
        List<ShipmentItem> shipmentItems = createShipmentItemsFromOrder(order);
        
        // Dùng service level mặc định cho đơn hàng manual
        Shipment shipment = Shipment.builder()
                .order(order)
                .carrier("GHN")
                .serviceLevel("2") // Service tiêu chuẩn
                .status("CREATED")
                .build();
        shipment.addAllItems(shipmentItems);
        shipmentRepository.save(shipment);
        return shipment;
    }

    @Transactional
    public void createShippingOrderInGHN(Shipment shipment, UserAddress userAddress) {
        CreateShippingOrder createShippingOrder = ghnService.buildCreateOrderRequest(shipment, userAddress,false);
        CreateOrderData createOrderData = ghnService.createShippingOrder(createShippingOrder);
        // Cập nhật thông tin vận chuyển vào shipment

        if (createOrderData == null || createOrderData.getOrderCode() == null) {
            throw new BadRequestException("Không thể tạo đơn vận chuyển trong GHN");
        }
        shipment.setTrackingNumber(createOrderData.getOrderCode());
        shipment.setStatus("READY_TO_PICK");
        shipmentRepository.save(shipment);
    }
    public String getPrintUrl(List<Order> orders) {
        log.info("[SHIPPING_SERVICE] getPrintUrl cho {} đơn hàng", orders.size());
        if (orders == null || orders.isEmpty()) {
            throw new BadRequestException("Danh sách đơn hàng trống");
        }
        
        List<UUID> orderIds = orders.stream()
                .map(Order::getId)
                .toList();
        
        log.info("[SHIPPING_SERVICE] Lấy tracking numbers cho orders: {}", orderIds);
        List<String> trackingNumbers = shipmentRepository.getAllTrackingNumbersForOrders(orderIds);
        log.info("[SHIPPING_SERVICE] Tìm thấy {} tracking numbers: {}", trackingNumbers.size(), trackingNumbers);
        
        if (trackingNumbers.isEmpty()) {
            throw new BadRequestException("Không tìm thấy mã vận đơn cho các đơn hàng đã chọn. Vui lòng đảm bảo đơn hàng đã được giao cho đơn vị vận chuyển.");
        }
        
        // Lọc ra các đơn hàng không có tracking number
        List<String> missingOrders = orders.stream()
                .filter(order -> {
                    List<Shipment> shipments = shipmentRepository.findAllByOrderId(order.getId());
                    return shipments.isEmpty() || 
                           shipments.stream().allMatch(s -> s.getTrackingNumber() == null || s.getTrackingNumber().isEmpty());
                })
                .map(Order::getOrderNumber)
                .toList();
        
        if (!missingOrders.isEmpty()) {
            log.error("[SHIPPING_SERVICE] Các đơn thiếu tracking number: {}", missingOrders);
            throw new BadRequestException("Các đơn hàng sau chưa có mã vận đơn: " + String.join(", ", missingOrders));
        }
        
        log.info("[SHIPPING_SERVICE] Gọi ghnService.getPrintOrderUrl");
        return ghnService.getPrintOrderUrl(trackingNumbers);
    }

    public Shipment getCurrentShipmentByOrder(UUID orderId) {
        return shipmentRepository.findByOrderIdAndIsActiveTrue(orderId).orElseThrow(()->new ShippingNotFoundException("Không tìm thấy thông tin giao hàng: " + orderId));
    }

    public void cancelShippingOrderInGHN(Shipment shipment) {
        if (shipment.getTrackingNumber()==null) {
            throw new BadRequestException("Đơn vận chuyển chưa được tạo trong GHN");
        }
        if (!ghnService.cancelShippingOrder(shipment.getTrackingNumber()))
        {
            throw new BadRequestException("Không hủy được đơn vận chuyển trong GHN");
        }
        shipment.setStatus("CANCELLED");
        shipmentRepository.save(shipment);
    }
    @Async
    public void returnOrderInGHN(Order order) {
        Shipment shipment = getCurrentShipmentByOrder(order.getId());
        if (Boolean.TRUE.equals(shipment.getIsReturnShipment()))
        {
            throw new BadRequestException("Đơn hàng đã có đơn trả hàng.");
        }
        if (!"DELIVERED".equals(shipment.getStatus()))
            throw new BadRequestException("Chỉ có thể tạo đơn trả hàng cho đơn đã giao.");
        List<ShipmentItem> shipmentItems = createShipmentItemsFromOrder(order);
        shipment.setIsActive(false);
        shipmentRepository.save(shipment);
        Shipment newShipment = Shipment.builder()
                .order(order)
                .carrier("GHN")
                .serviceLevel("2")
                .status("CREATED")
                .build();
        newShipment.addAllItems(shipmentItems);
        CreateShippingOrder createShippingOrder = ghnService.buildCreateOrderRequest(newShipment, order.getShippingAddress(),true);
        CreateOrderData createOrderData = ghnService.createShippingOrder(createShippingOrder);
        // Cập nhật thông tin vận chuyển vào shipment
        if (createOrderData == null || createOrderData.getOrderCode() == null) {
            throw new BadRequestException("Không thể tạo đơn trả hàng trong GHN");
        }
        newShipment.setTrackingNumber(createOrderData.getOrderCode());
        newShipment.setStatus("READY_TO_PICK");
        shipmentRepository.save(newShipment);
    }

    public ShipmentResponse getShipmentForOrder(UUID orderId) {
        Shipment shipment = getCurrentShipmentByOrder(orderId);
        return ShipmentResponse.builder()
                .id(shipment.getId())
                .deliveredAt(shipment.getDeliveredAt())
                .trackingNumber(shipment.getTrackingNumber())
                .warehouse(shipment.getWarehouse())
                .carrier(shipment.getCarrier())
                .build();
    }
    private List<ShipmentItem> createShipmentItemsFromOrder(Order order) {
        return order.getItems().stream()
                .map(orderItem -> ShipmentItem.builder()
                        .orderItem(orderItem)
                        .quantity(orderItem.getQuantity())
                        .build())
                .toList();
    }

    public List<Shipment> getAllShipmentsByOrder(UUID orderId) {
        return shipmentRepository.findAllByOrderId(orderId);
    }
}
