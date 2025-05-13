package guru.springframework.spring6resttemplate.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import guru.springframework.spring6resttemplate.config.OAuthClientInterceptor;
import guru.springframework.spring6resttemplate.config.RestTemplateBuilderConfig;
import guru.springframework.spring6resttemplate.model.BeerDTO;
import guru.springframework.spring6resttemplate.model.BeerDTOPageImpl;
import guru.springframework.spring6resttemplate.model.BeerStyle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@RestClientTest
@Import(RestTemplateBuilderConfig.class)
public class BeerClientMockTest {

    static final String URL = "http://localhost:8080";
    private final String BEARER_TEST = "Bearer test";

    BeerClient beerClient;

    MockRestServiceServer mockServer;

    @Autowired
    RestTemplateBuilder restTemplateBuilderConfigured;

    @Autowired
    ObjectMapper objectMapper;

    @Mock
    RestTemplateBuilder mockRestTemplateBuilder = new RestTemplateBuilder(new MockServerRestTemplateCustomizer());

    BeerDTO beerDto;
    String payload;

    @MockBean
    OAuth2AuthorizedClientManager manager;

    @TestConfiguration
    public static class TestConfig {
        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            return new InMemoryClientRegistrationRepository(ClientRegistration
                    .withRegistrationId("springauth")
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .clientId("test")
                    .tokenUri("test")
                    .build()
            );
        }

        @Bean
        OAuth2AuthorizedClientService oAuth2AuthorizedClientService(ClientRegistrationRepository clientRegistrationRepository) {
            return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
        }

        @Bean
        OAuthClientInterceptor oAuthClientInterceptor(OAuth2AuthorizedClientManager manager, ClientRegistrationRepository clientRegistrationRepository) {
            return new OAuthClientInterceptor(manager, clientRegistrationRepository);
        }
    }

    @Autowired
    ClientRegistrationRepository clientRegistrationRepository;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId("springauth");

        OAuth2AccessToken token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "test", Instant.MIN, Instant.MAX);
        when(manager.authorize(any())).thenReturn(new OAuth2AuthorizedClient(clientRegistration, "test", token));

        RestTemplate restTemplate = restTemplateBuilderConfigured.build();
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
        when(mockRestTemplateBuilder.build()).thenReturn(restTemplate);
        beerClient = new BeerClientImpl(mockRestTemplateBuilder);

        beerDto = getBeerDto();
        payload = objectMapper.writeValueAsString(beerDto);
    }

    @Test
    void testListBeersWithQueryParams() throws JsonProcessingException {
        String payload = objectMapper.writeValueAsString(getPage());
        mockServer.expect(method(HttpMethod.GET))
                .andExpect(requestTo(URL + BeerClientImpl.BEER_PATH + "?beerName=ALE&beerStyle=IPA&showInventory=false&pageNumber=1&pageSize=25"))
                .andExpect(header("Authorization", BEARER_TEST))
                .andRespond(withSuccess(payload, MediaType.APPLICATION_JSON));

        Page<BeerDTO> beerDTOS = beerClient.listBeers(Optional.of("ALE"), Optional.of(BeerStyle.IPA), Optional.of(false), Optional.of(1), Optional.of(25));
        assertThat(beerDTOS.getContent().size()).isGreaterThan(0);
        mockServer.verify();
    }

    @Test
    void testListBeers() throws JsonProcessingException {
        String payload = objectMapper.writeValueAsString(getPage());

        mockServer.expect(method(HttpMethod.GET))
                .andExpect(requestTo(URL + BeerClientImpl.BEER_PATH))
                .andExpect(header("Authorization", BEARER_TEST))
                .andRespond(withSuccess(payload, MediaType.APPLICATION_JSON));

        Page<BeerDTO> beerDTOS = beerClient.listBeers(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertThat(beerDTOS.getContent().size()).isGreaterThan(0);
    }

    @Test
    void testGetBeer() {

        mockGetOperation();

        Optional<BeerDTO> beerById = beerClient.getBeerById(beerDto.getId());
        assertThat(beerById).isNotNull();
        assertThat(beerById.get().getId()).isEqualTo(beerDto.getId());
    }

    @Test
    void testCreateBeer() {

        URI uri = UriComponentsBuilder.fromPath(BeerClientImpl.BEER_BY_ID_PATH).build(beerDto.getId());

        mockServer.expect(method(HttpMethod.POST))
                .andExpect(requestTo(URL + BeerClientImpl.BEER_PATH))
                .andExpect(header("Authorization", BEARER_TEST))
                .andRespond(withAccepted().location(uri));

        mockGetOperation();

        Optional<BeerDTO> beerById = beerClient.createBeer(beerDto);
        assertThat(beerById).isNotNull();
        assertThat(beerById.get().getId()).isEqualTo(beerDto.getId());
    }

    @Test
    void testUpdateBeer() {
        mockServer.expect(method(HttpMethod.PUT))
                .andExpect(requestToUriTemplate(URL + BeerClientImpl.BEER_BY_ID_PATH, beerDto.getId()))
                .andExpect(header("Authorization", BEARER_TEST))
                .andRespond(withNoContent());

        mockGetOperation();

        Optional<BeerDTO> optUpdatedBeerDTO = beerClient.updateBeer(beerDto);

        assertThat(optUpdatedBeerDTO).isNotNull();
        assertThat(optUpdatedBeerDTO.get().getId()).isEqualTo(beerDto.getId());
    }

    @Test
    void testDeleteBeer() {
        mockServer.expect(method(HttpMethod.DELETE))
                .andExpect(requestToUriTemplate(URL + BeerClientImpl.BEER_BY_ID_PATH, beerDto.getId()))
                .andExpect(header("Authorization", BEARER_TEST))
                .andRespond(withNoContent());

        beerClient.deleteBeer(beerDto.getId());

        mockServer.verify();
    }

    @Test
    void testDeleteNotFound() {
        mockServer.expect(method(HttpMethod.DELETE))
                .andExpect(requestToUriTemplate(URL + BeerClientImpl.BEER_BY_ID_PATH, beerDto.getId()))
                .andExpect(header("Authorization", BEARER_TEST))
                .andRespond(withResourceNotFound());

        assertThrows(HttpClientErrorException.class, () -> beerClient.deleteBeer(beerDto.getId()));

        mockServer.verify();
    }

    private void mockGetOperation() {
        mockServer.expect(method(HttpMethod.GET))
                .andExpect(requestToUriTemplate(URL + BeerClientImpl.BEER_BY_ID_PATH, beerDto.getId()))
                .andExpect(header("Authorization", BEARER_TEST))
                .andRespond(withSuccess(payload, MediaType.APPLICATION_JSON));
    }

    BeerDTO getBeerDto() {
        return BeerDTO.builder()
                .id(UUID.randomUUID())
                .price(new BigDecimal("10.99"))
                .beerName("Mango Bobs")
                .beerStyle(BeerStyle.IPA)
                .quantityOnHand(500)
                .upc("12345")
                .build();
    }

    BeerDTOPageImpl getPage() {
        return new BeerDTOPageImpl(
                Collections.singletonList(getBeerDto()),
                1,
                25,
                1);
    }
}
