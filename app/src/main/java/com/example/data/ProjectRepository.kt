package com.example.data

import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val projectDao: ProjectDao) {
    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    suspend fun getProjectById(id: Int): ProjectEntity? {
        return projectDao.getProjectById(id)
    }

    suspend fun insertProject(project: ProjectEntity): Long {
        return projectDao.insertProject(project)
    }

    suspend fun deleteProjectById(id: Int) {
        projectDao.deleteProjectById(id)
    }
}
