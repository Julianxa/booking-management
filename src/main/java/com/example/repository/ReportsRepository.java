package com.example.repository;

import com.example.constant.Enums;
import com.example.model.entity.Reports;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportsRepository extends JpaRepository<Reports, Long> {
  boolean existsByRefNo(String refNo);

  Optional<Reports> findByRefNo(String refNo);

  Page<Reports> findByReportType(Enums.ReportType reportType, Pageable pageable);
}
