package com.api_caich.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = RoleValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRole {
    String message() default "Rol no valido. Roles validos: Terapeuta, Alumno, Paciente, Administrador";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    boolean includeAdmin() default false; // Indica si se permite el rol Administrador
}