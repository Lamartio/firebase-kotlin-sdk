package dev.gitlive.firebase.auth

import dev.gitlive.firebase.firebase
import kotlinx.coroutines.await

actual open class AuthCredential(val js: firebase.auth.AuthCredential) {
    actual val providerId: String
        get() = js.providerId
}

actual class PhoneAuthCredential(js: firebase.auth.AuthCredential) : AuthCredential(js)
actual class OAuthCredential(js: firebase.auth.AuthCredential) : AuthCredential(js)

actual object EmailAuthProvider {
    actual fun credential(
        email: String,
        password: String
    ): AuthCredential = AuthCredential(firebase.auth.EmailAuthProvider.credential(email, password))
}

actual object FacebookAuthProvider {
    actual fun credential(accessToken: String): AuthCredential = AuthCredential(firebase.auth.FacebookAuthProvider.credential(accessToken))
}

actual object GithubAuthProvider {
    actual fun credential(token: String): AuthCredential = AuthCredential(firebase.auth.GithubAuthProvider.credential(token))
}

actual object GoogleAuthProvider {
    actual fun credential(idToken: String, accessToken: String): AuthCredential = AuthCredential(firebase.auth.GoogleAuthProvider.credential(idToken, accessToken))
}

actual class OAuthProvider(val js: firebase.auth.OAuthProvider, private val auth: FirebaseAuth) {
    actual constructor(provider: String, auth: FirebaseAuth) : this(firebase.auth.OAuthProvider(provider), auth)

    actual companion object {

        actual fun credentials(type: OAuthCredentialsType): AuthCredential {
            return when (type) {
                is OAuthCredentialsType.AccessToken -> getCredentials(type.providerId, accessToken = type.accessToken)
                is OAuthCredentialsType.IdAndAccessToken -> getCredentials(type.providerId, idToken = type.idToken, accessToken = type.accessToken)
                is OAuthCredentialsType.IdAndAccessTokenAndRawNonce -> getCredentials(type.providerId, idToken = type.idToken, rawNonce = type.rawNonce, accessToken = type.accessToken)
                is OAuthCredentialsType.IdTokenAndRawNonce -> getCredentials(type.providerId, idToken = type.idToken, rawNonce = type.rawNonce)
            }
        }

        private fun getCredentials(providerId: String, accessToken: String? = null, idToken: String? = null, rawNonce: String? = null): AuthCredential = rethrow {
            val acT = accessToken
            val idT = idToken
            val rno = rawNonce
            val provider = firebase.auth.OAuthProvider(providerId)
            val credentials = provider.credential(object : firebase.auth.OAuthCredentialOptions {
                override val accessToken: String? = acT
                override val idToken: String? = idT
                override val rawNonce: String? = rno
            }, accessToken)
            return AuthCredential(credentials)
        }
    }

    actual fun addScope(vararg scope: String) = rethrow { scope.forEach { js.addScope(it) } }
    actual fun setCustomParameters(parameters: Map<String, String>) = rethrow {
        js.setCustomParameters(parameters)
    }

    actual suspend fun signIn(signInProvider: SignInProvider): AuthResult = rethrow {
        AuthResult(when (signInProvider.type) {
            SignInProvider.SignInType.Popup -> {
                auth.js.signInWithPopup(js).await()
            }
            SignInProvider.SignInType.Redirect -> {
                auth.js.signInWithRedirect(js).await()
                auth.js.getRedirectResult().await()
            }
        })
    }
}

actual class SignInProvider(val type: SignInType) {
    enum class SignInType {
        Popup,
        Redirect
    }
}

actual class PhoneAuthProvider(val js: firebase.auth.PhoneAuthProvider) {

    actual constructor(auth: FirebaseAuth) : this(firebase.auth.PhoneAuthProvider(auth.js))

    actual fun credential(verificationId: String, smsCode: String): PhoneAuthCredential = PhoneAuthCredential(firebase.auth.PhoneAuthProvider.credential(verificationId, smsCode))
    actual suspend fun verifyPhoneNumber(phoneNumber: String, verificationProvider: PhoneVerificationProvider): AuthCredential = rethrow {
        val verificationId = js.verifyPhoneNumber(phoneNumber, verificationProvider.verifier).await()
        val verificationCode = verificationProvider.getVerificationCode(verificationId)
        credential(verificationId, verificationCode)
    }
}

actual interface PhoneVerificationProvider {
    val verifier: firebase.auth.ApplicationVerifier
    suspend fun getVerificationCode(verificationId: String): String
}

actual object TwitterAuthProvider {
    actual fun credential(token: String, secret: String): AuthCredential = AuthCredential(firebase.auth.TwitterAuthProvider.credential(token, secret))
}