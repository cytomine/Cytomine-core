package be.cytomine.project

class ProjectLastActivity {

    Project project

    Date lastActivity

    static constraints = {
        project(unique: true)
    }
}
