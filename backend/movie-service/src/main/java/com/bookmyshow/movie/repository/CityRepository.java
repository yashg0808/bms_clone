package com.bookmyshow.movie.repository;

import com.bookmyshow.movie.model.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CityRepository extends JpaRepository<City, UUID> {

    List<City> findByIsActiveTrueOrderByNameAsc();

    Optional<City> findByNameIgnoreCase(String name);
}
