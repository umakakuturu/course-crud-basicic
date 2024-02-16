 ActiveUser activeUser = ActiveUser.builder()
                .jti("someJti")
                .exchangedToken("someToken")
                .personid("somePersonId")
                .isImpersonated(false)
                .deviceInfo("someDeviceInfo")
                .authorities(Arrays.asList("ROLE_USER"))
                .build();
