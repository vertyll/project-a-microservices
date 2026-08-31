package com.vertyll.veds.project.application.service.command

import com.vertyll.veds.project.application.ENGLISH
import com.vertyll.veds.project.application.InMemoryMemberRepository
import com.vertyll.veds.project.application.InMemoryProjectRepository
import com.vertyll.veds.project.application.InMemoryRoleRepository
import com.vertyll.veds.project.application.InMemoryUserDirectory
import com.vertyll.veds.project.application.RecordingEventPublisher
import com.vertyll.veds.project.application.command.UpdateMemberRoleCommand
import com.vertyll.veds.project.application.exception.ApiException
import com.vertyll.veds.project.application.project
import com.vertyll.veds.project.application.role
import com.vertyll.veds.project.application.service.MemberViewAssembler
import com.vertyll.veds.project.application.service.ProjectAuthorizationService
import com.vertyll.veds.project.application.userRef
import com.vertyll.veds.project.domain.error.ProjectError
import com.vertyll.veds.project.domain.model.ProjectMember
import com.vertyll.veds.project.domain.model.ProjectPermission
import com.vertyll.veds.project.domain.model.ProjectRoleCode
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Membership is how everyone other than the owner gets access, so these commands are the ones an
 * attacker would aim at: demote the owner, or reach a member of a project you are not in. Both are
 * refused here rather than anywhere downstream.
 */
internal class ProjectMembershipCommandServiceTest {
    private val projects = InMemoryProjectRepository()
    private val members = InMemoryMemberRepository()
    private val roles = InMemoryRoleRepository()
    private val users = InMemoryUserDirectory()
    private val events = RecordingEventPublisher()

    private val managerRole = role(ProjectRoleCode.MANAGER).also { roles.given(it) }
    private val memberRole =
        role(ProjectRoleCode.MEMBER, permissions = setOf(ProjectPermission.VIEW_PROJECT)).also { roles.given(it) }

    private val service =
        ProjectMembershipCommandService(
            memberRepository = members,
            roleRepository = roles,
            memberViewAssembler = MemberViewAssembler(roles, users),
            authorization = ProjectAuthorizationService(projects, members, roles),
            eventPublisher = events,
        )

    private val owner = UUID.randomUUID()
    private val existing = project(ownerId = owner).also { projects.given(it) }

    private fun givenMember(
        userId: UUID = UUID.randomUUID(),
        roleId: UUID = memberRole.id,
        projectId: UUID = existing.id,
    ): ProjectMember {
        users.given(userRef(userId = userId))
        return ProjectMember(projectId = projectId, userId = userId, roleId = roleId, version = 0L)
            .also { members.given(it) }
    }

    // ── Changing a role ─────────────────────────────────────────────────

    @Test
    fun `a member's role can be changed by someone who manages members`() {
        val member = givenMember()

        val view = service.updateMemberRole(existing.id, member.id, UpdateMemberRoleCommand(managerRole.id), owner, ENGLISH, 0L)

        assertEquals(managerRole.id, members.findById(member.id)!!.roleId)
        assertEquals(member.userId, view.userId)
    }

    @Test
    fun `changing a role announces the member's new standing`() {
        val member = givenMember()

        service.updateMemberRole(existing.id, member.id, UpdateMemberRoleCommand(managerRole.id), owner, ENGLISH, 0L)

        assertEquals(listOf("MemberJoined(${existing.id},${member.userId},MANAGER)"), events.published)
    }

    /**
     * The owner's access is the project's last line of recovery. Letting a manager demote them
     * would allow a project to be taken away from the person responsible for it.
     */
    @Test
    fun `the owner's own membership cannot be reassigned`() {
        val ownerMembership = givenMember(userId = owner, roleId = managerRole.id)

        val error =
            assertFailsWith<ApiException> {
                service.updateMemberRole(existing.id, ownerMembership.id, UpdateMemberRoleCommand(memberRole.id), owner, ENGLISH, 0L)
            }

        assertEquals(ProjectError.MEMBER_OWNER_IMMUTABLE, error.error)
        assertEquals(managerRole.id, members.findById(ownerMembership.id)!!.roleId)
    }

