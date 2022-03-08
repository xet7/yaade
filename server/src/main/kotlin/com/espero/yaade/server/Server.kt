package com.espero.yaade.server

import com.espero.yaade.db.DaoManager
import com.espero.yaade.model.db.CollectionDb
import com.espero.yaade.server.routes.health
import com.espero.yaade.server.utils.coroutineHandler
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerFormat
import io.vertx.ext.web.handler.LoggerHandler
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.ext.web.openapi.RouterBuilder
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await

class Server(private val port: Int, private val daoManager: DaoManager) : CoroutineVerticle() {

    override suspend fun start() {
        val routerBuilder = RouterBuilder.create(vertx, "src/main/resources/openapi.yaml").await()
        routerBuilder.bodyHandler(BodyHandler.create())
        routerBuilder.operation("health").coroutineHandler(this, ::health)
        routerBuilder.operation("postCollection").coroutineHandler(this, ::postCollection)

        val router = routerBuilder.rootHandler(LoggerHandler.create(LoggerFormat.DEFAULT)).createRouter()

        router.route("/*").coroutineHandler(this, StaticHandler.create());

        val server = vertx.createHttpServer()
            .requestHandler(router)
            .listen(port)
            .await()

        println("Started server on com.espero.yaade.port ${server.actualPort()}")
    }

    private fun postCollection(ctx: RoutingContext) {
        val createCollectionRequest = ctx.bodyAsJson
        val newCollection = CollectionDb.fromJsonRequest(createCollectionRequest)
        daoManager.collectionDao.create(newCollection)
        ctx.end(newCollection.toJson().encode())
    }
}