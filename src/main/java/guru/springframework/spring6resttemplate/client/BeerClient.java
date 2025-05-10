package guru.springframework.spring6resttemplate.client;

import guru.springframework.spring6resttemplate.model.BeerDTO;
import guru.springframework.spring6resttemplate.model.BeerStyle;
import org.springframework.data.domain.Page;

import java.util.Optional;
import java.util.UUID;

public interface BeerClient {

    Optional<BeerDTO> createBeer(BeerDTO beerDTO);

    Optional<BeerDTO> getBeerById(UUID beerId);

    Page<BeerDTO> listBeers(Optional<String> optBeerName,
                            Optional<BeerStyle> optBeerStyle,
                            Optional<Boolean> optShowInventory,
                            Optional<Integer> optPage,
                            Optional<Integer> optSize);

    Optional<BeerDTO> updateBeer(BeerDTO beerDTO);

    void deleteBeer(UUID id);
}
