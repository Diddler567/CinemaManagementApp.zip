package com.example.backend.entities;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ProgramStateConverter implements AttributeConverter<ProgramState, String> {

    @Override
    public String convertToDatabaseColumn(ProgramState attribute) {
        if (attribute == null) return null;
        return attribute.name(); 
    }

    @Override
    public ProgramState convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        String v = dbData.trim().toUpperCase();
        if ("FINAL_SUBMISSION".equals(v)) v = "FINAL_PUBLICATION";
        return ProgramState.valueOf(v);
    }
}

