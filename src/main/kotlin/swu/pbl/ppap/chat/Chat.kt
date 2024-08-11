package swu.pbl.ppap.chat

import jakarta.persistence.*
import swu.pbl.ppap.user.User
import java.time.LocalDateTime

@Entity
class Chat (
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column
    var content: String,

    @Column
    var date: LocalDateTime,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    var conversation: Conversation,


    ) {
}