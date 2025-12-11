package com.api_caich.validation;

import com.api_caich.dto.RegisterRequestDTO;
import com.api_caich.dto.usuario.CrearUsuarioDTO;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ConditionalAlumnoIdValidator implements ConstraintValidator<ConditionalAlumnoId, CrearUsuarioDTO > {

    @Override
    public boolean isValid(CrearUsuarioDTO  dto, ConstraintValidatorContext context) {
        if (dto == null) {
            return true; // se maneja con @NotNull si fuera necesario
        }

        // Si el rol es Alumno y no hay idAlumno -> invalido
        if ("Alumno".equalsIgnoreCase(dto.getRol()) && (dto.getIdAlumno() == null || dto.getIdAlumno().isBlank())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("El campo idAlumno es obligatorio cuando el rol es 'Alumno'")
                    .addPropertyNode("idAlumno") // marca el error sobre ese campo
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
