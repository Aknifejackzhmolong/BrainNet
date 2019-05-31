package com.brainsci.security.repository;

import com.brainsci.security.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserRepository extends JpaRepository<UserEntity, String>, JpaSpecificationExecutor<UserEntity> {

    List<UserEntity> findByUsername(String username);

    List<UserEntity> findByEMail(String email);

    @Query(value = "select password(?1)", nativeQuery = true)
    String getBcryt(String password);

    @Modifying
    @Transactional
    @Query(value = "delete from UserEntity s where s.username = ?1")
    int deleteAllByStuId(String stuId);
}
