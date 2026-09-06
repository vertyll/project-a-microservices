package com.vertyll.veds.shared.web.security

/**
 * The endpoint answers a caller nobody authenticated.
 *
 * The gateway lets these through without a token, so the reason belongs beside the method rather
 * than in a route matcher three services away. Anything that reads or writes a person's data needs
 * a permission or [AuthorizedInUseCase] instead.
 *
 * @property why what makes an unauthenticated caller acceptable here.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PublicEndpoint(
    val why: String,
)

/**
 * The decision needs the resource's own state, so the use case takes it rather than the method.
 *
 * A static permission cannot express "a member of this project may edit this task": the answer
 * depends on membership, ownership and the aggregate's lifecycle, none of which the filter chain
 * can see. Naming the collaborator here is what separates a deliberate decision taken deeper from
 * an endpoint nobody guarded.
 *
 * @property by the type that refuses the call — a policy, an authorization service or the use case
 *              itself.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AuthorizedInUseCase(
    val by: String,
)

/**
 * The resource is the caller's own, so there is nothing to refuse.
 *
 * Reading your notifications or editing your profile needs no permission: the caller's id from the
 * token narrows the query, and a row belonging to somebody else is never a candidate. This is the
 * one kind of endpoint where "no guard" is the right answer, which is exactly why it has to be
 * stated rather than inferred from an absence.
 *
 * @property how what narrows the work to the caller.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ScopedToCaller(
    val how: String,
)
