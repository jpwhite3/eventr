package com.eventr.repository

import com.eventr.model.User
import com.eventr.model.UserStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserRepository : JpaRepository<User, UUID> {
    fun findByEmail(email: String): User?
    fun findByEmailVerificationToken(token: String): User?
    fun findByPasswordResetToken(token: String): User?
    fun existsByEmail(email: String): Boolean
    fun findByStatus(status: UserStatus): List<User>
    fun findByRoleAndStatus(role: com.eventr.model.UserRole, status: UserStatus): List<User>
}