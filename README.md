package com.bcbsm.mex.auth.controller;
/**
 * ForumSentry services exposed to fetch user details
 */
//import lombok.extern.slf4j.Slf4j;

import com.bcbsm.auth.server.api.FsuserApi;
import com.bcbsm.auth.server.model.IOktaObject;
import com.bcbsm.auth.server.model.UserRequest;
import com.bcbsm.mex.auth.enums.UserOperation;
import com.bcbsm.mex.auth.models.UserAuthModel;
import com.bcbsm.mex.auth.service.okta.OktaService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/service/v1/fs")
@RequiredArgsConstructor
//@Slf4j
public class FSUserController implements FsuserApi {
    private static final Logger log = LogManager.getLogger(FSUserController.class);

    private final OktaService oktaService;
    private final ModelMapper mapper;

    /**
     * This method is used to cover all set of
     * scenarios by passing different parameters
     * on multiple purposes based on below set.
     * <p>
     * userName - either string or list of strings - finding User or Users
     * eid - either string or list of strings - finding User or Users
     * emailId - string - finding User or Users
     * phoneNumber - string - finding User or Users
     * phoneType - string - finding User or Users
     * user - @com.bcbsm.auth.server.model.User - Create new user or Update existing User
     * tier0User - boolean
     * accountStatus - string
     * passStr - string
     * forcePasswordReset - boolean
     *
     * @param request
     * @return
     */
    @PostMapping("/{path}")
    public ResponseEntity<? extends IOktaObject> processUser(@PathVariable String path, @RequestBody UserRequest request) {
        UserAuthModel userAuthModel = mapper.map(request, UserAuthModel.class).toBuilder().userOperation(UserOperation.fromType(path)).build();
        return ResponseEntity.ok(oktaService.execute(userAuthModel));
    }
}

existing integration test 

import com.bcbsm.auth.server.model.CreateOrUpdateUsersResponse;
import com.bcbsm.auth.server.model.FindUserByEidResponse;
import com.bcbsm.auth.server.model.FindUsersByEidsResponse;
import com.bcbsm.auth.server.model.FindUsersResponse;
import com.bcbsm.auth.server.model.User;
import com.bcbsm.auth.server.model.UserRequest;
import com.bcbsm.mex.auth.MexAuthServiceApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import java.util.ArrayList;
import java.util.List;

import static org.hibernate.validator.internal.util.Contracts.assertNotEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
//@SpringBootTest(classes = {MexAuthServiceApplication.class})
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,classes = {MexAuthServiceApplication.class, FSUserController.class})
@ActiveProfiles("native")
public class FSUserControllerITest {

    @LocalServerPort
    private int port;

    TestRestTemplate restTemplate = new TestRestTemplate();

    HttpHeaders headers = new HttpHeaders();

    private UserRequest userRequest;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        userRequest = new UserRequest();
        userRequest.setApiType("post");
        userRequest.setPhoneNumber("123456756");
        //userRequest.setUserName(Arrays.asList("a", "b"));
        List<String> userNames = new ArrayList<>();
        userNames.add("AmandaSP");
        userNames.add("PATRICIAWESTON");
        userRequest.setUserName(userNames);
        List<String> eids = new ArrayList<>();
        eids.add("7159065");
        eids.add("125937");
        userRequest.setEid(eids);
        userRequest.setAccountStatus("accountStatus");
        userRequest.setEmailId("sthoutireddy@bcbsm.com");
        userRequest.setPassStr("passStr");
        userRequest.setUser(getUser());
        userRequest.setForcePasswordReset(true);

    }

    @Test
    void testFindByUsername() throws Exception {

        HttpEntity<UserRequest> entity = new HttpEntity<>(userRequest, headers);
        ResponseEntity<FindUsersResponse> response = restTemplate.exchange(
                createURLWithPort("/auth/service/v1/fs/findByUsername"),
                HttpMethod.POST, entity, FindUsersResponse.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody().getPayload());

    }
