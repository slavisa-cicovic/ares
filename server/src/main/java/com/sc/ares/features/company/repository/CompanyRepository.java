package com.sc.ares.features.company.repository;

import com.sc.ares.features.company.entity.Company;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    Optional<Company> findByNameAndDeletedAtIsNull(String name);
}
