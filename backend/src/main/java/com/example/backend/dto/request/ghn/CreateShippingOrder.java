package com.example.backend.dto.request.ghn;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.QAbstractAuditable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateShippingOrder {
    @JsonUnwrapped
    private GHNApiInfo info;
    //sender info
    private String fromName;
    private String fromPhone;
    private String fromAddress;
    private String fromWardName;
    private String fromDistrictName;
    private String fromProvinceName;
    //receiver info
    private String toName;
    private String toPhone;
    private String toAddress;
    private String toWardCode;
    private String toDistrictId;
    //items info
    @JsonUnwrapped
    private ParcelInfor parcelInfor;
    //service info
    private int serviceTypeId;
    private int paymentTypeId;
    private int requiredNote;


}
