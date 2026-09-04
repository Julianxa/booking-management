package com.example.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Existing image by key, or new upload by index into eventPics")
public class EventPicItemDTO {

    @Schema(description = "Existing S3 object key from GET event_pic_keys")
    @JsonProperty("key")
    private String key;

    @Schema(description = "0-based index into the multipart eventPics files for a new image")
    @JsonProperty("upload_index")
    private Integer uploadIndex;
}
