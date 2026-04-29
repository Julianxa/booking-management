package com.example.repository;

import com.example.model.entity.UsersLoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsersLoginHistoryRepository extends JpaRepository<UsersLoginHistory, Long> {

}

