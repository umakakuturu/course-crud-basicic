
import com.fasterxml.jackson.core.type.TypeReference;
import com.okta.sdk.resource.client.ApiClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.bcbsm.mex.auth.constants.OktaConstants.JTI_CLAIM_NAME;

@Service
@RequiredArgsConstructor
public class ActiveUserServiceImpl implements ActiveUserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveUserServiceImpl.class);
    @Value("${services.okta.member.token.domain}")
    private String oktaAddress;
    @Value("${services.okta.member.token.clientId}")
    private String oktaClientId;
    @Value("${services.okta.member.token.clientSecret}")
    private String oktaClientSecret;
    @Value("${services.okta.member.token.audience}")
    private String oktaTenantAudience;
    @Value("${services.okta.member.token.path}")
    private String oktaPath;
    private final ApiClient okatAuthClient;
    private final ActiveUserRepository activeUserRepository;
    private final OktaService oktaService;

    @Override
    public ActiveUser createAndCacheActiveUser(String subjectToken) {
        Optional<Jwt> jwt = Optional.of(SecurityContextHolder.getContext().getAuthentication())
                .map(auth -> (Jwt) auth.getCredentials());
        String jti = jwt.map(Jwt::getClaims).map(claim -> (String) claim.get(JTI_CLAIM_NAME)).orElse(null);

        //toDO: need to get this info from Membership service
        /*
        SearchCriteria allAccessibleMembershipCriteria = new SearchCriteria();
        allAccessibleMembershipCriteria.addFiltersItem(new Filters().name("type").operator("EQUALS").value("AUTHORIZATION_METADATA_POPULATOR"));
        MembershipResponse membershipResponse = this.membershipApi.getAllAccessibleMemberships(Long.valueOf(123), allAccessibleMembershipCriteria).block();
        */

        List<String> newScopes = Collections.emptyList();
        Map<String, String> headerParams = new HashMap<>();

        OktaTokenRequest exchangeRequest = OktaTokenRequest.builder()
                .grant_type("urn:ietf:params:oauth:grant-type:token-exchange")
                .subject_token_type("urn:ietf:params:oauth:token-type:access_token")
                .subject_token(subjectToken)
                .scopes(newScopes)
                .audience(oktaTenantAudience)
                .client_id(oktaClientId)
                .client_secret(oktaClientSecret)
                .build();

        OktaTokenResponse oktaTokenResponse = okatAuthClient.invokeAPI("/oauth2/auseizln69ZQfjd2v5d7/v1/token",
                "POST",
                Collections.emptyList(),
                Collections.emptyList(),
                "",
                exchangeRequest,
                headerParams,
                Collections.emptyMap(),
                Collections.emptyMap(),
                "",
                "application/x-www-form-urlencoded",
                new String[0],
                new TypeReference<OktaTokenResponse>() {
                });

        LOGGER.info("Exchanging token for current principal {} and jti {}", jwt.get().getSubject(), jti);

        ActiveUser activeUser = ActiveUser.builder()
                .jti(jti)
                .exchangedToken(oktaTokenResponse.getAccessToken())
                .isImpersonated(true)
                .tier("tier_2")
                .personid("901234567890")
                .firstname("User2")
                .deviceInfo("POSTMAN")
                .build();

        return activeUserRepository.save(activeUser);
    }

    @Override
    public ActiveUser createAndCacheActiveUserKey(String subjectToken, String userName) {
        UserAuthModel userRequest = new UserAuthModel();
        userRequest.setUserName(Collections.singletonList(userName));
        FindUsersResponse userResp = (FindUsersResponse) oktaService.execute(userRequest);
        User user = userResp.getPayload().get(0);
        ActiveUser activeUser = ActiveUser.builder()
                .jti(subjectToken)
                .isImpersonated(false)
                .tier("")
                .personid(user.getEnterpriseId().toString())
                .firstname(user.getUserName())
                .deviceInfo("IVR")
                .build();
        LOGGER.info("Cached active user for current token {}", subjectToken);
        return activeUserRepository.save(activeUser);
    }

    @Override
    public Optional<ActiveUser> findById(String id) {
        return activeUserRepository.findById(id);
    }

    @Override
    public String findByPersonId(String personId) {
        return activeUserRepository.findAllByPersonid(personId).toString();
    }

    @Override
    public String findByUserNameAndTier(String userName, String tier) {
        return activeUserRepository.findByFirstnameAndTier(userName, tier).toString();
    }
}
