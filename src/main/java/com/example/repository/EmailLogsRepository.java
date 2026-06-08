package com.example.repository;

import com.example.model.entity.EmailLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailLogsRepository extends JpaRepository<EmailLogs, Long> {

}
