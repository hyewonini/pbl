package swu.pbl.ppap.auth.user.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import swu.pbl.ppap.auth.user.entity.UserEntity
import swu.pbl.ppap.auth.user.repository.UserRepository
import swu.pbl.ppap.auth.user.service.UserService
import swu.pbl.ppap.openapi.generated.controller.UsersApi
import swu.pbl.ppap.openapi.generated.model.User

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
    private val userRepository: UserRepository
): UsersApi {

    @PostMapping("/signup")
    override fun createUser(@RequestBody user: User): ResponseEntity<User> {
        val createdUserEntity = userService.signup(user)
        val createdUser = userService.convertToUser(createdUserEntity)
        return ResponseEntity.ok(createdUser)
    }
}