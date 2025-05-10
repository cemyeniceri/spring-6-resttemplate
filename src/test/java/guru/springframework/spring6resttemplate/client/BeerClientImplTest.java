package guru.springframework.spring6resttemplate.client;

import guru.springframework.spring6resttemplate.model.BeerDTO;
import guru.springframework.spring6resttemplate.model.BeerStyle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BeerClientImplTest {

    @Autowired
    BeerClient beerClient;

    @Test
    void testDeleteBeers() {
        BeerDTO newDto = BeerDTO.builder()
                .price(new BigDecimal("10.99"))
                .beerName("Mango Bobs 2")
                .beerStyle(BeerStyle.IPA)
                .quantityOnHand(500)
                .upc("12345")
                .build();

        Optional<BeerDTO> optSavedBeer = beerClient.createBeer(newDto);

        beerClient.deleteBeer(optSavedBeer.get().getId());

        assertThrows(HttpClientErrorException.class,
                () -> beerClient.getBeerById(optSavedBeer.get().getId()));
    }

    @Test
    void testUpdateBeer() {
        BeerDTO newDto = BeerDTO.builder()
                .price(new BigDecimal("10.99"))
                .beerName("Mango Bobs 2")
                .beerStyle(BeerStyle.IPA)
                .quantityOnHand(500)
                .upc("12345")
                .build();

        Optional<BeerDTO> optSavedBeer = beerClient.createBeer(newDto);

        final String newBeerName = "Mango Bobs 3";
        optSavedBeer.get().setBeerName(newBeerName);

        Optional<BeerDTO> optUpdatedBeer = beerClient.updateBeer(optSavedBeer.get());

        assertEquals(newBeerName, optUpdatedBeer.get().getBeerName());
    }

    @Test
    void testCreateBeer() {
        BeerDTO newDto = BeerDTO.builder()
                .price(new BigDecimal("10.99"))
                .beerName("Mango Bobs")
                .beerStyle(BeerStyle.IPA)
                .quantityOnHand(500)
                .upc("12345")
                .build();

        Optional<BeerDTO> optionalSavedDto = beerClient.createBeer(newDto);
        assertTrue(optionalSavedDto.isPresent());
    }

    @Test
    void testGetBeerById() {
        Page<BeerDTO> beerDTOS = beerClient.listBeers(Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());

        BeerDTO beerDTO = beerDTOS.getContent().get(0);

        Optional<BeerDTO> optionalBeerDTO = beerClient.getBeerById(beerDTO.getId());

        assertTrue(optionalBeerDTO.isPresent());
    }

    @Test
    void testListBeersWithoutBeerName() {
        beerClient.listBeers(Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    @Test
    void testListBeersWithBeerName() {
        beerClient.listBeers(Optional.of("ALE"), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
}