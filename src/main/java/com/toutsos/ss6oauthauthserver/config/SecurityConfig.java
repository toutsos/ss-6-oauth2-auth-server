package com.toutsos.ss6oauthauthserver.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;


@Configuration
public class SecurityConfig {

    /**
     * Get code url: http://localhost:8080/oauth2/authorize?response_type=code&client_id=client&scope=openid&redirect_uri=https://springone.io/authorized&code_challenge=YWpwNrjIkMDsKF5yytnDm2Iua8HtW3Dyo-i5OPi2vWc&code_challenge_method=S256
     * /oauth2/authorize is the default authorization server path
     * response_type=code tells the server that we will use AuthorizationCode
     * client_id is the user that authServer created for us to get access
     * scope=openid
     * redirect_uri must match with the redirect_uri configured in the authServer
     *
     */

    /**
     * Get token url: http://localhost:8080/oauth2/token?client_id=client&redirect_uri=https://springone.io/authorized&grant_type=authorization_code&code=[code from previous request]&code_verifier=-6MTSCntuU-u3NPfiZotLD4Vg-LiDExWJyzgd9Axm90y3KtKmmUtodvqAVjzf4M7MWWJLP63KWabHjX9MOi3MprGzH5DsbxPBc9VnH4DFYOb802UPsSmzfevOBOmHhdy
     */

    /**
     * What code_challenge and code_verifier is
     * 1. Before a user asks for code the client generates a random string ->  code_verifier
     * 2. in the code request client sends this code_verifier with sha256 encryption -> code_challenge
     * 3. when client retrieves the code and sends the taken request is puts as parameter the code_verifier
     * 4. the auth server checks if the hashed version of code_verifier that used to create code matches with the hashed version of the code_verifier that used to access token
     * 5. this method is called PKCE (pixie)
     * 6. is used mainly to prevent a aunothorised user who stole the code to gain also the token.
     * @param http
     * @return
     * @throws Exception
     */



    @Bean
    @Order(1)
    public SecurityFilterChain asSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(Customizer.withDefaults());

        http.exceptionHandling(
                e -> e.authenticationEntryPoint(
                        new LoginUrlAuthenticationEntryPoint("/login")
                )
        );

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain appSecurityFilterChain(HttpSecurity http) throws Exception {
        http.formLogin()
                .and()
                .authorizeHttpRequests().anyRequest().authenticated();

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        var u1 = User.withUsername("user")
                .password("password")
                .authorities("read")
                .build();

        return new InMemoryUserDetailsManager(u1);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        RegisteredClient r1 = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("client")
                .clientSecret("secret")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .redirectUri("https://springone.io/authorized")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .build();

        return new InMemoryRegisteredClientRepository(r1);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .build();
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() throws Exception {
        KeyPairGenerator kg = KeyPairGenerator.getInstance("RSA");
        kg.initialize(2048);
        KeyPair kp = kg.generateKeyPair();

        RSAPublicKey publicKey = (RSAPublicKey) kp.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) kp.getPrivate();

        RSAKey key = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();

        JWKSet set = new JWKSet(key);
        return new ImmutableJWKSet(set);
    }


}
