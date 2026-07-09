package com.genc.arfoms1.repository;

import com.genc.arfoms1.model.FrequentFlyer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FrequentFlyerRepository extends JpaRepository<FrequentFlyer, Integer> {

}