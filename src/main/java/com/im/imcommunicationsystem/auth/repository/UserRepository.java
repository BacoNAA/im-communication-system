package com.im.imcommunicationsystem.auth.repository;

import com.im.imcommunicationsystem.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据邮箱检查用户是否存在
     * 这个方法名是根据Spring Data JPA的命名规范自动生成的查询
     * @param email 邮箱地址
     * @return 如果存在则返回 true, 否则返回 false
     */
    boolean existsByEmail(String email);
}