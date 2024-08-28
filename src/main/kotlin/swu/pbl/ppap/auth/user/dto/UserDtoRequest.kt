package swu.pbl.ppap.auth.user.dto

import swu.pbl.ppap.openapi.generated.model.User


data class UserDtoRequest (
    val id: Long?=null,
    val loginId: String,
    val password: String,
    val username: String,
    val userType: User.UserType
){




}