package swu.pbl.ppap.auth.user.entity

import jakarta.persistence.*
import swu.pbl.ppap.openapi.generated.model.User

@Entity
class UserEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var userId: Long? = null, //PK

    @Column(nullable = false, length = 30, updatable = false)
    val loginId: String,

    @Column(nullable = false, length = 100)
    val password: String,

    @Column(nullable = false, length = 100)
    val confirmPassword: String,

    @Column(nullable = false, length = 30)
    val username: String,

    @Column(nullable = false, length = 30)
    val email: String,

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    val userType: User.UserType, //UserType enum. 개인/법인 구분

    @Column(nullable=false)
    val isActive: Boolean = true,
    @Column(nullable = false)
    val isWithdrawed: Boolean = false
)