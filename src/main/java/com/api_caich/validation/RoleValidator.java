package com.api_caich.validation;

import com.api_caich.enums.Role;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RoleValidator implements ConstraintValidator<ValidRole, String> {

    private boolean includeAdmin;

    @Override
    public void initialize(ValidRole constraintAnnotation) {
        this.includeAdmin = constraintAnnotation.includeAdmin();
    }

    @Override
    public boolean isValid(String roleName, ConstraintValidatorContext context) {
        if (roleName == null) {
            return false;
        }

        boolean isValid = Role.isValid(roleName);

        if (isValid && !includeAdmin) {
            return !Role.ADMINISTRADOR.getNombre().equalsIgnoreCase(roleName);
        }

        return isValid;
    }
}
