package com.api_caich.validation;

import com.api_caich.enums.EstadoUsuario;
import com.api_caich.enums.Role;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validador para la anotación @ValidEnum
 * Verifica que un string corresponda a un valor válido del enum especificado
 */
public class EnumValidator implements ConstraintValidator<ValidEnum, String> {
    
    private Class<? extends Enum<?>> enumClass;
    
    @Override
    public void initialize(ValidEnum annotation) {
        this.enumClass = annotation.enumClass();
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true; // null/empty se maneja con @NotNull/@NotBlank
        }
        
        // Validar según el tipo de enum
        if (enumClass == EstadoUsuario.class) {
            return EstadoUsuario.isValid(value);
        } else if (enumClass == Role.class) {
            return Role.isValid(value);
        }
        
        // Validación genérica para otros enums
        for (Enum<?> enumConstant : enumClass.getEnumConstants()) {
            if (enumConstant.name().equalsIgnoreCase(value)) {
                return true;
            }
        }
        
        return false;
    }
}
