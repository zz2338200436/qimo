package com._202510007517.platform.common.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DevJwtKeyMaterialTest {

    @Test
    void exposesDevelopmentJwtKeyPairOutsideYamlConfiguration() {
        assertThat(DevJwtKeyMaterial.KID).isEqualTo("dev");
        assertThat(DevJwtKeyMaterial.publicKey().getAlgorithm()).isEqualTo("RSA");
        assertThat(DevJwtKeyMaterial.privateKey().getAlgorithm()).isEqualTo("RSA");
        assertThat(DevJwtKeyMaterial.publicKey().getModulus())
                .isEqualTo(DevJwtKeyMaterial.privateKey().getModulus());
    }
}
