package swu.pbl.ppap.auth.common.authority

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.util.StringUtils
import org.springframework.web.filter.GenericFilterBean

//jwt 인증을 위해 UsernamePasswordAuthenticationFilter 이전에 실행되는 Custom Filter.
//이 필터를 통과하면 UsernamePasswordAuthenticationFilter는 자동으로 통과하게 된다.
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : GenericFilterBean() {
    override fun doFilter(
        request: ServletRequest,
        response: ServletResponse,
        chain: FilterChain? ) {

        val token = resolveToken(request as HttpServletRequest)

        if(token != null && jwtTokenProvider.validateAccessTokenForFilter(token) ){
            val authentication = jwtTokenProvider.getAuthentication(token)
            SecurityContextHolder.getContext().authentication = authentication

        } //db를 거치지 않고 security context에 저장된 authentication의 유저 아이디로 유효한 유저인지 검증
        //유저의 다른 정보는 db 조회해야 함

        chain?.doFilter(request, response)
    }

    //Authorization 헤더에서 프리픽스인 Bearer를 제거한 jwt access 토큰만 추출하여 리턴
    private fun resolveToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")

        return if (StringUtils.hasText(bearerToken) &&
            bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else null
    }
}