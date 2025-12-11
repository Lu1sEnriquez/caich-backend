package com.api_caich.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ConditionalAlumnoIdValidator.class)
@Documented
public @interface ConditionalAlumnoId {
    String message() default "El campo idAlumno es obligatorio cuando el rol es 'Alumno'";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
