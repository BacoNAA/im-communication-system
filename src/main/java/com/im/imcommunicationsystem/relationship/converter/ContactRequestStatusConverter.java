package com.im.imcommunicationsystem.relationship.converter;

import com.im.imcommunicationsystem.relationship.enums.ContactRequestStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * ContactRequestStatus 枚举转换器
 * 用于在数据库存储时使用 code 字段而不是枚举名称
 */
@Converter(autoApply = true)
public class ContactRequestStatusConverter implements AttributeConverter<ContactRequestStatus, String> {

    @Override
    public String convertToDatabaseColumn(ContactRequestStatus status) {
        if (status == null) {
            return null;
        }
        return status.getCode();
    }

    @Override
    public ContactRequestStatus convertToEntityAttribute(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        return ContactRequestStatus.fromCode(code);
    }
}