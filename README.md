import com.bcbsm.mex.auth.controller.TokenController;
import com.bcbsm.mex.auth.service.ActiveUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TokenControllerTest {

    @Mock
    private ActiveUserService activeUserService;

    @InjectMocks
    private TokenController tokenController;

    @Test
    public void testExchangeToken() {
        // Mocking
        String authorization = "Bearer token";
        List<String> tokenList = Arrays.asList(authorization.split(" "));
        Optional<String> token = Optional.ofNullable(tokenList.get(1));

        // Mock the activeUserService
        when(activeUserService.createAndCacheActiveUser(anyString())).thenReturn(/* mock return value */);

        // Test
        ResponseEntity<Void> responseEntity = tokenController.exchangeToken(authorization);

        // Assertions
        verify(activeUserService).createAndCacheActiveUser(anyString());
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }
}
