package com.accesshr.emsbackend.Repo.CompanyNewsRepo;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.accesshr.emsbackend.Entity.*;
  import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.List;

@Repository

public interface CompanyNewsJpaRepository extends JpaRepository <CompanyNews, Integer>{
  @Modifying
  @Transactional

@Query("DELETE FROM CompanyNews c WHERE c.createdAt < :expiryDate")
void deleteExpiredEntities(@Param("expiryDate") LocalDateTime expiryDate);

}

