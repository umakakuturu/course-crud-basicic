@Test
    public void testCreateAndCacheActiveUser() {
        // Mocking
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaims()).thenReturn(Collections.singletonMap(OktaConstants.JTI_CLAIM_NAME, "someJti"));

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(new UsernamePasswordAuthenticationToken(jwt, ""));
        SecurityContextHolder.setContext(securityContext);

        // Test
        ActiveUser activeUser = activeUserService.createAndCacheActiveUser("subjectToken");

        // Assertions
        assertEquals("someJti", activeUser.getJti());
        // Add more assertions as needed
    }
