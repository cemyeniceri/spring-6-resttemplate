package guru.springframework.spring6resttemplate.client;

import guru.springframework.spring6resttemplate.model.BeerDTO;
import guru.springframework.spring6resttemplate.model.BeerDTOPageImpl;
import guru.springframework.spring6resttemplate.model.BeerStyle;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BeerClientImpl implements BeerClient {

    private final RestTemplateBuilder restTemplateBuilder;

    public static final String BEER_PATH = "/api/v1/beer";
    public static final String BEER_BY_ID_PATH = "/api/v1/beer/{beerId}";

    @Override
    public void deleteBeer(UUID id) {
        RestTemplate restTemplate = restTemplateBuilder.build();
        restTemplate.delete(BEER_BY_ID_PATH, id);
    }

    @Override
    public Optional<BeerDTO> updateBeer(BeerDTO beerDTO) {
        RestTemplate restTemplate = restTemplateBuilder.build();
        restTemplate.put(BEER_BY_ID_PATH, beerDTO, beerDTO.getId());
        return getBeerById(beerDTO.getId());
    }

    @Override
    public Optional<BeerDTO> createBeer(BeerDTO newDTO) {
        RestTemplate restTemplate = restTemplateBuilder.build();

        Optional<URI> optURI = Optional.ofNullable(restTemplate.postForLocation(BEER_PATH, newDTO));
        return optURI.flatMap(uri -> Optional.ofNullable(restTemplate.getForObject(uri.getPath(), BeerDTO.class)));
    }

    @Override
    public Optional<BeerDTO> getBeerById(UUID beerId) {
        RestTemplate restTemplate = restTemplateBuilder.build();
        return Optional.ofNullable(restTemplate.getForObject(BEER_BY_ID_PATH, BeerDTO.class, beerId));
    }

    @Override
    public Page<BeerDTO> listBeers(Optional<String> optBeerName,
                                   Optional<BeerStyle> optBeerStyle,
                                   Optional<Boolean> optShowInventory,
                                   Optional<Integer> optPage,
                                   Optional<Integer> optSize) {
        RestTemplate restTemplate = restTemplateBuilder.build();

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BEER_PATH)
                .queryParamIfPresent("beerName", optBeerName)
                .queryParamIfPresent("beerStyle", optBeerStyle)
                .queryParamIfPresent("showInventory", optShowInventory)
                .queryParamIfPresent("pageNumber", optPage)
                .queryParamIfPresent("pageSize", optSize);

        ResponseEntity<BeerDTOPageImpl> forEntity = restTemplate.getForEntity(builder.toUriString(), BeerDTOPageImpl.class);
        return forEntity.getBody();
    }
}
