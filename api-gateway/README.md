# api-gateway

The single entry point, and the only component that holds tokens.

|               |                  |
|---------------|------------------|
| Port          | 8080             |
| Session store | Redis, encrypted |

## Token Handler

The browser never sees an access token. It holds an opaque session id in an HttpOnly cookie; the
gateway exchanges it for a Bearer token on the way through and refreshes transparently.

That removes the entire class of token-in-JavaScript problems: nothing to exfiltrate via XSS, no
refresh loop in the client, no token in a query string.

- **Authorization Code with PKCE**, not ROPC. The password grant is deprecated and would put
  credentials through the front end.
- **Tokens are AES-256-GCM encrypted in Redis**, with a random nonce per write. The key is
  mandatory and has no default, so a misconfigured deployment fails to start rather than
  storing tokens in the clear.
- **An undecryptable session is treated as absent**, not as an error: a rotated key should log
  people out, not return 500 to everybody.

## Filter ordering

The cookie-to-Bearer swap is a Spring `WebFilter` at `HIGHEST_PRECEDENCE`, **not** a Spring
Cloud Gateway `GlobalFilter`. Gateway filters run inside the routing handler, after Spring
Security's chain — a token injected there arrives too late and every request is rejected as
anonymous.

WebSocket upgrades go through the same swap, which is why notification-service can authenticate
its handshake from a normal header.

See [Keycloak](../docs/keycloak.md).
