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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
        Shipment shipment = Shipment.builder()
                .order(order)
                .carrier("GHN")
                .serviceLevel(checkoutSession.getSelectedShippingMethod().getServiceLevel())
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
        List<UUID> orderIds = orders.stream()
                .map(Order::getId)
                .toList();
        List<String> trackingNumbers = shipmentRepository.getAllTrackingNumbersForOrders(orderIds);
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
        if (shipment.isReturnShipment())
        {
            throw new BadRequestException("Đơn hàng đã có đơn trả hàng.");
        }
        if (!"DELIVERED".equals(shipment.getStatus()))
            throw new BadRequestException("Chỉ có thể tạo đơn trả hàng cho đơn đã giao.");
        List<ShipmentItem> shipmentItems = createShipmentItemsFromOrder(order);
        shipment.setActive(false);
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
