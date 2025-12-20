package com.example.kotlintemplate.data.mapper

import com.example.kotlintemplate.data.remote.request.UserRequest
import com.example.kotlintemplate.data.remote.response.UserResponse
import com.example.kotlintemplate.domain.model.UserRemote

fun UserRequest.toDomainLocal() = UserRemote(name = name)
fun UserRemote.toRequest() = UserRequest(name = name)

fun UserResponse.toDomainLocal() = UserRemote(name = name)
fun UserRemote.toResponse() = UserResponse(name = name)