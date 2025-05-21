package com.vertyll.projecta.auth.domain.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.time.Instant

@Entity
@Table(name = "auth_user")
class AuthUser(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,

    @Column(nullable = false, unique = true) private var email: String,

    @Column(nullable = false) private var password: String,

    @OneToMany(
        fetch = FetchType.EAGER,
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    ) @JoinColumn(name = "auth_user_id") var userRoles: MutableSet<AuthUserRole> = mutableSetOf(),

    @Column(nullable = false) var enabled: Boolean = false,

    @Column(nullable = true) var userId: Long? = null,

    @Column(nullable = false) val createdAt: Instant = Instant.now(),

    @Column(nullable = false) var updatedAt: Instant = Instant.now()
) : UserDetails {
    // No-args constructor required for JPA
    protected constructor() : this(
        id = null, email = "", password = "", userRoles = mutableSetOf(), enabled = false
    )

    override fun getAuthorities(): Collection<GrantedAuthority> {
        return userRoles.map { SimpleGrantedAuthority("ROLE_${it.roleName}") }
    }

    override fun getUsername(): String = email

    override fun getPassword(): String = password

    fun setPassword(newPassword: String) {
        this.password = newPassword
        this.updatedAt = Instant.now()
    }

    fun setEmail(newEmail: String) {
        this.email = newEmail
        this.updatedAt = Instant.now()
    }

    /**
     * Adds a role to this user by creating an AuthUserRole entry
     */
    fun addRole(roleId: Long, roleName: String) {
        if (userRoles.none { it.roleId == roleId }) {
            userRoles.add(
                AuthUserRole(
                    authUserId = id ?: 0,
                    roleId = roleId,
                    roleName = roleName
                )
            )
            updatedAt = Instant.now()
        }
    }

    /**
     * Removes a role from this user
     */
    fun removeRole(roleId: Long) {
        val roleToRemove = userRoles.find { it.roleId == roleId }
        if (roleToRemove != null) {
            userRoles.remove(roleToRemove)
            updatedAt = Instant.now()
        }
    }

    /**
     * Get all role names for this user
     */
    fun getRoles(): Set<String> {
        return userRoles.map { it.roleName }.toSet()
    }

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = enabled

    companion object {
        fun create(
            email: String,
            password: String,
            enabled: Boolean = false,
            userId: Long? = null
        ): AuthUser {
            val authUser = AuthUser(
                email = email,
                password = password,
                enabled = enabled,
                userId = userId
            )

            // We'll need to update the roles later when we have role IDs
            // This will be handled by the UserEventConsumer

            return authUser
        }
    }
}
