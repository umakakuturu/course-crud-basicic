   @PostMapping({CmsConstants.CMS_ENDPOINT})
    public ResponseEntity<S> getCmsContent(@PathVariable(CmsConstants.CMS_PATH_VARIABLE) ServiceType serviceType,
                                           @RequestBody CmsRequest request) {
        log.info("posting CmsRequest ... {}", request);
        S type = serviceRegistry.getCmsService(serviceType).process((R) request);
        return new ResponseEntity<>(type,
                type != null ? HttpStatusCode.valueOf(200) :
                        HttpStatusCode.valueOf(204));
    }
	
	
	return type is not covering for below testcase
	
	@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {CmsServiceApplication.class})
@AutoConfigureMockMvc
public class CmsControllerTest {
    @Mock
    ServiceRegistry serviceRegistry;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    MockMvc mockMvc;

  
  
    @Test
    public void testPilotEligibility() {

        testExecution(ServiceType.PILOT_ELIGIBILITY_CONTENT, status().isOk());
    }
 private void testExecution(ServiceType serviceType, ResultMatcher resultMatcher) {
        CmsRequest cmsRequest = new CmsRequest();
        GroupRequest request = GroupRequest.builder().type("eligibility").groupOperation(GroupOperation.GROUPS_BY_TYPE).build();
        cmsRequest.setGroupRequest(request);
        cmsRequest.setInput("AAA");
        PilotRequest pilotRequest = PilotRequest.builder().groupNumbers(Arrays.asList(new String[]{"80317", "80322", "88881", "34553"})).
                contractNumbers(Arrays.asList(new String[]{"123456789", "123456789", "224467779", "987654321"})).build();
        cmsRequest.setPilotRequest(pilotRequest);
        MvcResult result = null;
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String cmsRequestJson = objectMapper.writeValueAsString(cmsRequest);
            result = mockMvc.perform(MockMvcRequestBuilders.post(CmsConstants.CMS_ENDPOINT, serviceType.getType())
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .content(cmsRequestJson))
                    .andExpect(status().isOk())
                    .andExpect(resultMatcher)
                    .andReturn();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
