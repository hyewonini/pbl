package swu.pbl.ppap.chat

import jakarta.persistence.*
import swu.pbl.ppap.user.User

@Entity
class Conversation(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column
    var title: String,

    @ManyToOne(fetch = FetchType.LAZY)
    var user: User,

    ) {
}