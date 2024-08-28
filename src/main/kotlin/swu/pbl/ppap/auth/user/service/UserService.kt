package swu.pbl.ppap.auth.user.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import swu.pbl.ppap.auth.user.entity.UserEntity
import swu.pbl.ppap.auth.user.repository.UserRepository
import swu.pbl.ppap.openapi.generated.model.User

@Transactional
@Service
class UserService(
    private val userRepository: UserRepository,
) {
    // 회원가입
    fun signup(user: User): UserEntity {
        // 이미 등록된 ID인지 확인
        val existingUser = userRepository.findByLoginId(user.loginId)

        if (existingUser != null) {
            // 이미 존재하는 ID이면 메시지 반환
            throw IllegalArgumentException("중복된 ID입니다.")
        }

        // UserEntity 객체로 변환
        val userEntity = UserEntity(
            loginId = user.loginId,
            username = user.username,
            password = user.password,
            email = user.email,
            userId = user.userId,
            confirmPassword = user.confirmPassword,
            isActive = true,
            isWithdrawed = false,
            userType = user.userType
        )

        // DB에 저장
        userRepository.save(userEntity)

        return userEntity
    }
    // UserEntity를 User로 변환
    fun convertToUser(userEntity: UserEntity): User {
        return User(
            loginId = userEntity.loginId,
            username = userEntity.username,
            password = userEntity.password,
            userId = userEntity.userId,
            email = userEntity.email,
            confirmPassword = userEntity.confirmPassword,
            isActive = userEntity.isActive,
            isWithdrawed = userEntity.isWithdrawed,
            userType = userEntity.userType
        )
    }
}