# actuators

## Semantics

Identifies the operational endpoints of the server, such as health, info and metrics, provided
by Spring Boot Actuator.

These are not part of the order API. They use their own media type,
`application/vnd.spring-boot.actuator.v3+json`, rather than HAL, and which endpoints are
exposed, and whether they require authentication, depends on how the server is deployed.

## Related link rels

None. The actuator representation links to the individual endpoints itself.
