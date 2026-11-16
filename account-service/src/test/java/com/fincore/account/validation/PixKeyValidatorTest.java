package com.fincore.account.validation;

import com.fincore.account.exception.BusinessException;
import com.fincore.account.model.PixKeyType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PixKeyValidator - Testes Unitários")
class PixKeyValidatorTest {

    @Test
    @DisplayName("Deve normalizar CPF e telefone para apenas dígitos")
    void normalizeCpfAndPhone() {
        assertThat(PixKeyValidator.normalize(PixKeyType.CPF, "529.982.247-25"))
                .isEqualTo("52998224725");
        assertThat(PixKeyValidator.normalize(PixKeyType.PHONE, "(11) 99999-9999"))
                .isEqualTo("11999999999");
    }

    @Test
    @DisplayName("Deve normalizar e-mail para minúsculas")
    void normalizeEmail() {
        assertThat(PixKeyValidator.normalize(PixKeyType.EMAIL, "  Maria@Fincore.COM "))
                .isEqualTo("maria@fincore.com");
    }

    @Test
    @DisplayName("Deve gerar UUID quando chave aleatória não for informada")
    void normalizeRandom_generatesUuid() {
        String value = PixKeyValidator.normalize(PixKeyType.RANDOM, null);

        assertThat(UUID.fromString(value)).isNotNull();
    }

    @Test
    @DisplayName("Deve validar chaves suportadas")
    void validateSupportedKeys() {
        PixKeyValidator.validate(PixKeyType.CPF, "52998224725");
        PixKeyValidator.validate(PixKeyType.EMAIL, "maria@fincore.com");
        PixKeyValidator.validate(PixKeyType.PHONE, "11999999999");
        PixKeyValidator.validate(PixKeyType.RANDOM, UUID.randomUUID().toString());
    }

    @Test
    @DisplayName("Deve rejeitar chaves inválidas")
    void validateInvalidKeys() {
        assertThatThrownBy(() -> PixKeyValidator.validate(PixKeyType.CPF, "11111111111"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> PixKeyValidator.validate(PixKeyType.EMAIL, "invalido"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> PixKeyValidator.validate(PixKeyType.PHONE, "123"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> PixKeyValidator.validate(PixKeyType.RANDOM, "nao-uuid"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Deve normalizar CPF/telefone formatados na busca por chave")
    void normalizeForLookup_formattedCpfAndPhone() {
        assertThat(PixKeyValidator.normalizeForLookup("529.982.247-25")).isEqualTo("52998224725");
        assertThat(PixKeyValidator.normalizeForLookup("(11) 99999-9999")).isEqualTo("11999999999");
    }

    @Test
    @DisplayName("Deve manter UUID e normalizar e-mail na busca")
    void normalizeForLookup_emailAndUuid() {
        String uuid = UUID.randomUUID().toString();

        assertThat(PixKeyValidator.normalizeForLookup("Maria@Fincore.COM"))
                .isEqualTo("maria@fincore.com");
        assertThat(PixKeyValidator.normalizeForLookup(uuid)).isEqualTo(uuid);
    }
}
