package io.github.devmugi.cv.agent.di

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

actual val httpEngineFactory: HttpClientEngineFactory<*> = Darwin
