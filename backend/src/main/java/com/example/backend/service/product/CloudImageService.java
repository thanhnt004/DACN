package com.example.backend.service.product;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.backend.config.CloudinaryProps;
import com.example.backend.config.ImagePolicy;
import com.example.backend.dto.response.auth.CustomUserDetail;
import com.example.backend.dto.request.common.ImageUploadRequest;
import com.example.backend.dto.response.common.ImageUploadResponse;
import com.example.backend.excepton.AuthenticationException;
import com.example.backend.excepton.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudImageService {
    private final ImagePolicy imagePolicy;
    private final CloudinaryProps cloudinaryProps;
    private final Cloudinary cloudinary;
    public ImageUploadResponse sign(ImageUploadRequest request){
        String type = request.getType();
        UUID targetId = request.getTargetId();
        if (!StringUtils.hasText(type)||!imagePolicy.getType().containsKey(type))
            throw new BadRequestException("Invalid image type");
        if ("avatar".equals(type))
        {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetail userDetail) {
                UUID currentUserId = userDetail.getId();
                if (!targetId.equals(currentUserId))
                    throw new AuthenticationException(403,"forbidden");
            } else {
                // Handle the case where user is not authenticated or principal is not CustomUserDetail
                throw new AuthenticationException(401,"User not properly authenticated");
            }

        }
        long timestamp = System.currentTimeMillis() / 1000L;
        Map<String, Object> paramsToSign = new HashMap<>();
        paramsToSign.put("timestamp", String.valueOf(timestamp));
        String folder = String.format(imagePolicy.getType().get(type).folderPattern(),targetId != null ? targetId : "misc");
        paramsToSign.put("folder", folder);

        String signature = cloudinary.apiSignRequest(paramsToSign,cloudinaryProps.getApiSecret(), 1);

        return ImageUploadResponse.create(cloudinaryProps,imagePolicy.getType().get(type),signature,folder);
    }
    @Async
    public void destroy(String publicId)  {
        // options: version, invalidate, resource_type, type, etc.
        Map options = ObjectUtils.asMap(
                "invalidate", true
        );
        Map result = null;
        try {
            result = cloudinary.uploader().destroy(publicId, options);
        } catch (IOException e) {
            //fallback
            log.warn("Remove image fail!");
        }
    }

}