    /**
     * Member ids are global, so the project in the path has to be checked against the row. Without
     * it, knowing an id would be enough to edit a membership of a project you cannot even see.
     */
    @Test
    fun `a member of another project cannot be reached through this one`() {
        val elsewhere = givenMember(projectId = UUID.randomUUID())

        val error =
            assertFailsWith<ApiException> {
                service.updateMemberRole(existing.id, elsewhere.id, UpdateMemberRoleCommand(managerRole.id), owner, ENGLISH, 0L)
            }

        assertEquals(ProjectError.MEMBER_NOT_FOUND, error.error)
    }

    @Test
    fun `an unknown member is reported as missing`() {
        val error =
            assertFailsWith<ApiException> {
                service.updateMemberRole(existing.id, UUID.randomUUID(), UpdateMemberRoleCommand(managerRole.id), owner, ENGLISH, 0L)
            }

        assertEquals(ProjectError.MEMBER_NOT_FOUND, error.error)
    }

    @Test
    fun `a role that does not exist cannot be assigned`() {
        val member = givenMember()

        val error =
            assertFailsWith<ApiException> {
                service.updateMemberRole(existing.id, member.id, UpdateMemberRoleCommand(UUID.randomUUID()), owner, ENGLISH, 0L)
            }

        assertEquals(ProjectError.ROLE_NOT_FOUND, error.error)
    }

    /** Two managers editing the same member must not silently overwrite one another. */
    @Test
    fun `a reassignment against a stale version is refused`() {
        val member = givenMember()

        val error =
            assertFailsWith<ApiException> {
                service.updateMemberRole(existing.id, member.id, UpdateMemberRoleCommand(managerRole.id), owner, ENGLISH, version = 7L)
            }

        assertEquals(ProjectError.VERSION_MISMATCH, error.error)
        assertEquals(memberRole.id, members.findById(member.id)!!.roleId)
    }

    @Test
    fun `someone without the manage-members permission cannot reassign anyone`() {
        val member = givenMember()
        val bystander = givenMember()

        assertFailsWith<ApiException> {
            service.updateMemberRole(existing.id, member.id, UpdateMemberRoleCommand(managerRole.id), bystander.userId, ENGLISH, 0L)
        }

        assertEquals(memberRole.id, members.findById(member.id)!!.roleId)
        assertTrue(events.published.isEmpty())
    }

    // ── Removing ────────────────────────────────────────────────────────

    @Test
    fun `a member can be removed and their departure announced`() {
        val member = givenMember()

        service.removeMember(existing.id, member.id, owner)

        assertNull(members.findById(member.id))
        assertEquals(listOf("MemberRemoved(${existing.id},${member.userId})"), events.published)
    }

    /** Removing the owner would leave the project with nobody able to administer it. */
    @Test
    fun `the owner cannot be removed from their own project`() {
        val ownerMembership = givenMember(userId = owner, roleId = managerRole.id)

        val error = assertFailsWith<ApiException> { service.removeMember(existing.id, ownerMembership.id, owner) }

        assertEquals(ProjectError.MEMBER_OWNER_IMMUTABLE, error.error)
        assertNotNull(members.findById(ownerMembership.id))
    }

    @Test
    fun `a member of another project cannot be removed through this one`() {
        val elsewhere = givenMember(projectId = UUID.randomUUID())

        assertFailsWith<ApiException> { service.removeMember(existing.id, elsewhere.id, owner) }

        assertNotNull(members.findById(elsewhere.id))
    }

    @Test
    fun `someone without the manage-members permission cannot remove anyone`() {
        val member = givenMember()
        val bystander = givenMember()

        assertFailsWith<ApiException> { service.removeMember(existing.id, member.id, bystander.userId) }

        assertNotNull(members.findById(member.id))
        assertTrue(events.published.isEmpty())
    }
}
