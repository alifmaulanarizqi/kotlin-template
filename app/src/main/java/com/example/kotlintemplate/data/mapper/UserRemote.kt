package com.example.kotlintemplate.data.mapper

import com.example.kotlintemplate.data.remote.response.UserResponse
import com.example.kotlintemplate.domain.model.UserRemote

fun UserResponse.toDomainLocal() = UserRemote(status = status, inserted = inserted)
