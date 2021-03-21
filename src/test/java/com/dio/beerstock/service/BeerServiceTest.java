package com.dio.beerstock.service;

import com.dio.beerstock.beerstock.dto.BeerDTO;
import com.dio.beerstock.beerstock.entity.Beer;
import com.dio.beerstock.beerstock.exception.BeerAlreadyRegisteredException;
import com.dio.beerstock.beerstock.exception.BeerNotFoundException;
import com.dio.beerstock.beerstock.exception.BeerStockExceededException;
import com.dio.beerstock.beerstock.exception.EmptyOrNegativeBeerStockException;
import com.dio.beerstock.beerstock.mapper.BeerMapper;
import com.dio.beerstock.beerstock.repository.BeerRepository;
import com.dio.beerstock.beerstock.service.BeerService;
import com.dio.beerstock.builder.BeerDTOBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BeerServiceTest {
    private static final long INVALID_BEER_ID = 1L;

    @Mock
    private BeerRepository beerRepository;

    private final BeerMapper beerMapper = BeerMapper.INSTANCE;

    @InjectMocks
    private BeerService beerService;

    @Test
    void whenBeerInformedThenItShuldBeCreated() throws BeerAlreadyRegisteredException {
        //give
        BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectedSaveBeer = beerMapper.toModel(expectedBeerDTO);

        //when
        when(beerRepository.findByName(expectedBeerDTO.getName())).thenReturn(Optional.empty());
        when(beerRepository.save(expectedSaveBeer)).thenReturn(expectedSaveBeer);

        //then
        BeerDTO createdBeerDTO = beerService.createBeer(expectedBeerDTO);
        assertThat(createdBeerDTO.getId(), is(equalTo(expectedBeerDTO.getId())));
        assertThat(createdBeerDTO.getName(), is(equalTo(expectedBeerDTO.getName())));
        assertThat(createdBeerDTO.getQuantity(), is(equalTo(expectedBeerDTO.getQuantity())));
    }

    @Test
    void whenAlreadyRegisteredBeerInformedThenAnExceptionShouldBeThrown() {
        //given
        BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer duplicatedBeer = beerMapper.toModel(expectedBeerDTO);

        //when
        when(beerRepository.findByName(expectedBeerDTO.getName())).thenReturn(Optional.of(duplicatedBeer));

        // then
        assertThrows(BeerAlreadyRegisteredException.class, () -> beerService.createBeer(expectedBeerDTO));
    }

    @Test
    void whenValidBeerNameIsGivenThenReturnABeer() throws BeerNotFoundException {
        //give
        BeerDTO expectedFoundBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectedFoundBeer = beerMapper.toModel(expectedFoundBeerDTO);

        //when
        when(beerRepository.findByName(expectedFoundBeer.getName()))
                .thenReturn(Optional.of(expectedFoundBeer));

        //then
        BeerDTO fundBeerDTO = beerService.findByName(expectedFoundBeerDTO.getName());
        assertThat(fundBeerDTO, is(equalTo(expectedFoundBeerDTO)));
    }

    @Test
    void whenNoRegisteredBeerNameIsGivenThenThrowAnException() throws BeerNotFoundException {
        //give
        BeerDTO expectedFoundBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();

        //when
        when(beerRepository.findByName(expectedFoundBeerDTO.getName()))
                .thenReturn(Optional.empty());

        //then
        assertThrows(
            BeerNotFoundException.class,
            () -> beerService.findByName(expectedFoundBeerDTO.getName())
        );
    }

    @Test
    void whenListBeerIsCalledThenReturnAListOfBeers() {
        //give
        BeerDTO expectedFoundBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectedFoundBeer = beerMapper.toModel(expectedFoundBeerDTO);

        //when
        when(beerRepository.findAll()).thenReturn(Collections.singletonList(expectedFoundBeer));

        //then
        List<BeerDTO> foundBeerDTO = beerService.listAll();
        assertThat(foundBeerDTO, is(not(empty())));
        assertThat(foundBeerDTO.get(0), is(equalTo(expectedFoundBeerDTO)));
    }

    @Test
    void whenListBeerIsCalledThenReturnAnEmptyListOfBeers() {
        //when
        when(beerRepository.findAll()).thenReturn(Collections.EMPTY_LIST);

        //then
        List<BeerDTO> foundBeerDTO = beerService.listAll();
        assertThat(foundBeerDTO, is(empty()));
    }

    @Test
    void whenExclusionIsCalledWithValidIdThenABeerShouldBeDeleted() throws BeerNotFoundException {
        //give
        BeerDTO expectedDeletedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectedDeletedBeer = beerMapper.toModel(expectedDeletedBeerDTO);

        //when
        when(beerRepository.findById(expectedDeletedBeer.getId()))
                .thenReturn(Optional.of(expectedDeletedBeer));
        doNothing().when(beerRepository).deleteById(expectedDeletedBeer.getId());

        //then
        beerService.deletedById(expectedDeletedBeerDTO.getId());
        verify(beerRepository, times(1)).findById(expectedDeletedBeerDTO.getId());
        verify(beerRepository, times(1)).deleteById(expectedDeletedBeerDTO.getId());
    }

    @Test
    void whenExclusionIsCalledWithInvalidIdThenAException() {
        //give
        BeerDTO expectedDeletedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        expectedDeletedBeerDTO.setId(INVALID_BEER_ID);

        //when
        when(beerRepository.findById(expectedDeletedBeerDTO.getId())).thenReturn(Optional.empty());

        //then
        assertThrows(
            BeerNotFoundException.class,
            () -> beerService.deletedById(expectedDeletedBeerDTO.getId())
        );
    }

    @Test
    void whenIncrementIsCalledThenIncrementBeerStock() throws BeerNotFoundException, BeerStockExceededException {
        //give
        BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectedBeer = beerMapper.toModel(expectedBeerDTO);

        //when
        when(beerRepository.findById(expectedBeerDTO.getId())).thenReturn(Optional.of(expectedBeer));
        when(beerRepository.save(expectedBeer)).thenReturn(expectedBeer);

        int quantityToIncrement = 10;
        int expectedQuantityAfterIncrement = expectedBeerDTO.getQuantity() + quantityToIncrement;

        //then
        BeerDTO incrementBeerDTO = beerService.increment(expectedBeerDTO.getId(), quantityToIncrement);

        assertThat(expectedQuantityAfterIncrement, equalTo(incrementBeerDTO.getQuantity()));
        assertThat(expectedQuantityAfterIncrement, lessThan(incrementBeerDTO.getMax()));
    }

    @Test
    void whenIncrementIsGreatherThanMaxThenThrowException() {
        //give
        BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectedBeer = beerMapper.toModel(expectedBeerDTO);

        //when
        when(beerRepository.findById(expectedBeerDTO.getId())).thenReturn(Optional.of(expectedBeer));

        int quantityToIncrement = 80;

        //then
        assertThrows(
            BeerStockExceededException.class,
            () -> beerService.increment(expectedBeerDTO.getId(), quantityToIncrement)
        );
    }

    @Test
    void whenIncrementAfterSumIsGreatherThanMaxThenThrowException() {
        //give
        BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectedBeer = beerMapper.toModel(expectedBeerDTO);
        expectedBeer.setQuantity(45);

        //when
        when(beerRepository.findById(expectedBeerDTO.getId())).thenReturn(Optional.of(expectedBeer));

        int quantityToIncrement = 10;

        //then
        assertThrows(
                BeerStockExceededException.class,
                () -> beerService.increment(expectedBeerDTO.getId(), quantityToIncrement)
        );
    }

    @Test
    void whenIncrementIsCalledWithInvalidIdThenThrowException() {
        //give
        BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        expectedBeerDTO.setId(INVALID_BEER_ID);

        int quantityToIncrement = 10;

        //then
        assertThrows(
            BeerNotFoundException.class,
            () -> beerService.increment(expectedBeerDTO.getId(), quantityToIncrement)
        );
    }

    @Test
    void whenDecrementIsCalledThenIncrementBeerStock() throws BeerNotFoundException, EmptyOrNegativeBeerStockException {
        //give
        BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectedBeer = beerMapper.toModel(expectedBeerDTO);

        //when
        when(beerRepository.findById(expectedBeerDTO.getId())).thenReturn(Optional.of(expectedBeer));
        when(beerRepository.save(expectedBeer)).thenReturn(expectedBeer);

        int quantityToDecrement = 3;
        int expectedQuantityAfterDecrement = expectedBeerDTO.getQuantity() - quantityToDecrement;

        //then
        BeerDTO incrementBeerDTO = beerService.decrement(expectedBeerDTO.getId(), quantityToDecrement);

        assertThat(expectedQuantityAfterDecrement, equalTo(incrementBeerDTO.getQuantity()));
    }

    @Test
    void whenDecrementIsCalledWithInvalidIdThenThrowException() {
        //give
        BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        expectedBeerDTO.setId(INVALID_BEER_ID);

        int quantityToDecrement = 10;

        //then
        assertThrows(
            BeerNotFoundException.class,
            () -> beerService.increment(expectedBeerDTO.getId(), quantityToDecrement)
        );
    }

    @Test
    void whenDecrementIsGreatherThanZeroOrEmptyThenThrowException() {
        //give
        BeerDTO expectedBeerDTO = BeerDTOBuilder.builder().build().toBeerDTO();
        Beer expectedBeer = beerMapper.toModel(expectedBeerDTO);

        //when
        when(beerRepository.findById(expectedBeerDTO.getId())).thenReturn(Optional.of(expectedBeer));

        int quantityToDecrement = 20;

        //then
        assertThrows(
            EmptyOrNegativeBeerStockException.class,
            () -> beerService.decrement(expectedBeerDTO.getId(), quantityToDecrement)
        );
    }
}
