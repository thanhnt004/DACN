package com.example.backend.service.shipping;

import com.example.backend.dto.ghn.CreateOrderData;
import com.example.backend.dto.ghn.CreateShippingOrder;
import com.example.backend.dto.response.checkout.CheckoutItemDetail;
import com.example.backend.dto.response.checkout.CheckoutSession;
import com.example.backend.dto.response.shipping.ShippingOption;
import com.example.backend.dto.response.user.UserAddress;
import com.example.backend.exception.shipping.ShippingNotFoundException;
import com.example.backend.model.ShipmentItem;
import com.example.backend.model.order.Order;
import com.example.backend.model.order.Shipment;
import com.example.backend.repository.shipping.ShipmentRepository;
import com.example.backend.service.user.UserManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShippingService {
    private final GHNService ghnService;
    private final UserManagerService userManagerService;
    private final ShipmentRepository shipmentRepository;
    public List<ShippingOption> getAvailableShippingOptions(Optional<UserAddress> userAddressOps, List<CheckoutItemDetail> items) {
        return ghnService.getShippingOptions(items, userAddressOps);
    }
    @Transactional
    public Shipment createShipmentForOrder(Order order, CheckoutSession checkoutSession) {
        List<ShipmentItem> shipmentItems = order.getItems().stream()
                .map(orderItem -> ShipmentItem.builder()
                        .orderItem(orderItem)
                        .quantity(orderItem.getQuantity())
                        .build())
                .toList();
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
        CreateShippingOrder createShippingOrder = ghnService.buildCreateOrderRequest(shipment, userAddress);
        CreateOrderData createOrderData = ghnService.createShippingOrder(createShippingOrder);
        // Cập nhật thông tin vận chuyển vào shipment
        shipment.setTrackingNumber(createOrderData.getOrderCode());
        shipment.setStatus("SHIPPED");
        shipmentRepository.save(shipment);
    }
    public String getPrintUrl(List<Order> orders) {
        List<String> trackingNumbers = orders.stream()
                .map(order -> {
                    Shipment shipment;
                    shipment = shipmentRepository.findByOrderId(order.getId())
                            .orElseThrow(() -> new ShippingNotFoundException("Shipment not found for order: " + order.getId()));
                    return shipment.getTrackingNumber();
                }).toList();

        return ghnService.getPrintOrderUrl(trackingNumbers);
    }
    public String cancelShipment(UUID orderId) {
        // Lấy shipment từ DB
        Shipment shipment = shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin giao hàng: " + orderId));

        // Gọi GHN API để hủy
        ghnService.cancelShippingOrder(shipment.getTrackingNumber());

        // Cập nhật trạng thái trong DB
        shipment.setStatus("CANCELLED");
        shipmentRepository.save(shipment);

        return "Shipment cancelled successfully";
    }

    public Shipment getShipmentByOrderId(UUID orderId) {
        return shipmentRepository.findByOrderId(orderId).orElseThrow(()->new ShippingNotFoundException("Không tìm thấy thông tin giao hàng: " + orderId));
    }

    public void cancelShippingOrderInGHN(Shipment shipmentByOrderId) {
        
    }

    public void returnOrderInGHN(Shipment shipmentByOrderId) {
    }
}
