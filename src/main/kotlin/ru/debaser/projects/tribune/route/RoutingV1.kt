package ru.debaser.projects.tribune.route

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.auth.authentication
import io.ktor.features.ParameterConversionException
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.files
import io.ktor.http.content.static
import io.ktor.request.receive
import io.ktor.request.receiveMultipart
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.util.pipeline.PipelineContext
import org.slf4j.Logger
import ru.debaser.projects.tribune.exception.LoginAlreadyExistsException
import ru.debaser.projects.tribune.dto.AuthenticationRequestDto
import ru.debaser.projects.tribune.dto.IdeaRequestDto
import ru.debaser.projects.tribune.dto.PushRequestDto
import ru.debaser.projects.tribune.exception.UserNotFoundException
import ru.debaser.projects.tribune.model.MediaModel
import ru.debaser.projects.tribune.model.UserModel
import ru.debaser.projects.tribune.service.FCMService
import ru.debaser.projects.tribune.service.FileService
import ru.debaser.projects.tribune.service.IdeaService
import ru.debaser.projects.tribune.service.UserService

val <T: Any> PipelineContext<T, ApplicationCall>.id
    get() = call.parameters["id"]?.toLongOrNull() ?: throw ParameterConversionException("id", "Long")

val <T: Any> PipelineContext<T, ApplicationCall>.authorId
    get() = call.parameters["authorId"]?.toLongOrNull() ?: throw ParameterConversionException("id", "Long")

val <T: Any> PipelineContext<T, ApplicationCall>.ideaId
    get() = call.parameters["ideaId"]?.toLongOrNull() ?: throw ParameterConversionException("id", "Long")

val <T: Any> PipelineContext<T, ApplicationCall>.me
    get() = call.authentication.principal<UserModel>()

class RoutingV1(
    private val staticPath: String,
    private val userService: UserService,
    private val fileService: FileService,
    private val ideaService: IdeaService,
    private val fcmService: FCMService
) {
    fun setup(configuration: Routing) {
        with(configuration) {
            route("/nosleep") {
                get {
                    call.respond("I'm not sleeping")
                }
            }
            route("/api/v1") {
                static("/static") {
                    files(staticPath)
                }
                post("/registration") {
                    val input = call.receive<AuthenticationRequestDto>()
                    when(userService.getByUsername(input.username)) {
                        null -> call.respond(userService.register(input))
                        else -> throw LoginAlreadyExistsException()
                    }
                }
                post("/authentication") {
                    val input = call.receive<AuthenticationRequestDto>()
                    call.respond(userService.authenticate(input))
                }
                authenticate {
                    route("/media") {
                        post {
                            val multipart = call.receiveMultipart()
                            val response = fileService.save(multipart)
                            call.respond(response)
                        }
                    }
                    route("/avatar") {
                        post {
                            val image = call.receive<MediaModel>()
                            userService.addAvatar(me!!.id, image.id)
                        }
                    }
                    route("/push") {
                        post {
                            val input = call.receive<PushRequestDto>()
                            userService.savePushToken(me!!.id, input.pushToken)
                            call.respond(PushRequestDto(input.pushToken))
                        }
                        patch("/remove") {
                            userService.remPushToken(me!!.id) ?: throw UserNotFoundException()
                            call.respond(HttpStatusCode.OK)
                        }
                    }
                    route("/ideas") {
                        post {
                            val input = call.receive<IdeaRequestDto>()
                            val id = ideaService.postIdea(me!!.id, input)
                            call.respond(ideaService.getById(id))
                        }
                        put("/{id}/like") {
                            val response = ideaService.like(id, me!!.id)
                            val authorId = ideaService.getById(id).authorId
                            if (!ideaService.isReaderEnough(id) && userService.isReader(authorId)) {
                                userService.setReader(authorId, false)
                            }
                            setBadges(me!!.id)
                            val pushToken = userService.getPushToken(response.authorId)
                            if (pushToken != null) {
                                fcmService.sendVote(me!!.username, pushToken, response.content, true)
                            }
                            call.respond(response)
                        }
                        put("/{id}/dislike") {
                            val response = ideaService.dislike(id, me!!.id)
                            val authorId = ideaService.getById(id).authorId
                            if (ideaService.isReaderEnough(id) && !userService.isReader(authorId)) {
                                userService.setReader(authorId, true)
                            }
                            setBadges(me!!.id)
                            val pushToken = userService.getPushToken(response.authorId)
                            if (pushToken != null) {
                                fcmService.sendVote(me!!.username, pushToken, response.content, false)
                            }
                            call.respond(response)
                        }
                        get("/recent") {
                            call.respond(ideaService.getRecent())
                        }
                        get("/{id}/before") {
                            call.respond(ideaService.getBefore(id))
                        }
                        get("/{id}/after") {
                            call.respond(ideaService.getAfter(id))
                        }
                        get("/recent/{authorId}") {
                            call.respond(ideaService.getRecentByAuthor(authorId))
                        }
                        get("/{id}/before/{authorId}") {
                            call.respond(ideaService.getBeforeByAuthor(authorId, id))
                        }
                        get("/{id}/after/{authorId}") {
                            call.respond(ideaService.getAfterByAuthor(authorId, id))
                        }
                    }
                    route("/votes") {
                        get("/{ideaId}") {
                            call.respond(ideaService.getAllVotes(ideaId))
                        }
                        get("/{ideaId}/after/{id}") {
                            call.respond(ideaService.getAfterVotes(ideaId, id))
                        }
                    }
                }
            }
        }
    }

    private suspend fun setBadges(id: Long) {
        when {
            ideaService.isGetPromoter(id) && !userService.isPromoter(id) -> userService.setPromoter(id, true)
            !ideaService.isGetPromoter(id) && userService.isPromoter(id) -> userService.setPromoter(id, false)
        }
        when {
            ideaService.isGetHater(id) && !userService.isHater(id) -> userService.setHater(id, true)
            !ideaService.isGetHater(id) && userService.isHater(id) -> userService.setHater(id, false)
        }
    }
}