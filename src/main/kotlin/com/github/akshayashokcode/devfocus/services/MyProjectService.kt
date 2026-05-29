package com.github.akshayashokcode.devfocus.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class MyProjectService(@Suppress("UNUSED_PARAMETER") project: Project)
