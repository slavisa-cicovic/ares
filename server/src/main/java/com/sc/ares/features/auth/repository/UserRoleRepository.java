package com.sc.ares.features.auth.repository;

import com.sc.ares.features.auth.entity.UserRole;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    boolean existsByUserIdAndRoleId(UUID userId, UUID roleId);

    @Query("""
        select userRole
        from UserRole userRole
        join fetch userRole.role
        where userRole.user.id = :userId
        """)
    List<UserRole> findAllByUserId(UUID userId);

    @Query("""
        select userRole
        from UserRole userRole
        join fetch userRole.role
        where userRole.user.id in :userIds
        """)
    List<UserRole> findAllByUserIdIn(List<UUID> userIds);

    void deleteByUserId(UUID userId);
}
