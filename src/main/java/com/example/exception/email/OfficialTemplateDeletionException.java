package com.example.exception.email;

import com.example.exception.BusinessException;
import static com.example.exception.ErrorCode.OFFICIAL_TEMPLATE;

public class OfficialTemplateDeletionException extends BusinessException {
    public OfficialTemplateDeletionException() {
        super(OFFICIAL_TEMPLATE);
    }
    public OfficialTemplateDeletionException(String message) {
        super(OFFICIAL_TEMPLATE, message);
    }

}
