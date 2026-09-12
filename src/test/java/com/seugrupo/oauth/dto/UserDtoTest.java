package com.seugrupo.oauth.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserDtoTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void responseSerializaNomesHifenizadosESemSenha() throws Exception {
        String json = objectMapper.writeValueAsString(
                new UserResponse("u-1", "ana@example.com", "Ana", "Silva", true));

        assertThat(json).contains("\"first-name\":\"Ana\"")
                .contains("\"last-name\":\"Silva\"")
                .doesNotContain("firstName", "lastName", "password");
    }

    @Test
    void responseAceitaNomesDoKeycloakNaDeserializacao() throws Exception {
        UserResponse response = objectMapper.readValue("""
                {"id":"u-1","username":"ana@example.com","firstName":"Ana",
                 "lastName":"Silva","enabled":true}
                """, UserResponse.class);

        assertThat(response.firstName()).isEqualTo("Ana");
        assertThat(response.lastName()).isEqualTo("Silva");
    }

    @Test
    void createRejeitaEmailInvalido() {
        CreateUserRequest request = new CreateUserRequest(
                "nao-e-email", "segredo", "Ana", "Silva");

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("username");
    }

    @Test
    void createAceitaLocalPartEntreAspas() {
        CreateUserRequest request = new CreateUserRequest(
                "\"quoted local\"@example.com", "segredo", "Ana", "Silva");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void createRejeitaCamposObrigatoriosNulosOuBrancos() {
        assertThat(validator.validate(new CreateUserRequest(null, null, " ", "")))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("username", "password", "firstName", "lastName");
    }

    @Test
    void updateRejeitaEmailInvalidoEEnabledAusente() {
        assertThat(validator.validate(new UpdateUserRequest("nao-e-email", "Ana", "Silva", null)))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("username", "enabled");
    }

    @Test
    void updatePasswordRejeitaSenhaAusenteOuBranca() {
        assertThat(validator.validate(new UpdatePasswordRequest(null)))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("password");
        assertThat(validator.validate(new UpdatePasswordRequest("   ")))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("password");
    }
}
