package com.api_caich.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Validación personalizada para verificar que un valor string
 * corresponda a un valor válido de un enum
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EnumValidator.class)
@Documented
public @interface ValidEnum {
    
    /**
     * La clase del enum a validar
     */
    Class<? extends Enum<?>> enumClass();
    
    /**
     * Mensaje de error personalizado
     */
    String message() default "Valor no válido";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}
