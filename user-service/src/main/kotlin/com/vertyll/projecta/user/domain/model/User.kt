package com.vertyll.projecta.user.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "\"user\"")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(nullable = false)
    var firstName: String,
    
    @Column(nullable = false)
    var lastName: String,
    
    @Column(nullable = false, unique = true)
    private var email: String,
    
    @Column(nullable = true)
    var profilePicture: String? = null,
    
    @Column(nullable = true)
    var phoneNumber: String? = null,
    
    @Column(nullable = true)
    var address: String? = null,
    
    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    // No-args constructor required for JPA
    constructor() : this(
        id = null,
        firstName = "",
        lastName = "",
        email = "",
        profilePicture = null,
        phoneNumber = null,
        address = null
    )
    
    // Roles are stored in the Role Service
    @Transient
    private var cachedRoles: Set<String>? = null
    
    /**
     * Set cached roles from Role Service
     */
    fun setCachedRoles(roles: Set<String>) {
        this.cachedRoles = roles
    }
    
    /**
     * Get cached roles
     */
    fun getCachedRoles(): Set<String> {
        return cachedRoles ?: emptySet()
    }
    
    fun getEmail(): String = email
    
    fun setEmail(newEmail: String) {
        this.email = newEmail
        this.updatedAt = Instant.now()
    }

    companion object {
        fun create(
            firstName: String,
            lastName: String,
            email: String,
            roles: Set<String> = setOf("USER"),
            profilePicture: String? = null,
            phoneNumber: String? = null,
            address: String? = null
        ): User {
            val user = User(
                firstName = firstName,
                lastName = lastName,
                email = email,
                profilePicture = profilePicture,
                phoneNumber = phoneNumber,
                address = address
            )
            user.setCachedRoles(roles)
            return user
        }
    }
}
