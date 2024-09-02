package swu.pbl.ppap.auth.common.authority

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import swu.pbl.ppap.auth.user.entity.UserEntity
import swu.pbl.ppap.auth.user.repository.UserRepository

@Service
class CustomUserDetailService(
    private val userRepository: UserRepository
) : UserDetailsService{

    override fun loadUserByUsername(loginId: String): UserDetails =
        userRepository.findByLoginId(loginId)
            ?.let {  return createUserDetails(it) } ?: throw UsernameNotFoundException("User not found")

    private fun createUserDetails(user: UserEntity): UserDetails =
        CustomUser(
            user.loginId,
            user.password,
            user.userRole.map { SimpleGrantedAuthority("ROLE_${it.role}") }
        )
}