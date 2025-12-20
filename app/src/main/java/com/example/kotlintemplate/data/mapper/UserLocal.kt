package com.example.kotlintemplate.data.mapper

import com.example.kotlintemplate.data.local.entity.UserEntity
import com.example.kotlintemplate.domain.model.UserLocal

fun UserEntity.toDomainLocal() = UserLocal(id = id, name = name, status = status)
fun UserLocal.toEntityLocal() = UserEntity(id = id, name = name, status = status)