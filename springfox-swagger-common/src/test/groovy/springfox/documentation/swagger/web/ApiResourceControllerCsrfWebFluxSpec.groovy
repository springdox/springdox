package springfox.documentation.swagger.web

import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import reactor.core.publisher.Mono
import spock.lang.Shared
import springfox.documentation.swagger.common.ClassUtils
import springfox.documentation.swagger.csrf.CsrfStrategy


class ApiResourceControllerCsrfWebFluxSpec extends ApiResourceControllerCsrfSpec {

    class Bridge {
        Closure<Mono<?>> cl
    }

    @Shared
    Bridge bridge = new Bridge()

    CsrfStrategy strategy
    WebTestClient flux

    void derive(CsrfStrategy strategy) {
        this.strategy = strategy
        flux = derive(this.strategy) {
            WebTestClient.bindToController(
                    new ApiResourceController.CsrfWebFluxController(it))
                    .webFilter({ exchange, chain ->
                        Mono.justOrEmpty(bridge.cl)
                                .flatMap({ cl -> cl(exchange) })
                                .switchIfEmpty(Mono.empty())
                                .then(chain.filter(exchange))
                    } as WebFilter)
                    .build()
        }
    }

    @Override
    def setupSpec() {
        Mockito.when(ClassUtils.isMvc()).thenReturn(false)
    }

    def cleanup() {
        bridge.with { cl = { x -> Mono.empty() } }
    }

    def "WebFlux - csrf token not supported"() {
        given:
        derive(CsrfStrategy.NONE)

        expect:
        flux.get().accept(MediaType.APPLICATION_JSON)
                .uri(ENDPOINT)
                .exchange()
                .expectBody().json(emptyCsrfToken)

    }

    def "WebFlux - csrf tokens stored in session"() {
        given:
        derive(CsrfStrategy.SESSION)
        bridge.with {
            cl = { ServerWebExchange e ->
                e.getSession().doOnNext({ s ->
                    s.getAttributes().put(
                            "org.springframework.security.web.server.csrf.WebSessionServerCsrfTokenRepository.CSRF_TOKEN",
                            new FakeCsrfToken())
                })
            }
        }

        expect:
        flux.get()
                .accept(MediaType.APPLICATION_JSON)
                .uri(ENDPOINT)
                .exchange()
                .expectBody().json(csrfToken)
    }

    def "WebFlux - csrf tokens not stored in session yet, but been temporarily stashed in request"() {
        given:
        derive(CsrfStrategy.SESSION)
        bridge.with {
            cl = { ServerWebExchange e ->
                e.getAttributes().put(
                        "org.springframework.security.web.server.csrf.CsrfToken",
                        Mono.just(new FakeCsrfToken()))
                Mono.empty()
            }
        }

        expect:
        flux.get().accept(MediaType.APPLICATION_JSON)
                .uri(ENDPOINT)
                .exchange()
                .expectBody().json(csrfToken)
    }

    def "WebFlux - csrf tokens stored in cookie"() {
        given:
        derive(CsrfStrategy.COOKIE)

        expect:
        flux.get().cookie(strategy.keyName, TOKEN)
                .accept(MediaType.APPLICATION_JSON)
                .uri(ENDPOINT)
                .exchange()
                .expectBody().json(csrfToken)
    }

    def "WebFlux - csrf tokens not stored in cookie yet, but been temporarily stashed in request"() {
        given:
        derive(CsrfStrategy.COOKIE)
        bridge.with {
            cl = { ServerWebExchange e ->
                e.getAttributes().put(
                        "org.springframework.security.web.server.csrf.CsrfToken",
                        Mono.just(new FakeCsrfToken()))
                Mono.empty()
            }
        }

        expect:
        flux.get().accept(MediaType.APPLICATION_JSON)
                .uri(ENDPOINT)
                .exchange()
                .expectBody().json(csrfToken)
    }

    def "WebFlux - cors requests should be prohibited"() {
        given:
        derive(CsrfStrategy.SESSION)
        bridge.with {
            cl = { ServerWebExchange e ->
                e.getSession().doOnNext({ s ->
                    s.getAttributes().put(
                            "org.springframework.security.web.server.csrf.WebSessionServerCsrfTokenRepository.CSRF_TOKEN",
                            new FakeCsrfToken())
                })
            }
        }

        expect:
        flux.get().header("Origin", "http://foreign.origin.com")
                .accept(MediaType.APPLICATION_JSON)
                .uri("http://dummy/" + ENDPOINT)
                .exchange()
                .expectBody().json(emptyCsrfToken)
    }
}
