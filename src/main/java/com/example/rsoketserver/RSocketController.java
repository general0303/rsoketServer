package com.example.rsoketserver;

import com.example.rsoketserver.data.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.annotation.ConnectMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Controller
public class RSocketController {

    static final String SERVER = "Server";
    static final String RESPONSE = "Response";
    static final String STREAM = "Stream";
    static final String CHANNEL = "Channel";

    private final List<RSocketRequester> CLIENTS = new ArrayList<>();

    public Integer factorial(Integer n){
        if (n>1){
            return n*factorial(n-1);
        }
        return 1;
    }

    public Integer fib(Long n){
        if (n<1){
            return 1;
        }
        else{
            int a = 1;
            int b = 1;
            int c;
            for (int i=2; i<n; i++){
                c = a;
                a = a + b;
                b = c;
            }
            return a;
        }
    }

    @PreDestroy
    void shutdown() {
        log.info("Detaching all remaining clients...");
        CLIENTS.forEach(requester -> Objects.requireNonNull(requester.rsocket()).dispose());
        log.info("Shutting down.");
    }

    @ConnectMapping("shell-client")
    void connectShellClientAndAskForTelemetry(RSocketRequester requester,
                                              @Payload String client) {

        Objects.requireNonNull(requester.rsocket())
                .onClose()
                .doFirst(() -> {
                    log.info("Client: {} CONNECTED.", client);
                    CLIENTS.add(requester);
                })
                .doOnError(error -> {
                    log.warn("Channel to client {} CLOSED", client);
                })
                .doFinally(consumer -> {
                    CLIENTS.remove(requester);
                    log.info("Client {} DISCONNECTED", client);
                })
                .subscribe();
        requester.route("client-status")
                .data("OPEN")
                .retrieveFlux(String.class)
                .doOnNext(s -> log.info("Client: {} Free Memory: {}.", client, s))
                .subscribe();
    }


    @PreAuthorize("hasRole('USER')")
    @MessageMapping("request-response")
    Mono<Message> requestResponse(final Message request, @AuthenticationPrincipal UserDetails user) {
        log.info("Received request-response request: {}", request);
        log.info("Request-response initiated by '{}' in the role '{}'", user.getUsername(), user.getAuthorities());
        return Mono.just(new Message(SERVER, RESPONSE, request.getNumber()* request.getNumber()));
    }


    @PreAuthorize("hasRole('USER')")
    @MessageMapping("fire-and-forget")
    public Mono<Void> fireAndForget(final Message request, @AuthenticationPrincipal UserDetails user) {
        log.info("Received fire-and-forget request: {}", request);
        log.info("Result of calculations: {}", factorial(request.getNumber()));
        log.info("Fire-And-Forget initiated by '{}' in the role '{}'", user.getUsername(), user.getAuthorities());
        return Mono.empty();
    }


    @PreAuthorize("hasRole('USER')")
    @MessageMapping("stream")
    Flux<Message> stream(final Message request, @AuthenticationPrincipal UserDetails user) {
        log.info("Received stream request: {}", request);
        log.info("Stream initiated by '{}' in the role '{}'", user.getUsername(), user.getAuthorities());

        return Flux
                .interval(Duration.ofSeconds(1))
                .map(index -> new Message(SERVER, STREAM, index, fib(index)));
    }


    @PreAuthorize("hasRole('USER')")
    @MessageMapping("channel")
    Flux<Message> channel(final Flux<Duration> settings, @AuthenticationPrincipal UserDetails user) {
        log.info("Received channel request...");
        log.info("Channel initiated by '{}' in the role '{}'", user.getUsername(), user.getAuthorities());

        return settings
                .doOnNext(setting -> log.info("Channel frequency setting is {} second(s).", setting.getSeconds()))
                .doOnCancel(() -> log.warn("The client cancelled the channel."))
                .switchMap(setting -> Flux.interval(setting)
                        .map(index -> {if (setting.getSeconds() == 1){
                                return new Message(SERVER, CHANNEL, index, (int) (index*index));
                            }
                            else if (setting.getSeconds() == 3){
                                return new Message(SERVER, CHANNEL, index, factorial(Math.toIntExact(index)));
                            }
                            return new Message(SERVER, CHANNEL, index, fib(index));
                        }));
    }
}
