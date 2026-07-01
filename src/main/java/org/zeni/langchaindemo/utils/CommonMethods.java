package org.zeni.langchaindemo.utils;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

public class CommonMethods {
    public static UserMessage extractUserMessage(String message, MultipartFile image) throws IOException {
        if (image == null || image.isEmpty()) {
            return UserMessage.from(message);
        }

        String mimeType = image.getContentType() != null ? image.getContentType() : MimeTypeUtils.IMAGE_PNG_VALUE;
        String base64 = Base64.getEncoder().encodeToString(image.getBytes());

        return UserMessage.from(
                TextContent.from(message),
                ImageContent.from(base64, mimeType)
        );
    }
}
