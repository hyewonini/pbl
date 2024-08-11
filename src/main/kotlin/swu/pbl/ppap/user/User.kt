package swu.pbl.ppap.user

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import org.hibernate.usertype.UserType

@Entity
class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var username: String,
    var password: String,
    var email: String,
    var phone: String,
    var userType: UserType

) {
    protected constructor() : this(null, "", "", "", "", UserType.PERSONAL)

    enum class UserType {
        PERSONAL, CORPORATE
    }
}
