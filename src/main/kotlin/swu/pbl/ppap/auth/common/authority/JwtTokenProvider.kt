package swu.pbl.ppap.auth.common.authority
 


import io.jsonwebtoken.*
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import swu.pbl.ppap.openapi.generated.model.UserToken
import java.util.Date
import javax.crypto.SecretKey


const val ACCESS_EXPIRATION_TIME: Long = 1000 * 60 * 30 //1시간
const val REFRESH_EXPIRATION_TIME: Long = 1000 * 60 * 60

@Component
class JwtTokenProvider {

    @Value("\${spring.jwt.access-secret}")
    lateinit var accessSecret: String

    @Value("\${spring.jwt.refresh-secret}")
    lateinit var refreshSecret: String

    final fun getSigningAccessKey(): SecretKey{
        var accessKeyBytes: ByteArray? = Decoders.BASE64.decode(accessSecret)
        return Keys.hmacShaKeyFor(accessKeyBytes)
    }
    final fun getSigningRefreshKey(): SecretKey{
        var refreshKeyBytes: ByteArray? = Decoders.BASE64.decode(refreshSecret)
        return Keys.hmacShaKeyFor(refreshKeyBytes)
    }
    private val accessKey: SecretKey = getSigningAccessKey()
    private val refreshKey: SecretKey = getSigningRefreshKey()


    //token 생성
    fun createToken(authentication: Authentication): UserToken {
        val authorities: String = authentication
            .authorities
            .joinToString(",", transform = GrantedAuthority::getAuthority)

        val now = Date()
        val accessExpiration = Date(now.time + ACCESS_EXPIRATION_TIME )
        val refreshExpiration = Date(now.time + REFRESH_EXPIRATION_TIME )

        val accessToken = Jwts.builder()
            .subject(authentication.name)
            .claim("auth", authorities)
            .issuedAt(now)
            .expiration(accessExpiration)
            .signWith(accessKey)
            .compact()
        val refreshToken = Jwts.builder()
            .subject(authentication.name)
            .claim("auth", authorities)
            .issuedAt(now)
            .expiration(refreshExpiration)
            .signWith(refreshKey)
            .compact()

        return UserToken("Bearer", accessToken,  refreshToken)
    }

    //token 정보 추출
    fun getAuthentication(token: String): Authentication {
        val claims: Claims = getAccessTokenClaims(token)
        val auth = claims["auth"] ?: throw RuntimeException("Wrong Token")

        //권한 정보 추출
        val authorities: Collection<GrantedAuthority> = (auth as String)
            .split(",")
            .map { SimpleGrantedAuthority(it)}

        val principal = CustomUser(claims.subject, "", authorities)
        return UsernamePasswordAuthenticationToken(principal, "", authorities)
    }

    fun validateRefreshTokenAndCreateToken(refreshToken: String) : UserToken {
        try {
            val refreshClaims: Claims = getRefreshTokenClaims(refreshToken)
            val now = Date()

            //새로운 access token 발급
            val newAccessToken = Jwts.builder()
                .subject(refreshClaims.subject)
                .claim("auth", refreshClaims["auth"])
                .issuedAt(now)
                .expiration(Date(now.time + ACCESS_EXPIRATION_TIME ))
                .signWith(accessKey)
                .compact()

            //새로운 refresh token 발급
            val newRefreshToken = Jwts.builder()
                .subject(refreshClaims.subject)
                .claim("auth", refreshClaims["auth"])
                .issuedAt(now)
                .expiration(Date(now.time + REFRESH_EXPIRATION_TIME ))
                .signWith(refreshKey)
                .compact()

            return UserToken("Bearer", newAccessToken,  newRefreshToken)
        } catch (e: Exception) {
            throw e
        }
    }
    fun validateAccessTokenForFilter(token: String) : Boolean {
        try {
            getAccessTokenClaims(token)
            return true
        } catch (e: Exception) {
            when(e) {
                is SecurityException -> {}
                is MalformedJwtException -> {}
                is ExpiredJwtException -> {}
                is UnsupportedJwtException -> {}
                is IllegalArgumentException -> {}  //JWT Claims string is empty
                else -> {}
            }
            throw e
        }
    }
    private fun getAccessTokenClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(accessKey)
            .build()
            .parseSignedClaims(token)
            .payload

    private fun getRefreshTokenClaims(token: String): Claims =
        Jwts.parser()
            .verifyWith(refreshKey)
            .build()
            .parseSignedClaims(token)
            .payload

}