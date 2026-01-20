package io.github.devmugi.cv.agent

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform