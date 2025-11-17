package com.example.backend.dto.request.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class ShippingItem
{
    private String name;
    private int quantity;
    private int weight;
    private int price;
}
