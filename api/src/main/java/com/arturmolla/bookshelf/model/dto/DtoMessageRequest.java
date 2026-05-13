package com.arturmolla.bookshelf.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request body for sending a new direct message.
 * Set {@code replyToId} to the ID of the message being quoted/replied to.
 * Leave it {@code null} (or omit the field) for a regular top-level message.
 * <p>
 * If uploading media, use multipart/form-data and set media accordingly.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoMessageRequest {
        @NotBlank(message = "Message content must not be blank")
        @Size(max = 4000, message = "Message must be at most 4000 characters")
        private String content;

        private Long replyToId;

        private MultipartFile media;
}
