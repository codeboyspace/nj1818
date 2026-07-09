package com.genc.arfoms1.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.genc.arfoms1.model.CrewAssignment;


@Repository
public interface CrewRepository extends JpaRepository<CrewAssignment, Integer> {
//select*from
    List<CrewAssignment> findByCrewMemberName(String crewMemberName);

}


