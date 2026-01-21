package io.github.devmugi.cv.agent.di

import io.ktor.client.engine.HttpClientEngineFactory

expect val httpEngineFactory: HttpClientEngineFactory<*>
